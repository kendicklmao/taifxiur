package client;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import shared.models.Auction;
import shared.models.BidTransaction;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AuctionChartController {

    @FXML
    private Label chartTitleLabel;

    @FXML
    private LineChart<String, Number> priceChart;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void populateChart(Auction auction) {
        chartTitleLabel.setText("Price History for: " + auction.getItem().getName());

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Bid Price");

        // Thêm giá khởi điểm vào đồ thị
        LocalDateTime startTime = LocalDateTime.ofInstant(auction.getStartTime(), ZoneId.systemDefault());
        series.getData().add(new XYChart.Data<>(startTime.format(formatter), auction.getStartPrice()));

        // Thêm các giá bid từ lịch sử
        for (BidTransaction bid : auction.getBidHistory()) {
            LocalDateTime bidTime = LocalDateTime.ofInstant(bid.getTime(), ZoneId.systemDefault());
            series.getData().add(new XYChart.Data<>(bidTime.format(formatter), bid.getAmount()));
        }

        priceChart.getData().add(series);
    }
}