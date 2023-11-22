package com.example.punsapp;

public interface ServerListener {
    void onChatMessageReceived(String message); // For handling chat messages
    void onCoordinatesReceived(double x, double y); // For handling drawing coordinates
    void onClearCanvasReceived();

}