package client.support;

import java.util.Map;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import shared.models.*;
import shared.utils.FormatUtils;

public class AuctionDetailViewBuilder {

    // Tiêu đề và nút đóng
    public static HBox createTitleBox(String titleText, Runnable onClose) {
        Label titleLabel = new Label(titleText);
        titleLabel.getStyleClass().add("dashboard-section-title");
        
        Button closeButton = new Button("X");
        closeButton.getStyleClass().add("dashboard-btn-ghost");
        closeButton.setOnAction(e -> onClose.run());
        
        HBox titleBox = new HBox(10, titleLabel, closeButton);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        return titleBox;
    }

    // Hình ảnh
    public static ImageView createImageView(Auction auction, double width, double height) {
        ImageView imageView = new ImageView();
        String imageUrl = auction.getItem().getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            imageView.setImage(new Image(imageUrl, width, height, true, true));
        }
        return imageView;
    }

    // Thông tin chi tiết
    public static GridPane createDetailsGrid(Auction auction) {
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(10);
        detailsGrid.setVgap(8);
        detailsGrid.getStyleClass().add("details-grid");

        int rowIndex = 0;
        detailsGrid.add(new Label("Name:"), 0, rowIndex);
        detailsGrid.add(new Label(auction.getItem().getName()), 1, rowIndex++);
        detailsGrid.add(new Label("Description:"), 0, rowIndex);
        Label descLabel = new Label(auction.getItem().getDescription());
        descLabel.setWrapText(true);
        detailsGrid.add(descLabel, 1, rowIndex++);
        detailsGrid.add(new Label("Base Price:"), 0, rowIndex);
        detailsGrid.add(new Label("$" + auction.getItem().getStartingPrice()), 1, rowIndex++);
        detailsGrid.add(new Label("Current Price:"), 0, rowIndex);
        detailsGrid.add(new Label("$" + auction.getCurrentPrice()), 1, rowIndex++);
        detailsGrid.add(new Label("Start Time:"), 0, rowIndex);
        detailsGrid.add(new Label(FormatUtils.formatTime(auction.getStartTime())), 1, rowIndex++);
        detailsGrid.add(new Label("End Time:"), 0, rowIndex);
        detailsGrid.add(new Label(FormatUtils.formatTime(auction.getEndTime())), 1, rowIndex++);
        detailsGrid.add(new Label("Status:"), 0, rowIndex);
        detailsGrid.add(new Label(auction.getStatus().toString()), 1, rowIndex++);
        detailsGrid.add(new Label("Seller:"), 0, rowIndex);
        detailsGrid.add(new Label(auction.getSeller().getUsername()), 1, rowIndex++);

        Item item = auction.getItem();
        Map<String, String> extraDetails = item.getAdditionalDetails();
        if (extraDetails != null) {
            for (Map.Entry<String, String> entry : extraDetails.entrySet()) {
                detailsGrid.add(new Label(entry.getKey() + ":"), 0, rowIndex);
                detailsGrid.add(new Label(entry.getValue()), 1, rowIndex++);
            }
        }

        return detailsGrid;
    }

    // Tạo giao diện chi tiết cơ bản
    public static void populateBasicDetails(VBox container, Auction auction, Runnable onClose) {
        populateFullDetails(container, auction, onClose);
    }

    public static void populateFullDetails(VBox container, Auction auction, Runnable onClose, Node... actionNodes) {
        container.getChildren().clear();
        container.setVisible(true);
        container.setManaged(true);

        HBox titleBox = createTitleBox("Auction Details", () -> {
            container.setVisible(false);
            container.setManaged(false);
            onClose.run();
        });

        ImageView imageView = createImageView(auction, 200, 200);
        GridPane detailsGrid = createDetailsGrid(auction);

        container.getChildren().addAll(titleBox, imageView, detailsGrid);

        if (actionNodes != null && actionNodes.length > 0) {
            HBox actionBox = new HBox(10);
            actionBox.setStyle("-fx-padding: 15 0 0 0;");
            actionBox.getChildren().addAll(actionNodes);
            container.getChildren().add(actionBox);
        }
    }
}
