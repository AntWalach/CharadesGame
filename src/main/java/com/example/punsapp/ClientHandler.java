// ClientHandler.java
package com.example.punsapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private PrintWriter out;
    private ServerListener serverListener;

    public ClientHandler(Socket clientSocket, ServerListener serverListener) {
        this.clientSocket = clientSocket;
        this.serverListener = serverListener;
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received message: " + message);

                if (message.startsWith("COORDINATES")) {
                    // Extract x and y coordinates from the message
                    String[] parts = message.split(" ");
                    if (parts.length >= 3) {
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);

                        // Broadcast the coordinates to other clients
                        serverListener.onCoordinatesReceived(x, y);
                    }
                } else if (message.equals("CLEAR_CANVAS")) {
                    // Broadcast the clear canvas command to other clients
                    serverListener.onClearCanvasReceived();}
                else {
                    // For other message types, broadcast as usual
                    serverListener.onChatMessageReceived(message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
