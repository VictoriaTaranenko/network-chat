package ru.geekbrains.network.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public String getUsername() {
        return username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                // цикл авторизации
                while (true) {
                    String msg = in.readUTF();
                    if(msg.startsWith("/login")) {
                        String usernameFromLogin = msg.split("\\s")[1];
                        if(server.isNickBusy(usernameFromLogin)) {
                            sendMessage("/login_failed Current nickname is already used ");
                            continue;
                        }
                    username = usernameFromLogin;
                    sendMessage("/login_ok " + username);
                    server.subscribe(this);
                    break;
                    }
                }
                // цикл общения с клиентом
                while (true) {
                    String msg = in.readUTF();
                    server.broadcastMessage(username + ": " + msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();

    }
    public void sendMessage(String message) throws IOException {
        out.writeUTF(message);
    }

    public void disconnect() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}