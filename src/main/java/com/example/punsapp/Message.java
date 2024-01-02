package com.example.punsapp;

// Defining a class named Message
public class Message {

    // Declaring instance variables
    int roomId; // Represents the room ID
    String username; // Represents the username
    String messageType; // Represents the type of message (e.g., text, image)
    String chat; // Represents the message content
    double x; // Represents x-coordinate (if applicable)
    double y; // Represents y-coordinate (if applicable)

    private String color; // Private variable to store color information

    // Constructor to initialize Message objects
    public Message() {
        // Setting default values for instance variables
        this.roomId = -1; // Default room ID set to -1
        this.username = ""; // Default username set to an empty string
        this.messageType = ""; // Default message type set to an empty string
        this.chat = ""; // Default chat/message content set to an empty string
        this.x = 0; // Default x-coordinate set to 0
        this.y = 0; // Default y-coordinate set to 0
    }

    // Getter method for retrieving the username
    public String getUsername() {
        return username;
    }

    // Setter method for setting the username
    public void setUsername(String username) {
        this.username = username;
    }

    // Getter method for retrieving the message type
    public String getMessageType() {
        return messageType;
    }

    // Setter method for setting the message type
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    // Getter method for retrieving the chat/message content
    public String getChat() {
        return chat;
    }

    // Setter method for setting the chat/message content
    public void setChat(String chat) {
        this.chat = chat;
    }

    // Getter method for retrieving the x-coordinate
    public double getX() {
        return x;
    }

    // Setter method for setting the x-coordinate
    public void setX(double x) {
        this.x = x;
    }

    // Getter method for retrieving the y-coordinate
    public double getY() {
        return y;
    }

    // Setter method for setting the y-coordinate
    public void setY(double y) {
        this.y = y;
    }

    // Getter method for retrieving the color
    public String getColor() {
        return color;
    }

    // Setter method for setting the color
    public void setColor(String color) {
        this.color = color;
    }

    // Getter method for retrieving the room ID
    public int getRoomId() {
        return roomId;
    }

    // Setter method for setting the room ID
    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }
}
