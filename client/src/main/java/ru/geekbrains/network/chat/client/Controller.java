package ru.geekbrains.network.chat.client;

import com.sun.java_cup.internal.runtime.Scanner;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;


public class Controller implements Initializable {
    @FXML
    TextField msgField, loginField;
    @FXML
    TextField passwordField;
    @FXML
    TextArea msgArea;
    @FXML
    HBox loginPanel, msgPanel;
    @FXML
    ListView<String> clientsList;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private static final String SPACE = " ";
    private static final String PREFIX_HISTORY = "history_";
    private static final String ROOT_PATH_HISTORY = "E:\\java-courses-practice\\Java write\\network-chat\\history\\";
    private static final String SUFFIX_TXT = ".txt";


    public void setUsername(String username) {
        this.username = username;
        if (username != null) {
            loginPanel.setVisible(false);
            loginPanel.setManaged(false);
            msgPanel.setVisible(true);
            msgPanel.setManaged(true);
            clientsList.setVisible(true);
            clientsList.setManaged(true);
        } else {
            loginPanel.setVisible(true);
            loginPanel.setManaged(true);
            msgPanel.setVisible(false);
            msgPanel.setManaged(false);
            clientsList.setVisible(false);
            clientsList.setManaged(false);
        }
    }

    private void writeToFile() {
        String fileName = MessageFormat.format("{0}{1}{2}{3}", ROOT_PATH_HISTORY, PREFIX_HISTORY, username, SUFFIX_TXT);
        String msg = msgField.getText() + "\n";
        try (FileOutputStream fos = new FileOutputStream(fileName, true)) {
            fos.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void readList() {
        Path historyPath = Paths.get("E:\\java-courses-practice\\Java write\\network-chat\\history");
        String pathToFile = MessageFormat.format("{0}{1}{2}{3}", ROOT_PATH_HISTORY, PREFIX_HISTORY, username, SUFFIX_TXT);
        if (historyPath.toFile().isDirectory() && historyPath.toFile().exists()) {
            String fileName = PREFIX_HISTORY + username + SUFFIX_TXT;//history_vik123.txt
            File[] files = historyPath.toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().equals(fileName)) {
                        try {
                            List<String> lines = Files.readAllLines(Paths.get(pathToFile));
                            StringBuilder sb = new StringBuilder();
                            if (lines.size() >= 10) {
                                for (int i = lines.size() - 10; i < lines.size(); i++) {
                                    sb.append(lines.get(i)).append("\n");
                                }
                            } else {
                                for (String line : lines) {
                                    sb.append(line).append("\n");
                                }
                            }
                            msgArea.setText(sb.toString());

                        } catch (IOException ioException) {
                            throw new RuntimeException("error reading history file");
                        }
                    }
                }

            }


        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUsername(null);
    }

    public void login() {
        if (socket == null || socket.isClosed()) {
            connect();

        }
        if (loginField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Имя пользователя не может быть пустым", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        try {//получится строка вида - /login vika 123456
            String login = loginField.getText();
            String password = passwordField.getText();

//            out.writeUTF("/login" + SPACE + login + SPACE + password);
//            out.writeUTF(String.format("%s %s %s", "/login",login,password));
            out.writeUTF(MessageFormat.format("{0} {1} {2}", "/auth", login, password));

//            out.writeUTF(new StringBuilder().
//                        append("/login").
//                        append(SPACE).
//                        append(login).
//                        append(SPACE).
//                        append(password).toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logout() {
        System.out.println("logout ======");
        try {
            out.writeUTF("/logout");
            setUsername(null);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        loginField.clear();
        passwordField.clear();
        msgArea.clear();
    }


    public void connect() {
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            Thread t = new Thread(() -> {
                try {
                    // цикл авторизации
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/login_failed ")) {
                            String message = msg.substring(14);
                            msgArea.appendText(message);
                            break;
                        }

                        if (msg.startsWith("/login_ok")) {
                            username = msg.split(" ")[1];
                            setUsername(username);
                            msgArea.appendText("Вы зашли под ником: " + username + "\n");
                            readList();
                            break;
                        }

                        msgArea.appendText(msg + '\n');

                    }
                    // цикл общения
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/")) {
                            if (msg.equals("/end_confirm")) {
                                msgArea.appendText("Завершено общение с сервером\n");
                                break;
                            }
                            if (msg.startsWith("/set_nick_to")) {
                                username = msg.split(" ")[1];
                                msgArea.appendText("Ваш новый ник: " + username + "\n");
                                continue;
                            }
                            if (msg.startsWith("/clients_list ")) {
                                Platform.runLater(() -> {
                                    clientsList.getItems().clear();
                                    String[] tokens = msg.split(" ");
                                    for (int i = 1; i < tokens.length; i++) {
                                        if (!username.equals(tokens[i])) {
                                            clientsList.getItems().add(tokens[i]);
                                        }
                                    }
                                });

                            }
                            continue;
                        }

                        msgArea.appendText(msg + '\n');
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    disconnect();
                }
            });
            t.start();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно подключиться к серверу ", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void sendMsg() {
        try {
            out.writeUTF(msgField.getText());
            writeToFile();

            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, " Невозможно отправить сообщение", ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void disconnect() {
        setUsername(null);
        try {
            if (socket != null) {
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
