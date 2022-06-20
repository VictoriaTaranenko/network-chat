package ru.geekbrains.network.chat.server;

import java.sql.SQLException;

public interface AuthManager {
    String getNicknameByLoginAndPassword(String login, String password);
    boolean changeNickname(String oldNickname, String newNickname);
    void start();
    void stop();

}
