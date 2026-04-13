package client;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import shared.network.Request;
import shared.network.Response;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class AuctionListController {
    @FXML
    ListView<String> auctionList;
    private final AppContext ctx = AppContext.getInstance();

    @FXML
    public void initialize() {
        fetchAuctionsFromServer();
    }

    private void fetchAuctionsFromServer() {
        try {
            Socket socket = new Socket("localhost", 8080);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Request req = new Request("GET_AUCTIONS", null);
            Gson gson = new Gson();
            out.println(gson.toJson(req));

            String serverResponse = in.readLine();
            Response res = gson.fromJson(serverResponse, Response.class);

            if ("SUCCESS".equals(res.getStatus())) {
                Platform.runLater(() -> {
                    auctionList.getItems().clear();
                    if (res.getMessage().equals("[]")) {
                        auctionList.getItems().add("Chưa có phiên đấu giá nào đang chạy!");
                        auctionList.getItems().add("(Hãy đăng nhập bằng tài khoản SELLER để tạo phiên)");
                    } else {
                        auctionList.getItems().add(res.getMessage());
                    }
                });
            }
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void viewDetail() {

    }
}