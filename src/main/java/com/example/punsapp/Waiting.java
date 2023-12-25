package com.example.punsapp;

import com.google.gson.Gson;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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

        ScrollPane scrollPane = new ScrollPane();
        VBox layout = new VBox(10);
        scrollPane.setContent(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        layout.setAlignment(Pos.TOP_RIGHT);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #F2F1EB");

        Button createButton = new Button("Create");
        createButton.setStyle("-fx-background-color: #88AB8E; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8px 16px;");
        layout.getChildren().add(createButton);

        Scene scene = new Scene(scrollPane, 600, 450);
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
                            Message message = new Message();
                            message.setMessageType("CREATE_ROOM");
                            message.setUsername(username);
                            String json = gson.toJson(message);
                            out.println(json);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                });

                while (isListening && (messageServer = in.readLine()) != null) {
                    System.out.println("Received message from server: " + messageServer);

                    Message message = gson.fromJson(messageServer, Message.class);

                    if (Objects.equals(message.getMessageType(), "CREATE_ROOM")) {
                        Platform.runLater(() -> {
                            VBox newLabel = createPlayerLabel((int) message.getX(), 0);
                            layout.getChildren().add(newLabel);
                        });
                    } else if (Objects.equals(message.getMessageType(), "PLAYERS_COUNT_UPDATE")) {
                        Platform.runLater(() -> {
                            int roomIdToUpdate = (int) message.getRoomId();
                            int updatedPlayerCount = (int) message.getY(); // Updated player count

                            for (Label label : playerLabels) {
                                if (Integer.parseInt(label.getId()) == roomIdToUpdate) {
                                    label.setText("Room " + roomIdToUpdate + " - " + updatedPlayerCount + "/4");
                                    break; // Found the label for the room, update completed
                                }
                            }
                        });
                    } else if (Objects.equals(message.getMessageType(), "START")) {
                        if(Objects.equals(message.getRoomId(), roomId)) {
                            Platform.runLater(() -> {
                                try {
                                    openMainApp(username, roomId);
                                    stopListening();
                                    primaryStage.close(); // Close the waiting room window
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            });
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        threadListener.setDaemon(true);
        threadListener.start();
    }

    private VBox createPlayerLabel(int componentId, int userCount) {
        Label label = new Label("Room " + componentId + " - " + userCount + "/4");

        Button joinButton = new Button("Join");
        Button startButton = new Button("Start");

        joinButton.setStyle("-fx-background-color: #88AB8E; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8px 16px;");
        startButton.setStyle("-fx-background-color: #88AB8E; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8px 16px;");

        label.setId(String.valueOf(componentId));
        joinButton.setId(String.valueOf(componentId)); // Nadanie ID przyciskowi "Join"
        startButton.setId(String.valueOf(componentId)); // Nadanie ID przyciskowi "Start"

        VBox labelLayout = new VBox(5, label, joinButton, startButton);
        labelLayout.setPadding(new Insets(10));
        labelLayout.setStyle("-fx-background-color: #AFC8AD");

        joinButton.setOnAction(event -> {
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

                final int roomIndexToUpdate = index;  // Ustalamy finalną zmienną, aby była dostępna w lambdzie
                // Aktualizacja etykiety po naciśnięciu przycisku "Join"
                Platform.runLater(() -> {
                    for (Label tmp : playerLabels) {
                        if (Integer.parseInt(tmp.getId()) == roomIndexToUpdate) {
                            tmp.setText("Room " + roomIndexToUpdate + " - " + (tmp.getText() + 1) + "/4");
                            break; // Znaleziono etykietę dla danego pokoju, aktualizacja zakończona
                        }
                    }
                });
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        startButton.setOnAction(event -> {
            Button clickedButton = (Button) event.getSource();
            String buttonId = clickedButton.getId();
            int index = Integer.parseInt(buttonId);
            // Tu możesz wykonać działania na przycisku "Start" z identyfikatorem "index"
            System.out.println("start przycisk " + index);


                try {
                    PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
                    Message message = new Message();
                    message.setMessageType("START");
                    message.setRoomId(index);
                    message.setUsername(username);
                    String json = new Gson().toJson(message, Message.class);
                    out.println(json);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
        });

        playerLabels.add(label);
        return labelLayout;
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void openMainApp(String username, int roomId) throws IOException {
        MainWindow mainApp = new MainWindow(username, roomId);
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

