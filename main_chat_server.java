package chop09.UDPChatGUI;

import java.io.IOException;

public class main_chat_server {
    public static void main(String[] args) throws IOException {
        try {
            new chat_server();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
