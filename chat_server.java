package chop09.UDPChatGUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class chat_server extends JFrame implements ActionListener {
    private final int port = 65533;
    private JLabel jl_status = new JLabel("开始监听" + port + "端口");
    private JTextArea jta_friends_status = new JTextArea(5, 20);//刷新好友列表提示
    private JButton jb_exit = new JButton("Exit");
    private JPanel jp_bottom = new JPanel();

    private ServerSocket serverSocket;
    private Map<String, Socket> clients;//存放访问服务器的客户端和其用户名

    public chat_server() {
        super("Chat room:server");
        initUI();
        try {
            initSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initSocket() throws Exception {
        //初始化
        serverSocket = new ServerSocket(this.port);//创建服务器
        JOptionPane.showMessageDialog(chat_server.this, "服务器创建成功");
        clients = new HashMap<>();//HashMap由数组+链表组成的
        showUserList();
        //每接收一个客户端的请求连接是都启用一个线程
        while (true) {
            Socket client_socket = serverSocket.accept();//在没接受到客户端请求之前，这个accept会一直阻塞
            new HandleClientThread(client_socket).start();//开启新线程处理请求
        }
    }
    //刷新客户端在线用户列表
    private  void refresh_friend(){
        int i=0;
        StringBuilder temp=new StringBuilder();
        temp.append("Online:");
        //将用户加入字符串
        for (Map.Entry<String,Socket> user:clients.entrySet()){
            i++;
            temp.append("|").append(i).append(".").append(user.getKey());
        }
        sendToAllClient(String.valueOf(temp));
        System.out.println("执行刷新操作,temp的值为："+temp);
    }

    //初始化图形界面
    private void initUI() {
        setSize(300, 370);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);//禁止修改窗口大小

        jl_status.setHorizontalAlignment(JLabel.RIGHT);
        jl_status.setEnabled(false);
        add(jl_status, BorderLayout.NORTH);
        jta_friends_status.setBorder(BorderFactory.createTitledBorder("Friend's status"));
        jta_friends_status.setEditable(false);
        jta_friends_status.setLineWrap(true);//允许换行
        add(new JScrollPane(jta_friends_status), BorderLayout.CENTER);
        jb_exit.addActionListener(this);
        jp_bottom.setLayout(new BorderLayout());
        jp_bottom.add(jb_exit, BorderLayout.CENTER);
        add(jp_bottom, BorderLayout.SOUTH);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == jb_exit) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            dispose();
        }
    }

    //广播给所有客户端
    private synchronized void sendToAllClient(String message) {
        try {
            for (Map.Entry<String, Socket> user : clients.entrySet()) {//循环每个用户
                //获取user对应Socket的outputStream
                PrintWriter server_out = new PrintWriter(user.getValue().getOutputStream(), true);
                server_out.println(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void sendToUser(String sendName, String receiveName, String message) {
        try {
            //给接收方绑定一个PrintWriter，更新别人对他的私聊
            PrintWriter out = new PrintWriter(clients.get(receiveName).getOutputStream(), true);
            PrintWriter out_self = new PrintWriter(clients.get(sendName).getOutputStream(), true);
            out.println("来自 " + sendName + " 的私聊：" + message);
            out_self.println("私聊 "+receiveName+" :"+message);//同步更新自己的对话框
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //synchronized实现代码块互斥,使共享数据完整，展示列表当然得展示完才去执行别的
    private synchronized void showUserList() {
        System.out.println("运行了showList");
        jta_friends_status.setText("在线人数：" + clients.size());
        System.out.println("当前的clients值为：" + clients.size());
        //更新服务器用户列表
        int client_count=0;
        for (Map.Entry<String, Socket> user : clients.entrySet()) {
            client_count++;
            jta_friends_status.append("|" + client_count + "." + user.getKey() + "----");
        }
    }

    //某个客户端退出服务器
    private synchronized void offline(String client_name) {
        clients.remove(client_name);
        if (client_name != null)
            sendToAllClient(client_name + "退出群聊");
        refresh_friend();
        chat_server.this.showUserList();
    }

    //线程处理客户端请求
    private class HandleClientThread extends Thread {
        private Socket clientSocket;
        private BufferedReader server_in;//BufferedReader用于加快读取字符的速度
        private PrintWriter server_out;//具有自动行刷新的缓冲字符输出流，特点是可以按行写出字符串，并且可以自动行刷新
        private String client_name;

        public HandleClientThread(Socket client_socket) {
            try {
                //初始化
                this.clientSocket = client_socket;
                //clientSocket调用get方法获得客户端的输出，被字符流读出之后放入缓冲区
                server_in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
                //将客户端的getOutput绑定到server_out
                server_out = new PrintWriter(this.clientSocket.getOutputStream(), true);//自动刷新

                //通知刚连接的客户端输入用户名
                server_out.println("连接成功！请输入用户名：");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //处理客户端消息
        public void run() {
            try {
                int flag = 0;//判断客户端是否为第一次访问
                String fromClientData;
                //循环接收客户端发送的数据
                while ((fromClientData = server_in.readLine()) != null) {//当读出的客户端第一行数据不为空
                    System.out.println("接收到消息：" + fromClientData);
                    //接收到客户端请求关闭连接
                    if ("exit".equals(fromClientData)) {
                        chat_server.this.offline(this.client_name);
                        break;
                    }
                    //客户端第一次访问
                    if (flag++ == 0) {
                        System.out.println(flag);
                        this.client_name = fromClientData;
                        clients.put(this.client_name, this.clientSocket);//存入hashMap
                        sendToAllClient("欢迎：" + this.client_name + " 加入聊天室");
                        refresh_friend();//更新客户端在线人数信息
                        chat_server.this.showUserList();
                        continue;
                    }
                    //处理私聊  格式： @接收客户端名字：所说内容
                    if (fromClientData.startsWith("@")) {
                        //获取客户端名字
                        String receiveName = fromClientData.substring(1, fromClientData.indexOf("："));
                        String message = fromClientData.substring(fromClientData.indexOf("：") + 1);
                        //调用私聊某个user的方法
                        sendToUser(this.client_name, receiveName, message);
                    } else {//非私聊，则是群发
                        sendToAllClient(this.client_name + "：" + fromClientData);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
