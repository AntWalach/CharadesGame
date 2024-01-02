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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.*;

// This class represents the waiting room GUI
public class WaitingRoom extends Application {
    // Instance variables
    String username;
    int roomId = 0;
    int waitingPlayersCount;

    private static final int PORT = 3000;
    Socket serverSocket = new Socket("localhost", PORT);
    private volatile boolean isListening = true; // Flag to control the listening thread

    // Lists to manage GUI components dynamically
    private List<Label> playerLabels = new ArrayList<>();
    private Map<Integer, Button> startButtons = new HashMap<>();
    private Map<Integer, Button> joinButtons = new HashMap<>();

    Label roomLabel;
    Button startButton;

    // Constructor initializing Waiting instance with a username
    public WaitingRoom(String username) throws IOException {
        this.username = username;
        waitingPlayersCount = 0;
    }

    // Method to set up the waiting room GUI
    public void start(Stage primaryStage) throws IOException {
        // Setting up the main window
        primaryStage.setTitle("Charades Game - Waiting room");

        // Creating a scrollable layout for the Waiting room
        ScrollPane scrollPane = new ScrollPane();
        VBox layout = new VBox(10);
        scrollPane.setContent(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        // Creating the top row containing room label and buttons
        HBox topRow = new HBox(10); // HBox for top row alignment
        topRow.setAlignment(Pos.CENTER_LEFT); // Align items to the left
        topRow.setPadding(new Insets(20));

        roomLabel = new Label("Room :");
        roomLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        topRow.getChildren().add(roomLabel); // Add roomLabel to the top row

        // Spacer to push the button to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Buttons for leaving room and creating a new room
        Button leaveRoomButton = new Button("Leave room");
        leaveRoomButton.setStyle("-fx-background-color: #88AB8E; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8px 16px;");

        Button createButton = new Button("Create");
        createButton.setStyle("-fx-background-color: #88AB8E; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8px 16px;");

        // Adding elements to the top row
        topRow.getChildren().addAll(spacer, leaveRoomButton, createButton); // Add spacer and createButton to the top row

        layout.getChildren().add(topRow); // Add the top row (HBox) to the main VBox

        // Creating the scene and displaying the window
        Scene scene = new Scene(scrollPane, 600, 450);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Thread to listen for server messages
        Thread threadListener = new Thread(() -> {
            try {
                // Setting up input stream to receive messages from the server
                BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                String messageServer;

                Gson gson = new Gson();

                // Actions when buttons are clicked
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

                leaveRoomButton.setOnAction(e -> {
                    if (!username.isEmpty()) {
                        try {
                            PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
                            Message message = new Message();
                            message.setRoomId(roomId);
                            message.setMessageType("LEAVE_ROOM");
                            message.setUsername(username);
                            String json = gson.toJson(message);
                            out.println(json);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    roomId = 0;
                });

                // Listening for messages from the server
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
                                    label.setText("Room " + roomIdToUpdate + " - " + updatedPlayerCount + " player/s");
                                    break; // Found the label for the room, update completed
                                }
                            }
                        });
                    } else if (Objects.equals(message.getMessageType(), "START")) {
                        Button specificStartButton = startButtons.get(message.getRoomId());
                        Button specificJoinButton = joinButtons.get(message.getRoomId());
                        if (specificStartButton != null && specificJoinButton != null) {
                            specificStartButton.setDisable(true);
                            specificJoinButton.setDisable(true);
                        }
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
        // Starting the listening thread
        threadListener.setDaemon(true);
        threadListener.start();
    }

    // Method to create player label and associated buttons
    private VBox createPlayerLabel(int componentId, int userCount) {
        Label label = new Label("Room " + componentId + " - " + userCount + " player/s");

        Button joinButton = new Button("Join");
        startButton = new Button("Start");

        joinButton.setStyle("-fx-background-color: #88AB8E; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8px 16px;");
        startButton.setStyle("-fx-background-color: #88AB8E; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8px 16px;");

        label.setId(String.valueOf(componentId));
        joinButton.setId(String.valueOf(componentId));
        startButton.setId(String.valueOf(componentId));

        startButtons.put(componentId, startButton);
        joinButtons.put(componentId, joinButton);

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
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        startButton.setOnAction(event -> {
            Button clickedButton = (Button) event.getSource();
            String buttonId = clickedButton.getId();
            int index = Integer.parseInt(buttonId);

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

    // Main method (entry point)
    public static void main(String[] args) {
        launch(args);
    }

    // Method to open the main app window
    private void openMainApp(String username, int roomId) throws IOException {
        MainWindow mainApp = new MainWindow(username, roomId);
        try {
            mainApp.start(new Stage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to stop listening to the server
    public void stopListening() {
        isListening = false;
    }
}

