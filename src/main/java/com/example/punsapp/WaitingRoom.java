package com.example.punsapp;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;

public class WaitingRoom {
    String username;
    int waitingPlayersCount;

    private static final int PORT = 3000;
    Socket serverSocket = new Socket("localhost", PORT);
    private volatile boolean isListening = true; // Flag to control the listening thread


    public WaitingRoom(String username) throws IOException {
        this.username=username;
        waitingPlayersCount = 0;
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("Charades Game - Waiting room");

        Label textField = new Label("Waiting players: " + waitingPlayersCount + "/4");

        Button submitButton = new Button("Start");
//        submitButton.setOnAction(e -> {
//            if (!username.isEmpty()) {
//                try {
//                    openMainApp(username, serverSocket);
//                    disconnectFromServer();
//                } catch (IOException ex) {
//                    throw new RuntimeException(ex);
//                }
//                primaryStage.close(); // Close the login window after submission
//            }
//        });

        VBox layout = new VBox(10);
        layout.getChildren().addAll(textField, submitButton);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));

        Scene scene = new Scene(layout, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();

        Thread threadListener = new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                String messageServer;

                Gson gson = new Gson();

                submitButton.setOnAction(e -> {
                    if (!username.isEmpty()) {
                        try {
                            PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
                            //openMainApp(username, serverSocket);
                            //stopListening();
                            Message message = new Message();
                            message.setMessageType("START");
                            String json = gson.toJson(message);
                            out.println(json);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        primaryStage.close(); // Close the login window after submission
                    }
                });

                while (isListening && (messageServer = in.readLine()) != null) {
                    System.out.println("Received message from server: " + messageServer);

                    Message message = gson.fromJson(messageServer, Message.class);

                    if (Objects.equals(message.getMessageType(), "PLAYER_COUNT")) {
                        waitingPlayersCount = (int) message.getX();
                        updatePlayerCountLabel(textField);
                    } else if (Objects.equals(message.getMessageType(), "START")) {
                        Platform.runLater(() -> {
                            try {
                                openMainApp(username, serverSocket);
                                stopListening();
                                primaryStage.close(); // Close the waiting room window
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        });
                    }
                    else {
                        System.out.println("ERROR WAITING ROOM");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        threadListener.setDaemon(true);
        threadListener.start();
    }

    private void openMainApp(String username, Socket serverSocket) throws IOException {
        MainWindow mainApp = new MainWindow(username, serverSocket);
        try {
            mainApp.start(new Stage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void stopListening() {
        isListening = false;
    }

    private void updatePlayerCountLabel(Label label) {
        Platform.runLater(() -> label.setText("Waiting players: " + waitingPlayersCount + "/4"));
    }
}
