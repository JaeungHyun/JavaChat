import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

// SimpleChat -- 서버와 하나의 클라이언트 간의 네트워크 채팅 프로그램
// 서버를 실행하기 위해서는 별도의 실행인자(Argument)가 필요 없다.
// 기본 포트는 3004번으로 설정되어 있다.

// 클라이언트 프로그램을 실행하기 전에 반드시 서버 프로그램을 먼저 실행해야 한다.
// 클라이언트 프로그램을 실행하기 위해서는 연결하고자 하는 IP나 URL을 인자로 지정한다.
// 인자(Argument):
//   java SimpleChat local          // 자신의 컴퓨터 서버에 연결


public class SimpleChat extends Frame {

    Socket sock;
    private int port = 3004;         // 포트를 3004로 고정

    private DataOutputStream osDataStream = null; // 네트워크 전송

    public JTextArea ReceiveDataField;     // 수신 데이터 표시
    public JTextField SendDataField;        // 전송 데이터 입력
    public JScrollPane scrollPane;
    


    // 프로그램 시작
    public static void main(String args[]) {

        // 채팅창을 만듦
        SimpleChat chat = new SimpleChat();

        chat.pack();
        chat.setSize(350, 600);   //  창 크기 설정
        chat.setVisible(true);    //  창을 화면에 보인다.

        // 실행인자에 의해 서버나 클라이언트로 접속
        // 만일 실행인자에 접속 사이트 주소를 입력하면 클라이언트
        String s = null;

        if (args.length > 0)
            s = args[0];

        // 접속 방법 설정
        if (s == null) {
            chat.setTitle(" # 서버로 실행합니다.");
            chat.server();
        } else {
            chat.setTitle(" # 클라이언트로 실행합니다.");
            chat.client(s);
        }
    }

