package client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import shared.models.Auction;
import shared.network.Request;
import shared.network.Response;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import shared.enums.Category;
import shared.enums.ItemStatus;
import shared.utils.GsonUtils;
import shared.models.Auction;

public class SellerHomeController {

    @FXML private TextField itemNameField;
    @FXML private TextField startPriceField;
    @FXML private ChoiceBox<Category> categoryBox;
    @FXML private VBox dynamicForm;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ListView<Auction> auctionList;
    @FXML private TextArea descField;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = GsonUtils.createGson();

    private final AppContext ctx = AppContext.getInstance();

    @FXML
    public void initialize() {
        try {
             out = ctx.getOut();   // ✅ NO data type before this
            in = ctx.getIn();
        } catch (Exception e) {
            e.printStackTrace();
        }

        categoryBox.getItems().addAll(Category.values());
        categoryBox.setOnAction(e -> updateForm());
        auctionList.setCellFactory(list -> new ListCell<>() {
        @Override
        protected void updateItem(Auction a, boolean empty) {
            super.updateItem(a, empty);

            if (empty || a == null) {
                setText(null);
            } else {
                setText(
                    "📦 " + a.getItem().getName() +
                    "\n💰 Price: " + a.getCurrentPrice() +
                    "\n📊 Status: " + a.getStatus()
                );
            }
        }
    });

        // Fetch seller's auctions on initialization
        fetchSellerAuctions();
    }

    // ================= DYNAMIC FORM =================
    private void updateForm() {
        dynamicForm.getChildren().clear();

        Category c = categoryBox.getValue();

        if (c == Category.ELECTRONICS) {
             TextField brand = new TextField();
             brand.setPromptText("Brand");
             brand.setId("brandField");

             ChoiceBox<ItemStatus> status = new ChoiceBox<>();
             status.getItems().addAll(ItemStatus.values());
             status.setValue(ItemStatus.NEW);
             status.setId("statusField");

             dynamicForm.getChildren().addAll(brand, status);
         }

         else if (c == Category.ARTS) {
            TextField artist = new TextField();
            artist.setPromptText("Artist");
            artist.setId("artistField");

            TextField year = new TextField();
            year.setPromptText("Year");
            year.setId("yearField");

            CheckBox original = new CheckBox("Original");
            original.setId("originalBox");

            dynamicForm.getChildren().addAll(artist, year, original);
        }

        else if (c == Category.VEHICLES) {
            TextField brand = new TextField();
            brand.setPromptText("Brand");
            brand.setId("brandField");

            TextField model = new TextField();
            model.setPromptText("Model year");
            model.setId("modelField");

            TextField km = new TextField();
            km.setPromptText("KM traveled");
            km.setId("kmField");

            dynamicForm.getChildren().addAll(brand, model, km);
        }

         else if (c == Category.FASHIONS) {
             TextField brand = new TextField();
             brand.setPromptText("Brand");
             brand.setId("brandField");

             ChoiceBox<ItemStatus> status = new ChoiceBox<>();
             status.getItems().addAll(ItemStatus.values());
             status.setValue(ItemStatus.NEW);
             status.setId("statusField");

             dynamicForm.getChildren().addAll(brand, status);
         }

        else if (c == Category.COLLECTIBLES) {
            TextField year = new TextField();
            year.setPromptText("Year");
            year.setId("yearField");

            dynamicForm.getChildren().add(year);
        }
    }

    // ================= CREATE AUCTION =================
    public void handleCreateAuction() {
        try {
            String name = itemNameField.getText();
            String price = startPriceField.getText();
            String desc = descField.getText();

            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();

            if (name.isEmpty() || startDate == null || endDate == null) {
                 showAlert("Error", "Missing information!");
                return;
            }

            if (name.length() < 3 || desc.length() < 3) {
                 showAlert("Error", "Name and description must have at least 3 characters!");
                return;
            }

            if (startDate.isAfter(endDate)) {
                 showAlert("Error", "Start date must be before end date!");
                return;
            }

            if (startDate.isBefore(LocalDate.now())) {
                 showAlert("Error", "Start date must be from today onwards!");
                return;
            }

            Map<String, String> data = new HashMap<>();
            data.put("name", name);
            data.put("price", price);
            data.put("startDate", startDate.toString());
            data.put("endDate", endDate.toString());
            data.put("category", categoryBox.getValue().name());
            data.put("username", ctx.getCurrentUser().getUsername());
            data.put("description", desc);

            for (javafx.scene.Node node : dynamicForm.getChildren()) {
                 if (node instanceof TextField tf) {
                     data.put(tf.getId(), tf.getText());
                 } else if (node instanceof CheckBox cb) {
                     data.put(cb.getId(), String.valueOf(cb.isSelected()));
                 } else if (node instanceof ChoiceBox cb) {
                     Object value = cb.getValue();
                     if (value != null) {
                         data.put(cb.getId(), value.toString());
                     }
                 }
             }

            Request req = new Request("CREATE_AUCTION", data);

            Response response = ctx.sendRequestAndWait(req, 10);
            
            System.out.println("MESSAGE = " + response.getMessage()); 
            Auction auction = null;
            if ("SUCCESS".equals(response.getStatus())) {
                auction = gson.fromJson(response.getMessage(), Auction.class);
                auctionList.getItems().add(auction);
                 showAlert("OK", "Auction created successfully!");
                // Clear all fields after successful creation
                itemNameField.clear();
                startPriceField.clear();
                descField.clear();
                startDatePicker.setValue(null);
                endDatePicker.setValue(null);
                categoryBox.setValue(null);
                dynamicForm.getChildren().clear();
            }
            else{
                showAlert("Lỗi", response.getMessage());
            }

        } catch (Exception e) {
            showAlert("Error", "Invalid data!");
            e.printStackTrace();
        }
    }

    // ================= LOGOUT =================
    @FXML
    public void handleLogout() {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("username", ctx.getCurrentUser().getUsername());
            Request req = new Request("LOGOUT", data);
            ctx.sendRequestAndWait(req, 5);
        } catch (Exception e) {
            // Ignore, proceed with logout
        }
        ctx.setCurrentUser(null);
        Navigator.switchScene("login.fxml");
    }

    // ================= ALERT =================
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // ================= FETCH SELLER AUCTIONS =================
    private void fetchSellerAuctions() {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("username", ctx.getCurrentUser().getUsername());
            Request req = new Request("GET_SELLER_AUCTIONS", data);
            Response response = ctx.sendRequestAndWait(req, 10);
            if ("SUCCESS".equals(response.getStatus())) {
                Auction[] auctions = gson.fromJson(response.getMessage(), Auction[].class);
                auctionList.getItems().clear();
                auctionList.getItems().addAll(auctions);
            } else {
                showAlert("Error", "Failed to load auctions: " + response.getMessage());
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to load auctions!");
            e.printStackTrace();
        }
    }
}
