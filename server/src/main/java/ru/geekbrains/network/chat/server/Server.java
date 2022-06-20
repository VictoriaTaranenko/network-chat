package ru.geekbrains.network.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private List<ClientHandler> clients;
    private AuthManager authManager;

    public AuthManager getAuthManager() {
        return authManager;
    }

    public Server (int port) {
        this.clients = new ArrayList<>();
        this.authManager = new DbAuthManager();
        authManager.start();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(" Сервер запущен на порту " + port);
            while (true) {
                System.out.println("Ждем нового клиента ...");
                Socket socket = serverSocket.accept();
                System.out.println("Клиент подключился ");
                new ClientHandler(this,socket);
            }
        } catch (IOException  e) {
            e.printStackTrace();
        } finally {
            authManager.stop();
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler)   {
        clients.add(clientHandler);
        broadcastMessage("Клиент " + clientHandler.getUsername() + " вошел в чат ");
        broadcastClientsList();
    }

    public synchronized void unsubscribe(ClientHandler clientHandler)  {
        clients.remove(clientHandler);
        broadcastMessage("Клиент " + clientHandler.getUsername() + " вышел из чата ");
        broadcastClientsList();


    }
    public synchronized void broadcastMessage(String message) {
        for(ClientHandler clientHandler : clients) {
            clientHandler.sendMessage(message);
        }
    }
    public synchronized void sendPrivateMessage(ClientHandler sender, String receiverUsername, String message) {
        for(ClientHandler c : clients) {
            if(c.getUsername().equals(receiverUsername)) {
                c.sendMessage("От: " + sender.getUsername() + " Собщение: " + message);
                sender.sendMessage(" Пользователю: " + receiverUsername + " Сообщение: " + message);
                return;
            }
        }
        sender.sendMessage(" Невозможно отправить сообщение пользователю " + receiverUsername + " . Такого пользователя нет в списке");
    }
    public synchronized boolean isUserOnline(String username) {
        for(ClientHandler clientHandler : clients) {
        if(clientHandler.getUsername().equals(username)) {
            return true;
          }
        }
        return false;
    }
     public synchronized void  broadcastClientsList()  {
        StringBuilder stringBuilder = new StringBuilder("/clients_list ");
        for(ClientHandler c : clients) {
            stringBuilder.append(c.getUsername()).append(" ");
        }
        stringBuilder.setLength(stringBuilder.length() -1);
        String clientsList = stringBuilder.toString();
         for(ClientHandler clientHandler : clients) {
             clientHandler.sendMessage(clientsList);
         }
     }
}
