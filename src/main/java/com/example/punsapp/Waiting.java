package com.example.punsapp;

import com.google.gson.Gson;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Waiting extends Application {
    String username;
    int roomId = 0;
    int waitingPlayersCount;
    private static final int PORT = 3000;
    Socket serverSocket = new Socket("localhost", PORT);
    private volatile boolean isListening = true; // Flag to control the listening thread
    private List<Label> playerLabels = new ArrayList<>();

    public Waiting(String username) throws IOException {
        this.username = username;
        waitingPlayersCount = 0;
    }

    public void start(Stage primaryStage) throws IOException {
        primaryStage.setTitle("Charades Game - Waiting room");

        VBox layout = new VBox(10);
        layout.setAlignment(Pos.TOP_RIGHT);
        layout.setPadding(new Insets(20));

        Button createButton = new Button("Create");
        layout.getChildren().add(createButton);

        Scene scene = new Scene(layout, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();

        Thread threadListener = new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                String messageServer;

                Gson gson = new Gson();

                createButton.setOnAction(e -> {
                    if (!username.isEmpty()) {
                        try {
                            PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
                            //openMainApp(username, serverSocket);
                            //stopListening();
                            Message message = new Message();
                            message.setMessageType("CREATE_ROOM");
                            String json = gson.toJson(message);
                            out.println(json);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        //primaryStage.close(); // Close the login window after submission
                    }
                });

                while (isListening && (messageServer = in.readLine()) != null) {
                    System.out.println("Received message from server: " + messageServer);

                    Message message = gson.fromJson(messageServer, Message.class);

                    if (Objects.equals(message.getMessageType(), "CREATE_ROOM")) {
                        Platform.runLater(() -> {
                            VBox newLabel = createPlayerLabel((int) message.getX());
                            layout.getChildren().add(newLabel);
                        });
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
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        threadListener.setDaemon(true);
        threadListener.start();
    }

    private VBox createPlayerLabel(int componentId) {
        Label label = new Label("Player " + (playerLabels.size() + 1));
        Button joinButton = new Button("Join");
        Button startButton = new Button("Start");

        joinButton.setId(String.valueOf(componentId)); // Nadanie ID przyciskowi "Join"
        startButton.setId(String.valueOf(componentId)); // Nadanie ID przyciskowi "Start"

        VBox labelLayout = new VBox(5, label, joinButton, startButton);
        labelLayout.setPadding(new Insets(10));
        labelLayout.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

        joinButton.setOnAction(event -> {
            // Action on join button click
            // Implement the join action here
            Button clickedButton = (Button) event.getSource();
            String buttonId = clickedButton.getId();
            int index = Integer.parseInt(buttonId);

            roomId = index;

            try {
                PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
                Message message = new Message();
                message.setMessageType("JOIN_ROOM");
                message.setRoomId(index);
                message.setUsername(username);
                String json = new Gson().toJson(message, Message.class);
                out.println(json);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        startButton.setOnAction(event -> {
            // Action on start button click
            // Implement the start action here
            Button clickedButton = (Button) event.getSource();
            String buttonId = clickedButton.getId();
            int index = Integer.parseInt(buttonId);
            // Tu możesz wykonać działania na przycisku "Start" z identyfikatorem "index"
            System.out.println("start " + index);
        });

        playerLabels.add(label);
        return labelLayout;
    }

    public static void main(String[] args) {
        launch(args);
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
}

