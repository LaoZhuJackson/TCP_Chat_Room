package chop09.UDPChatGUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class chat_client extends JFrame implements ActionListener {
    private final int port = 65533;
    private InetAddress ip_address = null;
    private PrintWriter client_out;
    private BufferedReader client_in;
    private Socket client_socket;

    private JLabel jl_status = new JLabel("当前还没有启动监听！");// 状态栏
    private JTextArea jta_log = new JTextArea(5, 20);// 聊天记录
    private JTextArea jta_friends = new JTextArea(5, 15);// 好友列表
    private JTextArea jta_input = new JTextArea(5, 20);// 聊天输入
    private JTextField jtf_ip = new JTextField();// 本机ip
    private JTextField jtf_port = new JTextField(port);// 监听的port
    private JPanel jp_bottom = new JPanel();
    private JPanel jp_foot = new JPanel();
    private JButton jb_send = new JButton("发送");
    private JButton jb_clear = new JButton("清空");// 清空聊天记录
    private JButton jb_exit = new JButton("退出");// 退出群聊

    public chat_client() {
        super("Chat room:client");
        try {
            ip_address = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        initUI();
        try {
            initSocket();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(chat_client.this, "端口可能被占用，请换一个空闲端口！");
            e.printStackTrace();
        }
        jl_status.setText("正在监听" + port + "端口！");
    }

    private void initSocket() throws IOException {
        //初始化
        client_socket = new Socket(ip_address, port);
        client_in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
        client_out = new PrintWriter(client_socket.getOutputStream(), true);
        //开启线程处理监听服务端发来的消息
        new ClientThread().start();
    }

    //初始化图形界面
    private void initUI() {
        setSize(540, 360);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);//禁止修改窗口大小

        jl_status.setHorizontalAlignment(JLabel.RIGHT);
        add(jl_status, BorderLayout.NORTH);

        jta_log.setBorder(BorderFactory.createTitledBorder("Chat record"));
        jta_log.setEditable(false);
        jta_friends.setBorder(BorderFactory.createTitledBorder("Friends"));
        jta_friends.setEditable(false);
        add(new JScrollPane(jta_log), BorderLayout.CENTER);
        add(new JScrollPane(jta_friends), BorderLayout.WEST);

        jp_bottom.setLayout(new BorderLayout());//为底部创建一个新的panel，设置为边界布局
        jp_bottom.add(jta_input, BorderLayout.CENTER);
        jp_bottom.add(jp_foot, BorderLayout.SOUTH);
        jta_input.setBorder(BorderFactory.createTitledBorder("Please enter text:"));
        jp_foot.setLayout(new GridLayout(1, 5));
        jtf_ip.setText(ip_address.getHostAddress());
        jp_foot.add(jtf_ip);
        jp_foot.add(jtf_port);
        jb_send.addActionListener(this);
        jb_clear.addActionListener(this);
        jb_exit.addActionListener(this);
        jp_foot.add(jb_clear);
        jp_foot.add(jb_send);
        jp_foot.add(jb_exit);
        add(jp_bottom, BorderLayout.SOUTH);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == jb_send) {
            //获取客户端输入
            String input = jta_input.getText();
            client_out.println(input);//发消息给服务器
            client_out.flush();
            jta_log.setCaretPosition(jta_log.getText().length());
            jta_input.setText("");
        }
        if (e.getSource() == jb_clear) {
            jta_log.setText("");
        }
        if (e.getSource() == jb_exit) {//按退出按钮
            client_out.println("exit");
            try {//客户端退出服务器
                client_out.close();
                client_in.close();
                client_socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            dispose();//销毁当前窗口
        }
    }

    private class ClientThread extends Thread {
        public void run() {
            try {
                //接收服务器消息
                String fromServer_data;
                while ((fromServer_data = client_in.readLine()) != null) {
                    jta_log.append(fromServer_data + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
