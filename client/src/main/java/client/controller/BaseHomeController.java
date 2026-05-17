package client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import shared.network.Request;
import shared.network.Response;
import shared.utils.FormatUtils;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import client.Navigator;
import client.support.ChangePasswordSupport;
import shared.models.Auction;

import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import client.support.AuctionCardBuilder;
import java.util.List;

public abstract class BaseHomeController extends UserController {

    @FXML
    protected Label welcomeLabel;

    @FXML
    protected VBox auctionDetailPane;

    protected Auction selectedAuction;

    protected Consumer<String> messageListener;

    // Các thuộc tính chung cho Grid
    protected final Map<String, VBox> auctionCardMap = new HashMap<>();

    // Mặc định trả về null, các lớp con dùng Grid sẽ override lại
    protected TilePane getAuctionGrid() {
        return null;
    }

    // Mặc định không làm gì, các lớp con dùng Grid sẽ override lại
    protected void onAuctionCardDoubleClicked(Auction auction) {
    }

    protected void setupHome() {
        if (welcomeLabel != null && ctx.getCurrentUser() != null) {
            welcomeLabel.setText("Welcome " + ctx.getCurrentUser().getUsername());
        }

        messageListener = line -> {
            try {
                Response res = gson.fromJson(line, Response.class);
                onSocketMessage(res);
            } catch (Exception e) {
            }
        };
        ctx.addMessageListener(messageListener);
    }

    protected void updateAuctionGrid(List<Auction> auctions) {
        TilePane grid = getAuctionGrid();
        if (grid == null)
            return;

        Map<String, VBox> existingCards = new HashMap<>();
        for (var node : grid.getChildren()) {
            if (node instanceof VBox card && card.getUserData() != null) {
                existingCards.put(card.getUserData().toString(), card);
            }
        }

        for (Auction auction: auctions) {
            String id = auction.getId();
            if (existingCards.containsKey(id)) {
                VBox card = existingCards.get(id);
                AuctionCardBuilder.updateCardData(card, auction);
                existingCards.remove(id);
            } else {
                VBox card = AuctionCardBuilder.createAuctionCard(auction, this::onAuctionCardDoubleClicked);
                grid.getChildren().add(card);
                auctionCardMap.put(id, card);
            }
        }

        // Xóa các card không còn tồn tại
        grid.getChildren().removeAll(existingCards.values());
        existingCards.keySet().forEach(auctionCardMap::remove);
    }

    @FXML
    public void handleLogout() {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("username", ctx.getCurrentUser().getUsername());
            Request req = new Request("LOGOUT", data);
            ctx.sendRequestAndWait(req, 30);
        } catch (Exception e) {
        }

        if (messageListener != null) {
            ctx.removeMessageListener(messageListener);
        }
        ctx.setCurrentUser(null);
        Navigator.switchSceneStatic("login.fxml");
    }

    @FXML
    public void handleChangePassword() {
        ChangePasswordSupport.showDialog(ctx, welcomeLabel);
    }

    protected String formatTime(Instant instant) {
        return FormatUtils.formatTime(instant);
    }

    protected void onSocketMessage(Response response) {
    }

    protected abstract void refreshData();

    protected void handleTerminateAuction(Auction auction) {
        boolean confirmed = alertService.showConfirmation(
            "Are you sure", 
            "This action cannot be undone.", 
            welcomeLabel.getScene().getWindow()
        );

        if (confirmed) {
            terminateAuctionOnServer(auction);
        }
    }

    private void terminateAuctionOnServer(Auction auction) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("auctionId", auction.getId());
            data.put("username", ctx.getCurrentUser().getUsername());
            Request req = new Request("TERMINATE_AUCTION", data);
            Response response = ctx.sendRequestAndWait(req, 20);

            if ("SUCCESS".equals(response.getStatus())) {
                alertService.showAlert("Success", "Auction terminated successfully.", welcomeLabel);
                refreshData();
                if (auctionDetailPane != null) {
                    auctionDetailPane.setVisible(false);
                    auctionDetailPane.setManaged(false);
                }
                this.selectedAuction = null;
            } else {
                alertService.showAlert("Error", "Failed to terminate auction: " + response.getMessage(), welcomeLabel);
            }

        } catch (Exception e) {
            alertService.showAlert("Error", "An error occurred while terminating the auction.", welcomeLabel);
        }
    }
}
