package client.support;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import shared.models.Auction;
import java.util.function.Consumer;
import shared.utils.FormatUtils;

public class AuctionCardBuilder {

    public static VBox createAuctionCard(Auction auction, Consumer<Auction> onDoubleClicked) {
        ImageView imageView = new ImageView();
        Label nameLabel = new Label();
        Label priceLabel = new Label();
        Label statusLabel = new Label();
        Label startsAtLabel = new Label();
        Label endsAtLabel = new Label();
        
        VBox card = new VBox(10);
        card.getStyleClass().add("auction-card");
        
        imageView.setFitHeight(150);
        imageView.setFitWidth(150);
        
        nameLabel.getStyleClass().add("item-name");
        priceLabel.getStyleClass().add("item-price");
        statusLabel.getStyleClass().add("item-status");
        startsAtLabel.getStyleClass().add("item-ends-in");
        endsAtLabel.getStyleClass().add("item-ends-in");

        VBox itemDetails = new VBox(5, nameLabel, priceLabel, statusLabel, startsAtLabel, endsAtLabel);
        card.getChildren().addAll(imageView, itemDetails);

        card.getProperties().put("nameLabel", nameLabel);
        card.getProperties().put("priceLabel", priceLabel);
        card.getProperties().put("statusLabel", statusLabel);
        card.getProperties().put("startsLabel", startsAtLabel);
        card.getProperties().put("endsLabel", endsAtLabel);

        updateCardData(card, auction);

        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && onDoubleClicked != null) {
                onDoubleClicked.accept(auction);
            }
        });

        card.setUserData(auction.getId());
        return card;
    }

    public static void updateCardData(VBox card, Auction auction) {
        Label nameLabel = (Label) card.getProperties().get("nameLabel");
        Label priceLabel = (Label) card.getProperties().get("priceLabel");
        Label statusLabel = (Label) card.getProperties().get("statusLabel");
        Label startsLabel = (Label) card.getProperties().get("startsLabel");
        Label endsLabel = (Label) card.getProperties().get("endsLabel");
        
        ImageView imageView = (ImageView) card.getChildren().get(0);

        if (nameLabel != null) nameLabel.setText(auction.getItem().getName());
        if (priceLabel != null) priceLabel.setText("Current Price: $" + auction.getCurrentPrice());
        if (statusLabel != null) statusLabel.setText("Status: " + auction.getStatus());
        if (startsLabel != null) startsLabel.setText("Starts: " + FormatUtils.formatTime(auction.getStartTime()));
        if (endsLabel != null) endsLabel.setText("Ends: " + FormatUtils.formatTime(auction.getEndTime()));

        if (imageView.getImage() == null && auction.getItem().getImageUrl() != null && !auction.getItem().getImageUrl().isEmpty()) {
            imageView.setImage(new Image(auction.getItem().getImageUrl(), 150, 150, true, true, true));
        }
    }
}
