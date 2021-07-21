package ru.geekbrains.network.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp {

    /* 1.пусть сервер подсчитывает количество сообщений от клиента
       2.Если клиент отправил команду '/stat' , то сервер должен выслать клиенту не эхо ,
         а сообщение вида " Количество сообщений - n"
    */
    public static void main(String[] args)  {
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            System.out.println(" Сервер запущен на порту 8189. Ожидаем подключение клиента... ");
            Socket socket = serverSocket.accept();
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            System.out.println(" Клиент подключился ");
            int msgCounter = 0;

            while (true) {
                String msg = in.readUTF();
                System.out.print(msg);
                if(msg.startsWith("/")) {
                    if(msg.equals("/stat")) {
                        out.writeUTF("Messages count: " + msgCounter);
                        continue;
                    }
                }
                out.writeUTF("ECHO:" + msg);
                msgCounter++;

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
