package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import shared.models.Auction;
import shared.network.Request;
import shared.network.Response;
import shared.utils.GsonUtils;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BidderHomeController {
    @FXML private ListView<Auction> auctionList;
    @FXML private TextField bidAmountField;
    private final AppContext ctx = AppContext.getInstance();
    private final Gson gson = GsonUtils.createGson();
    private Consumer<String> messageListener;

    @FXML
    public void initialize() {
        auctionList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Auction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getItem().getName() + " - Current: " + item.getCurrentPrice() + " - " + item.getStatus());
                }
            }
        });

        messageListener = line -> {
            try {
                Response res = gson.fromJson(line, Response.class);
                if ("SUCCESS".equals(res.getStatus())) {
                    // Could be the result of GET_AUCTIONS or PLACE_BID
                    // But we don't know which one.
                    // Let's check if it looks like a list of auctions
                    try {
                        List<Auction> list = gson.fromJson(res.getMessage(), new TypeToken<List<Auction>>(){}.getType());
                        if (list != null) {
                            Platform.runLater(() -> {
                                auctionList.getItems().setAll(list);
                            });
                        }
                    } catch (Exception e) {
                        // Not a list, maybe a simple success message
                    }
                } else if ("UPDATE_PRICE".equals(res.getStatus())) {
                    Platform.runLater(this::refreshAuctions);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        ctx.addMessageListener(messageListener);

        refreshAuctions();
    }

    private void refreshAuctions() {
        Request req = new Request("GET_AUCTIONS", new HashMap<>());
        ctx.getOut().println(gson.toJson(req));
    }

    public void handleBid() {
        Auction selected = auctionList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Lỗi", "Vui lòng chọn một phiên đấu giá!");
            return;
        }

        String amountStr = bidAmountField.getText();
        if (amountStr == null || amountStr.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập số tiền!");
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("auctionId", selected.getId());
        data.put("amount", amountStr);
        data.put("username", ctx.getCurrentUser().getUsername());

        Request req = new Request("PLACE_BID", data);
        ctx.getOut().println(gson.toJson(req));
    }

    public void handleLogout() {
        ctx.removeMessageListener(messageListener);
        Navigator.switchScene("login.fxml");
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}