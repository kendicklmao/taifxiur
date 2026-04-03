package com.auction.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            ServerMain.clientList.add(writer);
            for (PrintWriter client: ServerMain.clientList) {
                client.println("Mot client moi da ket noi voi server.");
            }
            String messageFromClient;
            while ((messageFromClient = reader.readLine()) != null) {
                System.out.println("Client gui: " + messageFromClient);
                if (messageFromClient.equalsIgnoreCase("dmm")) {
                    writer.println("Dit con me may!");
                } else if (messageFromClient.equalsIgnoreCase("hello")) {
                    writer.println("Lo cai con CAC!");
                } else {
                    writer.println("Server da nhan: " + messageFromClient);
                }
            }
        } catch (IOException e) {
            System.err.println("Loi ket noi voi 1 client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                for (PrintWriter client: ServerMain.clientList) {
                    client.println("Mot client da ngat ket noi.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
