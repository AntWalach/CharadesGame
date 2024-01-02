package com.example.punsapp;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

// Main class extending Application (required for JavaFX applications)
public class Main extends Application {

    // The entry point of the application
    public static void main(String[] args) {
        launch(args);
    }

    // Method that sets up the initial stage (login window)
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Charades Game - Login"); // Title of the window

        // Creating a text field for username input
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username"); // Placeholder text
        usernameField.setStyle("-fx-font-size: 14px; -fx-background-color: #FFFFFF; -fx-border-color: #88AB8E; -fx-prompt-text-fill: #A9A9A9;");

        // Creating a submit button
        Button submitButton = new Button("Submit");
        submitButton.setStyle("-fx-background-color: #88AB8E; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8px 16px;");
        submitButton.setOnAction(e -> {
            String username = usernameField.getText(); // Get the entered username
            if (!username.isEmpty()) {
                try {
                    openWaitingRoom(username); // Open the main app window with the entered username
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                primaryStage.close(); // Close the login window after submission
            }
        });

        // Creating a vertical layout to hold the components
        VBox layout = new VBox(10); // Spacing between elements
        layout.getChildren().addAll(usernameField, submitButton); // Adding components to the layout
        layout.setAlignment(Pos.CENTER); // Center aligning components
        layout.setPadding(new Insets(20)); // Setting padding around the layout
        layout.setStyle("-fx-background-color: #F2F1EB"); // Setting background color

        // Creating the scene with the layout, setting width and height
        Scene scene = new Scene(layout, 300, 200);
        primaryStage.setScene(scene); // Setting the scene on the primary stage
        primaryStage.show(); // Display the stage
    }

    // Method to open the main application window
    private void openWaitingRoom(String username) throws IOException {
        // Creating an instance of the Waiting class with the entered username
        WaitingRoom waitingRoom = new WaitingRoom(username);
        waitingRoom.start(new Stage()); // Starting a new stage for the "Waiting" window
    }
}