    public SimpleChat() {
        super("SimpleChat");

        // 사용자 인터페이스를 만듦
        setLayout(new BorderLayout());

        // 받은 데이터 표시 필드 생성
        ReceiveDataField = new JTextArea();
        ReceiveDataField.setEditable(false);
        
        // 스크롤바 구현
        scrollPane = new JScrollPane(ReceiveDataField);
        
        this.add(scrollPane, BorderLayout.CENTER);
        ReceiveDataField.setBackground(new Color(192, 207, 217));
        
        

        // 전송 데이터 입력 필드 생성
        SendDataField = new JTextField();
        this.add(SendDataField, BorderLayout.SOUTH);
   

        // 전송 데이터 입력 필드에서 Enter키를 눌렸을때 이벤트 처리
        SendDataField.addActionListener(new TextActionHandler());

        // 닫기 버튼을 누르면 윈도우 종료 이벤트 처리
        addWindowListener(
                new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        System.exit(0);
                    }
                });
    }

    // 서버로 실행하고, 클라이언트의 접속을 기다림
    private void server() {
        ServerSocket serverSock = null;   // 서버소켓을 설정
        try {
            InetAddress serverAddr = InetAddress.getByName(null);

            this.setTitle(" # Port:" + port + "에서 클라이언트의 연결을 기다립니다.");

            // 만일 클라이언트로 부터 연결이 되었다면
            serverSock = new ServerSocket(port, 1);

            // 클라언트와 통신할 소켓을 생성한다.
            sock = serverSock.accept();

            this.setTitle(" # 연결. 상대방 호스트:" + sock.getInetAddress().getHostName());

            // 소켓으로 전송할 통로(스트림)을 만듦
            osDataStream = new DataOutputStream(sock.getOutputStream());

            // 수신 쓰레드를 생성하여 주기적으로 소켓을 검사하여 데이터를 읽어 들임
            new ReceiveDataThread(this).start();
        } catch (IOException e) {
            this.setTitle(" ## 클라이언트와 연결 중 문제 발생 --> " + e.toString());
            System.out.println(e.toString());
        } finally {
            // 문제가 발생하면  소켓을 닫음.
            if (serverSock != null) {
                try {
                    serverSock.close();
                } catch (IOException x) {

                }
            }
        }
    }

    // 클라이언트로 실행하여 서버에 접속
    public void client(String serverName) {
        try {
            // 만일 'local'로 입력하면 서버 이름을 null로 하여 자신의 컴퓨터로 연결
            if (serverName.equals("local"))
                serverName = null;

            // 서버 이름을 통해 IP주소를 찾음
            InetAddress serverAddr = InetAddress.getByName(serverName);

            // 서버에 연결이 성공적으로 이뤄지면 sock 클래스가 생성됨
            sock = new Socket(serverAddr.getHostName(), port);

            // 네트워크로 보낼 통로(Stream)를 생성
            osDataStream = new DataOutputStream(sock.getOutputStream());

            this.setTitle(" # 호스트 : " + serverAddr.getHostName() +
                    ", Port: " + sock.getPort() + "에 연결되었습니다.");

            // 수신 쓰레드를 생성하여 주기적으로 소켓을 검사하여 데이터를 읽어 들임
            new ReceiveDataThread(this).start();
        } catch (IOException e) {
            this.setTitle(" ## 서버와 연결 중 문제 발생 --> " + e.toString());
            System.out.println(e.toString());
        }
    }

    // 테스트 필드에서 데이터를 입력하면 네트워크로 전송
    class TextActionHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            // 아직 연결이 안되었으면 이벤트 처리하지 않음
            if (osDataStream == null)
                return;

            // 데이터를 네트워크 전송 통로를 통해 상대방으로전송
            try {
                osDataStream.writeUTF(SendDataField.getText()); // 데이터를 전송한다.
                ReceiveDataField.append("                                                   " + SendDataField.getText() + "\n" + "\n"); // 전송한 데이터를 내 창에 띄운다.
                ReceiveDataField.setCaretPosition(ReceiveDataField.getDocument().getLength()); // 자동스크롤 추가 
                SendDataField.setText("");                // 기존의 입력한 데이터를 지운다.
            } catch (IOException x) {
                setTitle(" # 에러 : " + x.getMessage());
            }
        }
    }

    // 수신 쓰레드에서 데이터를 받으면 TextArea에 데이터를 누적 시킴
    public void addRecvString(String str) {
        ReceiveDataField.append(" " + str + "\n" + "\n");
        ReceiveDataField.setCaretPosition(ReceiveDataField.getDocument().getLength()); // 자동스크롤 추가
    }

    protected void finalize() throws Throwable {
        try {
            if (osDataStream != null)
                osDataStream.close();
            if (sock != null)
                sock.close();
        } catch (IOException x) {

        }
        super.finalize();
    }
}

//
// 독립적으로 실행하는 ReceiveDataThread는 소켓에 데이터가 도착했는지를 주기적으로
// 감시하고, 만일 데이터가 전송 되었다면, 윈도우의 수신 데이터 필드에 데이터를 추가
//
class ReceiveDataThread extends Thread {
    private SimpleChat chat;             // 현재 연결된 채팅 클래스
    private DataInputStream isDataStream;     // 네트워크로 수신 통로(Stream)
    private boolean bWaitting = true; // 무한 반복

    public ReceiveDataThread(SimpleChat chat) {
        this.chat = chat;
    }

    public synchronized void run() {
        String str;
        try {
            // 네트워크 수신 통로를 설정
            isDataStream = new DataInputStream(chat.sock.getInputStream());

            // 반복
            while (bWaitting) {
                str = isDataStream.readUTF(); // 만을 수신 데이터가 있다면
                chat.addRecvString(str);      // 수신 데이터 필드에 추가
            }
        } catch (IOException e) {
            chat.setTitle(" # 에러 : " + e.getMessage());
        } finally {
            try {
                if (isDataStream != null)
                    isDataStream.close();
            } catch (IOException x) {
            }
        }
    }
}
