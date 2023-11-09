package com.example.punsapp;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    private TextArea chatArea = new TextArea();
    private TextField inputField = new TextField();

    private Canvas canvas;
    private GraphicsContext gc;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("JavaFX Combined App");

        // Drawing Tab
        Pane drawingPane = createDrawingTab();

        // Chat Tab
        Pane chatPane = createChatTab();

        // SplitPane to divide the window into two halves
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(drawingPane, chatPane);
        splitPane.setDividerPositions(0.5);

        Scene scene = new Scene(splitPane, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Pane createDrawingTab() {
        canvas = new Canvas(400, 300);
        gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(2.0);

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> clearCanvas());

        ColorPicker colorPicker = new ColorPicker(Color.BLACK);
        colorPicker.setOnAction(e -> setPenColor(colorPicker.getValue()));

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
            gc.stroke();
        });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
        });

        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            gc.closePath();
        });

        VBox drawingPane = new VBox(10);
        drawingPane.getChildren().addAll(canvas, colorPicker, clearButton);
        return drawingPane;
    }

    private void clearCanvas() {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
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
        inputField.setOnAction(e -> sendMessage());
        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());

        VBox inputContainer = new VBox(10);
        inputContainer.setPadding(new Insets(10));
        inputContainer.getChildren().addAll(inputField, sendButton);
        borderPane.setBottom(inputContainer);

        return borderPane;
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (!message.isEmpty()) {
            chatArea.appendText("You: " + message + "\n");
            inputField.clear();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
