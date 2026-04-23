package client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.control.ToggleGroup;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;

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
    @FXML private Spinner<Integer> startHourSpinner;
    @FXML private Spinner<Integer> startMinuteSpinner;
    @FXML private Spinner<Integer> endHourSpinner;
    @FXML private Spinner<Integer> endMinuteSpinner;
    @FXML private RadioButton defaultTimingRadio;
    @FXML private RadioButton customTimingRadio;
    @FXML private VBox customTimingPane;
    @FXML private ListView<Auction> auctionList;
    @FXML private TextArea descField;
    @FXML private Label welcomeLabel;
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

        welcomeLabel.setText("Welcome " + ctx.getCurrentUser().getUsername());
        categoryBox.getItems().addAll(Category.values());
        categoryBox.setOnAction(e -> updateForm());

        ToggleGroup timingGroup = new ToggleGroup();
        defaultTimingRadio.setToggleGroup(timingGroup);
        customTimingRadio.setToggleGroup(timingGroup);
        defaultTimingRadio.setSelected(true);

        // Initialize time spinners
        startHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9));
        startMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        endHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 17));
        endMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

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

        // Schedule periodic refresh
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(SellerHomeController.this::fetchSellerAuctions);
            }
        }, 0, 5000); // Refresh every 5 seconds
    }

    @FXML
    private void handleTimingTypeChange() {
        boolean isCustom = customTimingRadio.isSelected();
        customTimingPane.setVisible(isCustom);
        customTimingPane.setManaged(isCustom);
    }

    // ================= DYNAMIC FORM =================
    private void updateForm() {
        dynamicForm.getChildren().clear();

        Category c = categoryBox.getValue();

        if (c == null) {
            return;
        }

        switch (c) {
            case ELECTRONICS:
                addTextField("Brand", "brandField");
                addStatusChoiceBox("statusField");
                break;
            case ARTS:
                addTextField("Artist", "artistField");
                addTextField("Year", "yearField");
                addCheckBox("Original", "originalBox");
                break;
            case VEHICLES:
                addTextField("Brand", "brandField");
                addTextField("Model Year", "modelField");
                addTextField("KM Traveled", "kmField");
                break;
            case FASHIONS:
                addTextField("Brand", "brandField");
                addStatusChoiceBox("statusField");
                break;
            case COLLECTIBLES:
                addTextField("Year", "yearField");
                break;
        }
    }

    private void addTextField(String prompt, String id) {
        VBox container = new VBox(6);
        Label label = new Label(prompt);
        label.getStyleClass().add("form-label-register");
        TextField field = new TextField();
        field.setPromptText("Enter " + prompt.toLowerCase());
        field.setId(id);
        field.getStyleClass().add("dashboard-input");
        container.getChildren().addAll(label, field);
        dynamicForm.getChildren().add(container);
    }

    private void addStatusChoiceBox(String id) {
        VBox container = new VBox(6);
        Label label = new Label("Status");
        label.getStyleClass().add("form-label-register");
        ChoiceBox<ItemStatus> status = new ChoiceBox<>();
        status.getItems().addAll(ItemStatus.values());
        status.setValue(ItemStatus.NEW);
        status.setId(id);
        status.getStyleClass().add("dashboard-choicebox");
        container.getChildren().addAll(label, status);
        dynamicForm.getChildren().add(container);
    }

    private void addCheckBox(String text, String id) {
        CheckBox checkbox = new CheckBox(text);
        checkbox.setId(id);
        checkbox.getStyleClass().add("dashboard-checkbox");
        dynamicForm.getChildren().add(checkbox);
    }

    // ================= CREATE AUCTION =================
    public void handleCreateAuction() {
        try {
            String name = itemNameField.getText();
            String price = startPriceField.getText();
            String desc = descField.getText();

            if (name.isEmpty()) {
                showAlert("Error", "Missing information!");
                return;
            }

            if (name.length() < 3 || desc.length() < 3) {
                showAlert("Error", "Name and description must have at least 3 characters!");
                return;
            }

            Instant startTime;
            Instant endTime;

            if (defaultTimingRadio.isSelected()) {
                startTime = Instant.now();
                endTime = startTime.plusSeconds(3600);
            } else {
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();

                if (startDate == null || endDate == null) {
                    showAlert("Error", "Missing date information!");
                    return;
                }

                int startHour = startHourSpinner.getValue();
                int startMinute = startMinuteSpinner.getValue();
                int endHour = endHourSpinner.getValue();
                int endMinute = endMinuteSpinner.getValue();

                startTime = startDate.atTime(startHour, startMinute).atZone(ZoneId.systemDefault()).toInstant();
                endTime = endDate.atTime(endHour, endMinute).atZone(ZoneId.systemDefault()).toInstant();

                if (startTime.isAfter(endTime)) {
                    showAlert("Error", "Start time must be before end time!");
                    return;
                }

                Instant minStartTime = Instant.now().plusSeconds(300);
                if (startTime.isBefore(minStartTime)) {
                    showAlert("Error", "Start time must be at least 5 minutes from now!");
                    return;
                }
            }

            Map<String, String> data = new HashMap<>();
            data.put("name", name);
            data.put("price", price);
            data.put("startTime", startTime.toString());
            data.put("endTime", endTime.toString());
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
                itemNameField.clear();
                startPriceField.clear();
                descField.clear();
                startDatePicker.setValue(null);
                endDatePicker.setValue(null);
                startHourSpinner.getValueFactory().setValue(9);
                startMinuteSpinner.getValueFactory().setValue(0);
                endHourSpinner.getValueFactory().setValue(17);
                endMinuteSpinner.getValueFactory().setValue(0);
                categoryBox.setValue(null);
                dynamicForm.getChildren().clear();
                defaultTimingRadio.setSelected(true);
                handleTimingTypeChange();
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
    public void handleChangePassword() {
        ChangePasswordSupport.showDialog(ctx);
    }

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
            Response response = ctx.sendRequestAndWait(req, 5);
            if ("SUCCESS".equals(response.getStatus())) {
                Auction[] auctions = gson.fromJson(response.getMessage(), Auction[].class);
                auctionList.getItems().clear();
                auctionList.getItems().addAll(auctions);
            }
        } catch (Exception e) {
            System.out.println("Failed to refresh auctions: " + e.getMessage());
        }
    }
}
