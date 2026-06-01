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
import shared.models.items.Item;
import shared.utils.FormatUtils;

public class AuctionDetailViewBuilder {

    // Tiêu đề, subtitle và nút đóng
    public static HBox createHeaderBox(String titleText, String subtitleText, Runnable onClose) {
        // Cột văn bản chứa Title và Subtitle (giống cấu trúc bảng bên trái)
        VBox textColumn = new VBox(4);
        Label titleLabel = new Label(titleText);
        titleLabel.getStyleClass().add("dashboard-section-title");
        
        Label subtitleLabel = new Label(subtitleText);
        subtitleLabel.getStyleClass().add("dashboard-section-subtitle");
        textColumn.getChildren().addAll(titleLabel, subtitleLabel);
        
        Button closeButton = new Button("x");
        closeButton.getStyleClass().add("dashboard-btn-ghost");
        closeButton.setStyle("-fx-min-width: 32px; -fx-min-height: 32px; -fx-padding: 0;");
        closeButton.setOnAction(e -> onClose.run());
        
        // Dùng Spacer để đẩy nút X về bên phải tuyệt đối
        javafx.scene.layout.Pane spacer = new javafx.scene.layout.Pane();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        // Hàng tổng chứa Cột văn bản và Nút đóng
        HBox headerBox = new HBox(textColumn, spacer, closeButton);
        headerBox.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        return headerBox;
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

        // Cấu hình cột để nhãn bên trái không bị co, nhãn bên phải tự xuống dòng
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setMinWidth(100);
        col1.setPrefWidth(100);
        col1.setHgrow(Priority.NEVER);

        javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        col2.setMinWidth(0);
        
        detailsGrid.getColumnConstraints().addAll(col1, col2);

        int rowIndex = 0;
        addDetailRow(detailsGrid, "Name:", auction.getItem().getName(), rowIndex++);
        addDetailRow(detailsGrid, "Description:", auction.getItem().getDescription(), rowIndex++);
        addDetailRow(detailsGrid, "Base Price:", "$" + auction.getItem().getStartingPrice(), rowIndex++);
        addDetailRow(detailsGrid, "Current Price:", "$" + auction.getCurrentPrice(), rowIndex++);
        addDetailRow(detailsGrid, "Start Time:", FormatUtils.formatTime(auction.getStartTime()), rowIndex++);
        addDetailRow(detailsGrid, "End Time:", FormatUtils.formatTime(auction.getEndTime()), rowIndex++);
        addDetailRow(detailsGrid, "Status:", auction.getStatus().toString(), rowIndex++);
        addDetailRow(detailsGrid, "Seller:", auction.getSeller().getUsername(), rowIndex++);

        Item item = auction.getItem();
        Map<String, String> extraDetails = item.getAdditionalDetails();
        if (extraDetails != null) {
            for (Map.Entry<String, String> entry : extraDetails.entrySet()) {
                addDetailRow(detailsGrid, entry.getKey() + ":", entry.getValue(), rowIndex++);
            }
        }

        return detailsGrid;
    }

    private static void addDetailRow(GridPane grid, String label, String value, int row) {
        Label titleLabel = new Label(label);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #8b8ba7;");
        
        Label valueLabel = new Label(value != null ? value : "N/A");
        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(Double.MAX_VALUE); // Cho phép giãn nở hết cỡ để wrap text
        
        grid.add(titleLabel, 0, row);
        grid.add(valueLabel, 1, row);
    }

    // Tạo giao diện chi tiết cơ bản
    public static void populateBasicDetails(VBox container, Auction auction, Runnable onClose) {
        populateFullDetails(container, auction, onClose);
    }

    public static void populateFullDetails(VBox container, Auction auction, Runnable onClose, Node... actionNodes) {
        container.getChildren().clear();
        container.setVisible(true);
        container.setManaged(true);

        HBox headerBox = createHeaderBox("Auction Details", "View detailed information and status of this auction", () -> {
            container.setVisible(false);
            container.setManaged(false);
            onClose.run();
        });

        ImageView imageView = createImageView(auction, 200, 200);
        GridPane detailsGrid = createDetailsGrid(auction);

        container.getChildren().addAll(headerBox, imageView, detailsGrid);

        if (actionNodes != null && actionNodes.length > 0) {
            HBox actionBox = new HBox(10);
            actionBox.setStyle("-fx-padding: 15 0 0 0;");
            actionBox.getChildren().addAll(actionNodes);
            container.getChildren().add(actionBox);
        }
    }
}
