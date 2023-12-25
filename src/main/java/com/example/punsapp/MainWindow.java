package com.example.punsapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.util.Objects;


public class MainWindow extends Application {

    private String username;
    private TextArea chatArea = new TextArea();
    private TextField inputField = new TextField();
    private Canvas canvas;
    private GraphicsContext gc;
    private Label timerLabel;
    private Label chatLabel;
    private Label turnLabel;
    private Label wordLabel;

    Socket serverSocket;
    private final int roomId;

    private long lastClearTime = 0;
    private static final long CLEAR_COOLDOWN = 1000; // Cooldown time in milliseconds
    private boolean drawingPermission = false;

    public MainWindow(String username, int roomId) throws IOException {
        this.username = username;
        this.roomId = roomId;
        serverSocket = new Socket("localhost", 3000 + roomId);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {

        PrintWriter out1 = new PrintWriter(serverSocket.getOutputStream(), true);
        Message clientMessage = new Message();
        clientMessage.setMessageType("SET_USERNAME");
        clientMessage.setUsername(username);
        String json1 = new Gson().toJson(clientMessage);
        out1.println(json1);

        primaryStage.setTitle("Charades Game - " + username);

        // Drawing Tab
        Pane drawingPane = createDrawingTab();

        // Chat Tab
        Pane chatPane = createChatTab();

        // SplitPane to divide the window into two halves
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: #AFC8AD");
        splitPane.getItems().addAll(drawingPane, chatPane);
        splitPane.setDividerPositions(0.5);

        Scene scene = new Scene(splitPane, 800, 580); //window size
        primaryStage.setScene(scene);
        primaryStage.show();

        inputField.setOnAction(e -> {
            sendChat(inputField.getText());
            inputField.clear();
        });

        Thread serverListenerThread = new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                String messageServer;

                Gson gson = new Gson();

                while ((messageServer = in.readLine()) != null) {
                    System.out.println("Received message from server: " + messageServer);

                    Message message = gson.fromJson(messageServer, Message.class);

                    if(Objects.equals(message.getRoomId(), roomId)) {
                        switch (message.getMessageType()) {
                            case "XY":
                                double x = message.getX();
                                double y = message.getY();
                                Platform.runLater(() -> handleReceivedCoordinates(x, y));
                                break;
                            case "CLEAR_CANVAS":
                                Platform.runLater(this::clearCanvas);
                                break;
                            case "COUNTDOWN":
                                int countdownValue = (int) message.getX();
                                Platform.runLater(() -> updateTimerLabel(countdownValue));
                                break;
                            case "PERMISSION":
                                drawingPermission = Objects.equals(message.getChat(), username);
                                break;
                            case "COLOR_CHANGE":
                                String color = message.getColor();
                                Platform.runLater(() -> setPenColor(Color.web(color)));
                                break;
                            case "CLEAR_CHAT":
                                Platform.runLater(() -> chatArea.clear());
                                break;
                            case "CLEAR_LEADERBOARD":
                                Platform.runLater(() -> chatLabel.setText("Leaderboard"));
                                break;
                            case "LEADERBOARD":
                                Platform.runLater(() -> chatLabel.setText(chatLabel.getText() + "\n" + message.getUsername() + " : " + (int) message.getX()));
                                break;
                            case "TURN_INFO":
                                Platform.runLater(() -> turnLabel.setText("Turn to draw: " + message.getChat()));
                                break;
                            case "WORD_INFO":
                                Platform.runLater(() -> wordLabel.setText("Word: " + message.getChat()));
                                break;
                            case "CLEAR_WORD_LABEL":
                                Platform.runLater(() -> wordLabel.setText(""));
                                break;
                            default:
                                String messageUsername = message.getUsername();
                                String finalMessage = message.getChat();
                                Platform.runLater(() -> chatArea.appendText(messageUsername + ": " + finalMessage + "\n"));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverListenerThread.setDaemon(true);
        serverListenerThread.start();
    }

    private void handleReceivedCoordinates(double x, double y) {
        Platform.runLater(() -> {
            gc.fillOval(x, y, 3, 3); // Draw a small circle at the received coordinates
        });
    }

    private Pane createDrawingTab() {
        canvas = new Canvas(600, 400);
        gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(2.0);

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> handleClearButtonClick());

        ColorPicker colorPicker = new ColorPicker(Color.BLACK);

        colorPicker.setOnAction(e -> {
            Color newColor = colorPicker.getValue();
            setPenColor(newColor);
        });


        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (drawingPermission) {
                gc.beginPath();
                gc.moveTo(e.getX(), e.getY());
                gc.setStroke(colorPicker.getValue());
                gc.stroke();

                sendCoordinatesToServer(e.getX(), e.getY(), colorPicker.getValue());
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (drawingPermission) {
                gc.lineTo(e.getX(), e.getY());
                gc.setStroke(colorPicker.getValue());
                gc.stroke();

                sendCoordinatesToServer(e.getX(), e.getY(), colorPicker.getValue());
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (drawingPermission) {
                gc.lineTo(e.getX(), e.getY());
                gc.stroke();
                gc.closePath();

                // Send coordinates to the server
                sendCoordinatesToServer(e.getX(), e.getY(), colorPicker.getValue());
            }
        });

        timerLabel = new Label("01:00"); // Initial label text
        timerLabel.setStyle("-fx-font-size: 25px; -fx-control-inner-background: #FFFFFF;");

        turnLabel = new Label("Turn to draw: ");
        turnLabel.setStyle("-fx-font-size: 20px; -fx-control-inner-background: #FFFFFF; -fx-font-weight: bold;");

        wordLabel = new Label();
        wordLabel.setStyle("-fx-font-size: 20px; -fx-control-inner-background: #FFFFFF; -fx-font-weight: bold;");

        turnLabel.setMinWidth(Label.USE_PREF_SIZE);
        wordLabel.setMinWidth(Label.USE_PREF_SIZE);

        HBox labelBox = new HBox(30);
        labelBox.getChildren().addAll(turnLabel, wordLabel);

        clearButton.setStyle("-fx-background-color: #88AB8E; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8px 16px;");

        VBox drawingPane = new VBox(10);
        drawingPane.setStyle("-fx-background-color: #F2F1EB;"); // Set background color for the VBox containing the Canvas
        drawingPane.getChildren().addAll(labelBox, canvas, colorPicker, clearButton, timerLabel);
        return drawingPane;
    }

    private void updateTimerLabel(int countdownValue) {
        Platform.runLater(() -> {
            int minutes = countdownValue / 60;
            int seconds = countdownValue % 60;

            String formattedTime = String.format("%02d:%02d", minutes, seconds);
            timerLabel.setText(formattedTime);
        });
    }

    private void handleClearButtonClick() {
        if (drawingPermission) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClearTime > CLEAR_COOLDOWN) {
                clearCanvas();
                Message message = new Message();
                message.setRoomId(roomId);
                message.setUsername(username);
                message.setMessageType("CLEAR_CANVAS");
                sendMessage(message, serverSocket);
                lastClearTime = currentTime;
            }
        }
    }

    private void clearCanvas() {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        Message message = new Message();
        message.setRoomId(roomId);
        message.setUsername(username);
        message.setMessageType("CLEAR_CANVAS");
        sendMessage(message, serverSocket);
    }

    private void setPenColor(Color newColor) {
        Color currentColor = (Color) gc.getStroke();

        if (!currentColor.equals(newColor)) {
            gc.setStroke(newColor);
            gc.setFill(newColor);
            sendColorToServer(newColor);
            System.out.println("Local color set: " + newColor);
        }
    }

    private Pane createChatTab() {
        BorderPane borderPane = new BorderPane();

        // Chat Area
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        VBox chatVBox = new VBox(chatArea);
        chatVBox.setPadding(new Insets(10));
        VBox.setVgrow(chatArea, Priority.NEVER); // Allow chatArea to grow

        // Label on the chat side
        chatLabel = new Label("Leaderboard");
        chatLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        VBox labelVBox = new VBox(chatLabel);
        labelVBox.setAlignment(Pos.CENTER);
        labelVBox.setPadding(new Insets(10));
        VBox.setVgrow(chatLabel, Priority.ALWAYS);

        // Input Field and Send Button
        inputField.setPromptText("Type your message...");
        inputField.setOnAction(e -> sendChat(inputField.getText()));
        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendChat(inputField.getText()));

        sendButton.setStyle("-fx-background-color: #88AB8E; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8px 16px; ");
        chatArea.setStyle("-fx-font-size: 14px; -fx-background-color: #FFFFFF; -fx-control-inner-background: #FFFFFF;");
        inputField.setStyle("-fx-font-size: 14px; -fx-background-color: #FFFFFF; -fx-prompt-text-fill: #A9A9A9;");

        VBox chatAndLabelVBox = new VBox();
        chatAndLabelVBox.getChildren().addAll(labelVBox);

        VBox inputContainer = new VBox(10);
        inputContainer.setPadding(new Insets(10));
        inputContainer.getChildren().addAll(chatArea, inputField, sendButton);

        VBox finalChatVBox = new VBox();
        finalChatVBox.getChildren().addAll(chatAndLabelVBox); // Reversed order: label/chat above chatArea

        borderPane.setTop(finalChatVBox);
        borderPane.setBottom(inputContainer); // Place inputContainer at the bottom

        // Align chatVBox and inputContainer to the bottom of the BorderPane
        BorderPane.setAlignment(chatVBox, Pos.BOTTOM_CENTER);
        BorderPane.setAlignment(inputContainer, Pos.BOTTOM_CENTER);

        return borderPane;
    }



    private void sendMessage(Message message, Socket serverSocket) {
        try {
            PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);

            Gson gson = new Gson();
            String json = gson.toJson(message);

            out.println(json);
            System.out.println("Sent message to server: " + json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendChat(String chat) {
        Message message = new Message();
        message.setRoomId(roomId);
        message.setUsername(username);
        message.setMessageType("CHAT");
        message.setChat(chat);

        sendMessage(message, serverSocket);
    }

    private void sendCoordinatesToServer(double x, double y, Color color) {
        Message message = new Message();
        message.setRoomId(roomId);
        message.setUsername(username);
        message.setMessageType("XY");
        message.setX(x);
        message.setY(y);
        message.setColor(color.toString());

        sendMessage(message, serverSocket);
    }

    private void sendColorToServer(Color color) {
        Message message = new Message();
        message.setRoomId(roomId);
        message.setUsername(username);
        message.setMessageType("COLOR_CHANGE");
        message.setColor(color.toString()); // Convert Color to String for simplicity

        sendMessage(message, serverSocket);
    }
}
