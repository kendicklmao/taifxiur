package com.auction.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain extends Application {

    private PrintWriter writer;
    private BufferedReader reader;
    private Socket socket;

    private TextArea chatBox;
    private TextField inputField;
    private Button sendButton;

    public static void main(String[] args) {
        launch(args);
    }

    public void updateChatBox(String msg) {
        Platform.runLater(() -> {
            chatBox.appendText(msg + "\n");
        });
    }

    private void connectToServer() {
        Thread connectionThread = new Thread(() -> {
            try {
                socket = new Socket("localhost", 8888);
                writer = new PrintWriter(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                updateChatBox("Successfully connected.");
                String serverMsg;
                while ((serverMsg = reader.readLine()) != null) {
                    updateChatBox(serverMsg);
                }
            } catch (Exception e) {
                System.out.println("Cannot connect to server.");
            }
        }
        );
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    private void sendMessage() {
        String myMsg = inputField.getText();
        if (!myMsg.trim().isEmpty()) {
            if (writer != null) {
                writer.println(myMsg);
                inputField.clear();
            } else {
                updateChatBox("Not connected to server.");
            }
        }
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start(Stage stage) {
        chatBox = new TextArea();
        chatBox.setEditable(false);
        chatBox.setPrefColumnCount(300);

        inputField = new TextField();
        inputField.setPromptText("Nhap tin nhan hoac lenh (VD: BID:50000)...");

        sendButton = new Button("Gui/ Dat gia");
        sendButton.setMaxWidth(Double.MAX_VALUE);

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.getChildren().addAll(chatBox, inputField, sendButton);

        Scene scene = new Scene(root, 400, 400);
        stage.setTitle("Online auction");
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> disconnect());
        stage.show();

        connectToServer();

        sendButton.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());
    }
}