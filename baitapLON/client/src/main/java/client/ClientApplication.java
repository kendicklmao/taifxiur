package client;

import com.google.gson.Gson;
import shared.network.Request;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientApplication {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to server");
            Gson gson = new Gson();

            Map<String, String> bidData = new HashMap<>();
            bidData.put("auctionId", "IPHONE_15");
            bidData.put("amount", "100000000000000000");
            Request req = new Request("PLACE_BID", bidData);

            String jsonRequest = gson.toJson(req);
            System.out.println("Sending: " + jsonRequest);
            out.println(jsonRequest);

            String serverResponse;
            while ((serverResponse = in.readLine()) != null) {
                System.out.println("Server update: " + serverResponse);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}