// ClientHandler.java
package com.example.punsapp;

import com.google.gson.Gson;

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

            String messageServer;
            while ((messageServer = in.readLine()) != null) {
                System.out.println("Received message: " + messageServer);

                Gson gson = new Gson();

                Message message = gson.fromJson(messageServer, Message.class);

                if (message.getMessageType() == "COORDINATES") {
                    // Extract x and y coordinates from the message
                    double x = message.getX();
                    double y = message.getY();
                    // Broadcast the coordinates to other clients
                    serverListener.onCoordinatesReceived(x, y);
                } else if (message.getMessageType() == "CLEAR_CANVAS") {
                    // Broadcast the clear canvas command to other clients
                    serverListener.onClearCanvasReceived();}
                else {
                    // For other message types, broadcast as usual
                    serverListener.onChatMessageReceived(message.chat);
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
