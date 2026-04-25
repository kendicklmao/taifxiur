package client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.stage.FileChooser;
import org.imgscalr.Scalr;
import shared.models.Auction;
import shared.network.Request;
import shared.network.Response;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.application.Platform;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
    @FXML private TilePane auctionGrid;
    @FXML private TextArea descField;
    @FXML private Label welcomeLabel;
    @FXML private ImageView itemImageView;
    @FXML private Label walletBalanceLabel;
    private File selectedImageFile;
    private byte[] croppedImageBytes;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = GsonUtils.createGson();

    private final AppContext ctx = AppContext.getInstance();
    private static final List<String> BANK_NAMES = Arrays.asList(
            "Vietcombank", "Techcombank", "BIDV", "Agribank", "VPBank",
            "MBBank", "ACB", "Sacombank", "Eximbank", "HDBank",
            "TPBank", "VIB", "SeABank", "SHB", "OCB",
            "MSB", "LienVietPostBank", "BacABank", "VietBank", "PVcomBank"
    );

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

        // Fetch seller's auctions on initialization
        fetchSellerAuctions();
        refreshWalletBalance();

        // Schedule periodic refresh
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    fetchSellerAuctions();
                    refreshWalletBalance();
                });
            }
        }, 0, 5000); // Refresh every 5 seconds
    }

    private void updateAuctionGrid(List<Auction> auctions) {
        Map<String, VBox> existingAuctionCards = auctionGrid.getChildren().stream()
                .map(node -> (VBox) node)
                .collect(Collectors.toMap(node -> (String) node.getUserData(), node -> node));

        for (Auction auction : auctions) {
            if (existingAuctionCards.containsKey(auction.getId())) {
                // Update existing card
                VBox card = existingAuctionCards.get(auction.getId());
                // You can update specific labels here if needed, e.g., price
                existingAuctionCards.remove(auction.getId());
            } else {
                // Add new card
                VBox card = createAuctionCard(auction);
                card.setUserData(auction.getId());
                auctionGrid.getChildren().add(card);
            }
        }

        // Remove old cards
        auctionGrid.getChildren().removeAll(existingAuctionCards.values());
    }

    private VBox createAuctionCard(Auction auction) {
        ImageView imageView = new ImageView();
        Label nameLabel = new Label();
        Label priceLabel = new Label();
        Label statusLabel = new Label();
        VBox card = new VBox(10);

        imageView.setFitHeight(150);
        imageView.setFitWidth(150);
        card.getStyleClass().add("auction-card");
        nameLabel.getStyleClass().add("item-name");
        priceLabel.getStyleClass().add("item-price");
        statusLabel.getStyleClass().add("item-status");
        VBox itemDetails = new VBox(5, nameLabel, priceLabel, statusLabel);
        card.getChildren().addAll(imageView, itemDetails);

        if (auction.getItem().getImageUrl() != null && !auction.getItem().getImageUrl().isEmpty()) {
            imageView.setImage(new Image(auction.getItem().getImageUrl(), 150, 150, true, true));
        }
        nameLabel.setText(auction.getItem().getName());
        priceLabel.setText("Current Price: " + auction.getCurrentPrice());
        statusLabel.setText("Status: " + auction.getStatus());

        return card;
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Item Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
        selectedImageFile = fileChooser.showOpenDialog(itemNameField.getScene().getWindow());
        if (selectedImageFile != null) {
            try {
                BufferedImage originalImage = ImageIO.read(selectedImageFile);
                int size = Math.min(originalImage.getWidth(), originalImage.getHeight());
                BufferedImage cropped = Scalr.crop(originalImage, (originalImage.getWidth() - size) / 2, (originalImage.getHeight() - size) / 2, size, size);
                BufferedImage resized = Scalr.resize(cropped, 200);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(resized, getFileExtension(selectedImageFile.getName()), baos);
                croppedImageBytes = baos.toByteArray();

                Image image = new Image(selectedImageFile.toURI().toString());
                itemImageView.setImage(image);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to process image.");
            }
        }
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

            if (selectedImageFile == null) {
                showAlert("Error", "Please upload an image for the item.");
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

                Instant minStartTime = Instant.now().plusSeconds(60);
                if (startTime.isBefore(minStartTime)) {
                    showAlert("Error", "Start time must be at least 1 minute from now!");
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

            if (croppedImageBytes != null) {
                String encodedImage = Base64.getEncoder().encodeToString(croppedImageBytes);
                data.put("image", encodedImage);
                data.put("imageContentType", "image/" + getFileExtension(selectedImageFile.getName()));
            }

            for (javafx.scene.Node node : dynamicForm.getChildren()) {
                 if (node instanceof VBox) {
                     for (javafx.scene.Node innerNode : ((VBox) node).getChildren()) {
                         if (innerNode instanceof TextField tf) {
                             data.put(tf.getId(), tf.getText());
                         } else if (innerNode instanceof CheckBox cb) {
                             data.put(cb.getId(), String.valueOf(cb.isSelected()));
                         } else if (innerNode instanceof ChoiceBox cb) {
                             Object value = cb.getValue();
                             if (value != null) {
                                 data.put(cb.getId(), value.toString());
                             }
                         }
                     }
                 } else if (node instanceof TextField tf) {
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
            if ("SUCCESS".equals(response.getStatus())) {
                fetchSellerAuctions(); // Refresh the grid
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
                itemImageView.setImage(null);
                selectedImageFile = null;
                croppedImageBytes = null;
            }
            else{
                showAlert("Lỗi", response.getMessage());
            }

        } catch (Exception e) {
            showAlert("Error", "Invalid data!");
            e.printStackTrace();
        }
    }

    private String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return fileName.substring(lastIndexOf + 1);
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
                List<Auction> auctions = gson.fromJson(response.getMessage(), new TypeToken<List<Auction>>(){}.getType());
                Platform.runLater(() -> updateAuctionGrid(auctions));
            }
        } catch (Exception e) {
            System.out.println("Failed to refresh auctions: " + e.getMessage());
        }
    }

    private void refreshWalletBalance() {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("username", ctx.getCurrentUser().getUsername());
            Response response = ctx.sendRequestAndWait(new Request("GET_WALLET_BALANCE", data), 5);
            if ("SUCCESS".equals(response.getStatus())) {
                Platform.runLater(() -> walletBalanceLabel.setText("Balance: $" + response.getMessage()));
            } else {
                Platform.runLater(() -> walletBalanceLabel.setText("Balance: unavailable"));
            }
        } catch (Exception e) {
            Platform.runLater(() -> walletBalanceLabel.setText("Balance: unavailable"));
        }
    }

    @FXML
    public void handleWithdrawRequest() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Withdraw Request");
        dialog.setHeaderText("Send a withdraw request to admin");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");

        ComboBox<String> bankNameComboBox = new ComboBox<>();
        bankNameComboBox.setPromptText("Select bank name");
        bankNameComboBox.setEditable(true);
        ObservableList<String> bankOptions = FXCollections.observableArrayList(BANK_NAMES);
        bankNameComboBox.setItems(bankOptions);

        bankNameComboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isEmpty()) {
                bankNameComboBox.setItems(bankOptions);
            } else {
                List<String> filteredList = BANK_NAMES.stream()
                        .filter(s -> s.toLowerCase().contains(newText.toLowerCase()))
                        .collect(Collectors.toList());
                bankNameComboBox.setItems(FXCollections.observableArrayList(filteredList));
            }
        });

        TextField accountNumberField = new TextField();
        accountNumberField.setPromptText("Enter account number");

        VBox content = new VBox(10);
        content.getChildren().addAll(new Label("Amount"), amountField, new Label("Bank Name"), bankNameComboBox, new Label("Account Number"), accountNumberField);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String selectedBank = bankNameComboBox.getSelectionModel().getSelectedItem();
                if (selectedBank == null && !bankNameComboBox.getEditor().getText().isEmpty()) {
                    selectedBank = bankNameComboBox.getEditor().getText();
                }
                return amountField.getText() + "," + selectedBank + "," + accountNumberField.getText();
            }
            return null;
        });
        dialog.showAndWait().ifPresent(result -> {
            String[] parts = result.split(",");
            if (parts.length == 3) {
                try {
                    BigDecimal amount = new BigDecimal(parts[0]);
                    String bankName = parts[1];
                    String accountNumber = parts[2];
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        showAlert("Error", "Amount must be greater than 0");
                        return;
                    }
                    if (bankName.trim().isEmpty() || accountNumber.trim().isEmpty()) {
                        showAlert("Error", "Bank name and account number cannot be empty");
                        return;
                    }

                    Map<String, String> data = new HashMap<>();
                    data.put("username", ctx.getCurrentUser().getUsername());
                    data.put("amount", amount.toPlainString());
                    data.put("bankName", bankName.trim());
                    data.put("accountNumber", accountNumber.trim());
                    Response response = ctx.sendRequestAndWait(new Request("CREATE_WITHDRAW_REQUEST", data), 5);
                    if ("SUCCESS".equals(response.getStatus())) {
                        showAlert("Success", response.getMessage());
                    } else {
                        showAlert("Error", response.getMessage());
                    }
                } catch (Exception e) {
                    showAlert("Error", "Please enter valid data");
                }
            }
        });
    }
}

