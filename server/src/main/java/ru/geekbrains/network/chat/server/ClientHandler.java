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
                    String msg = in.readUTF();//пришла строка вида - /login vik123 123456
                    System.out.print("Сообщение от клиента: " + msg + "\n");
                    if (msg.startsWith("/auth")) {
                        String[] tokens = msg.split(" ", 3);
                        String nickFromAuthManager = server.getAuthManager().getNicknameByLoginAndPassword(tokens[1], tokens[2]);
                        if (nickFromAuthManager != null) {
                            if (server.isUserOnline(nickFromAuthManager)) {
                                sendMessage("/login_failed nickname: " + nickFromAuthManager + " is already used");
                                continue;
                            }
                            username = nickFromAuthManager;
                            sendMessage("/login_ok " + username);
                            server.subscribe(this);
                            break;
                        } else{
                            sendMessage("Указан не верный логин/пароль");
                        }
                    }
                    if (msg.startsWith("/logout")) {
                        server.unsubscribe(this);
                        break;
                    }
                }
                // цикл общения с клиентом
                while (true) {
                    String msg = in.readUTF();
                    System.out.print("Сообщение от клиента: " + msg + "\n");
                   if(msg.startsWith("/")) {
                       if(msg.startsWith("/w")) {
                           String[] tokens = msg.split(" ",3);
                           server.sendPrivateMessage(this, tokens[1], tokens[2]);
                           continue;
                       }
                       if(msg.startsWith("/changenick")) {
                           String[] tokens = msg.split(" ",2);
                           String newNickname = tokens[1];
                           if(server.getAuthManager().changeNickname(username, newNickname)) {
                               username =newNickname;
                               sendMessage("/set_nick_to" + newNickname);
                           } else {
                               sendMessage("Сервер: не удалось сменить ник, ник уже занят");
                           }
                           continue;
                       }
                       if(msg.equals("/end")) {
                           sendMessage("end_confirm");
                           break;
                       }
                   } else {
                       server.broadcastMessage(username + " : " + msg);
                   }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();

    }

    public void executeCommand(String cmd) {
        if (cmd.startsWith("/w ")) {
            String[] tokens = cmd.split("\\s", 3);
            server.sendPrivateMessage(this, tokens[1], tokens[2]);
            return;
        }

    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            disconnect();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
