package ru.geekbrains.network.chat.server;

import java.sql.*;

public class DbAuthManager implements AuthManager {
    private Connection connection;
    private Statement stmt;
    private PreparedStatement psGetNicknameByLoginAndPassword;
    private PreparedStatement psChangeNickname;
    private PreparedStatement psGetUserByNickname;

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        try {
            psGetNicknameByLoginAndPassword.setString(1, login);
            psGetNicknameByLoginAndPassword.setString(2, password);
            try(ResultSet rs = psGetNicknameByLoginAndPassword.executeQuery()) {
                if(!rs.next()) {
                    return  null;
                }
                return rs.getString(1);
            }
        } catch (SQLException throwables) {
            throw new AuthServiceException("Unable to get name by login/password");
        }

    }

    @Override
    public boolean changeNickname(String oldNickname, String newNickname) {
        try {
            if(isNicknameExists(newNickname)) {
                return false;
            }
            psChangeNickname.setString(1, newNickname);
            psChangeNickname.setString(2, oldNickname);
            psChangeNickname.executeUpdate();
            return true;

        } catch (SQLException throwables) {
            throw new AuthServiceException("Unable to get name");
        }
    }
    public boolean isNicknameExists(String nickname) {
        try {

            psGetUserByNickname.setString(1, nickname);
            try(ResultSet rs = psGetUserByNickname.executeQuery()) {
                if(rs.next()) {
                    return true;
                }
                return false;
            }

        } catch (SQLException throwables) {
            throw new AuthServiceException("Unable to get name");
        }
    }

    @Override
    public void start() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:users.db");
            stmt = connection.createStatement();
            checkUsersTable();
            psGetNicknameByLoginAndPassword = connection.prepareStatement("SELECT name FROM users WHERE login = ? AND password = ?;");
            psChangeNickname = connection.prepareStatement("UPDATE users SET name = ? WHERE name = ?;");
            psGetUserByNickname = connection.prepareStatement("SELECT * FROM users WHERE name = ?;");

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw new AuthServiceException("Unable to connect to users database");
        }

    }

    public void checkUsersTable() throws SQLException {
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, name STRING, login STRING, password STRING);");
    }


    @Override
    public void stop() {
        if(psChangeNickname != null) {
            try {
                psChangeNickname.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        if(psGetNicknameByLoginAndPassword != null) {
            try {
                psGetNicknameByLoginAndPassword.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        if(psGetUserByNickname != null) {
            try {
                psGetUserByNickname.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        if(stmt != null) {
            try {
                stmt.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        if(connection != null) {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }
}
