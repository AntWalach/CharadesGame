package com.example.punsapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
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


public class MainWindow extends Application implements ServerListener {

    private String username;
    private TextArea chatArea = new TextArea();
    private TextField inputField = new TextField();
    private Canvas canvas;
    private GraphicsContext gc;
    private Label timerLabel;
    private int countdownSeconds = 60;

    private static final int PORT = 3000;
    Socket serverSocket;

    private long lastClearTime = 0;
    private static final long CLEAR_COOLDOWN = 1000; // Cooldown time in milliseconds
    private boolean drawingPermission = false;

    public MainWindow(String username, Socket serverSocket) throws IOException {
        this.username = username;
        this.serverSocket = serverSocket;
    }

    @Override
    public void start(Stage primaryStage) throws IOException {

        primaryStage.setTitle("Charades Game");

        // Drawing Tab
        Pane drawingPane = createDrawingTab();

        // Chat Tab
        Pane chatPane = createChatTab();

        // SplitPane to divide the window into two halves
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(drawingPane, chatPane);
        splitPane.setDividerPositions(0.5);

        Scene scene = new Scene(splitPane, 600, 420); //window size
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

                    if (Objects.equals(message.getMessageType(), "XY")) {
                        double x = message.getX();
                        double y = message.getY();

                        Platform.runLater(() -> handleReceivedCoordinates(x, y));
                    } else if (Objects.equals(message.getMessageType(), "CLEAR_CANVAS")) {
                        Platform.runLater(this::clearCanvas);
                    } else if (Objects.equals(message.getMessageType(), "COUNTDOWN")) {
                        int countdownValue = (int) message.getX();
                        updateTimerLabel(countdownValue);
                    } else if (Objects.equals(message.getMessageType(), "PERMISSION")) {
                        if(Objects.equals(message.getChat(), username)){
                            drawingPermission = true;
                        }
                        else {
                            drawingPermission = false;
                        }
                    } else {
                        String messageUsername = message.getUsername();
                        String finalMessage = message.getChat();
                        Platform.runLater(() -> chatArea.appendText(messageUsername + ": " + finalMessage + "\n"));
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
            // Draw on the canvas with the received coordinates
            gc.fillOval(x, y, 3, 3); // Draw a small circle at the received coordinates
        });
    }

    private Pane createDrawingTab() {
        canvas = new Canvas(400, 300);
        gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(2.0);

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> handleClearButtonClick());

        ColorPicker colorPicker = new ColorPicker(Color.BLACK);
        colorPicker.setOnAction(e -> setPenColor(colorPicker.getValue()));

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if(drawingPermission) {
                gc.beginPath();
                gc.moveTo(e.getX(), e.getY());
                gc.stroke();

                sendCoordinatesToServer(e.getX(), e.getY());
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if(drawingPermission) {
                gc.lineTo(e.getX(), e.getY());
                gc.stroke();

                sendCoordinatesToServer(e.getX(), e.getY());
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if(drawingPermission) {
                gc.lineTo(e.getX(), e.getY());
                gc.stroke();
                gc.closePath();

                // Send coordinates to the server
                sendCoordinatesToServer(e.getX(), e.getY());
            }
        });

        timerLabel = new Label("01:00"); // Initial label text
        timerLabel.setStyle("-fx-font-size: 20;"); // Set font size

        VBox drawingPane = new VBox(10);
        drawingPane.getChildren().addAll(canvas, colorPicker, clearButton, timerLabel);
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
        if(drawingPermission) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClearTime > CLEAR_COOLDOWN) {
                clearCanvas();
                Message message = new Message();
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
        message.setUsername(username);
        message.setMessageType("CLEAR_CANVAS");
        sendMessage(message, serverSocket);
    }

    private void setPenColor(Color color) {
        gc.setStroke(color);
        gc.setFill(color);
    }

    private Pane createChatTab() {
        BorderPane borderPane = new BorderPane();

        // Chat Area
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        borderPane.setCenter(chatArea);

        // Input Field and Send Button
        inputField.setPromptText("Type your message...");
        inputField.setOnAction(e -> sendChat(inputField.getText()));
        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendChat(inputField.getText()));

        VBox inputContainer = new VBox(10);
        inputContainer.setPadding(new Insets(10));
        inputContainer.getChildren().addAll(inputField, sendButton);
        borderPane.setBottom(inputContainer);

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
        message.setUsername(username);
        message.setMessageType("CHAT");
        message.setChat(chat);

        sendMessage(message, serverSocket);
    }

    private void sendCoordinatesToServer(double x, double y) {
        Message message = new Message();
        message.setUsername(username);
        message.setMessageType("XY");
        message.setX(x);
        message.setY(y);

        sendMessage(message, serverSocket);
    }

    @Override
    public void onCoordinatesReceived(double x, double y) {
        handleReceivedCoordinates(x, y);
    }

    @Override
    public void onClearCanvasReceived() {
        Platform.runLater(this::clearCanvas);
    }

    @Override
    public void onChatMessageReceived(String message) {
        chatArea.appendText(message + "\n");
    }
}
