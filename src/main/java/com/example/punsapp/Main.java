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

import static javafx.application.Application.launch;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Charades Game - Login");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");

        Button submitButton = new Button("Submit");
        submitButton.setOnAction(e -> {
            String username = usernameField.getText();
            if (!username.isEmpty()) {
                try {
                    openMainApp(username);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                primaryStage.close(); // Close the login window after submission
            }
        });

        VBox layout = new VBox(10);
        layout.getChildren().addAll(usernameField, submitButton);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));

        Scene scene = new Scene(layout, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void openMainApp(String username) throws IOException {
        MainWindow mainApp = new MainWindow(username);
        try {
            mainApp.start(new Stage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
