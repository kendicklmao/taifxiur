package client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
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

public class SellerHomeController {

    @FXML
    private TextField itemNameField;
    @FXML
    private TextField startPriceField;
    @FXML
    private ChoiceBox<Category> categoryBox;
    @FXML
    private VBox dynamicForm;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private Spinner<Integer> startHourSpinner;
    @FXML
    private Spinner<Integer> startMinuteSpinner;
    @FXML
    private Spinner<Integer> endHourSpinner;
    @FXML
    private Spinner<Integer> endMinuteSpinner;
    @FXML
    private VBox customTimingPane;
    @FXML
    private ChoiceBox<String> incrementTypeBox;
    @FXML
    private VBox customIncrementPane;
    @FXML
    private TextField customIncrementField;
    @FXML
    private TilePane auctionGrid;
    @FXML
    private TextArea descField;
    @FXML
    private Label welcomeLabel;
    @FXML
    private ImageView itemImageView;
    @FXML
    private Label walletBalanceLabel;
    private File selectedImageFile;
    private byte[] croppedImageBytes;
    private final Gson gson = GsonUtils.createGson();

    private final AppContext ctx = AppContext.getInstance();
    private final IAlertService alertService = new AlertServiceImpl();
    private static final List<String> BANK_NAMES = Arrays.asList(
            "Vietcombank", "Techcombank", "BIDV", "Agribank", "VPBank",
            "MBBank", "ACB", "Sacombank", "Eximbank", "HDBank",
            "TPBank", "VIB", "SeABank", "SHB", "OCB",
            "MSB", "LienVietPostBank", "BacABank", "VietBank", "PVcomBank");

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome " + ctx.getCurrentUser().getUsername());
        categoryBox.getItems().addAll(Category.values());
        categoryBox.setOnAction(e -> updateForm());

        // Set default timing values
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.plusMinutes(5);
        LocalDateTime endTime = startTime.plusMinutes(30);

        startDatePicker.setValue(startTime.toLocalDate());
        startHourSpinner
                .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, startTime.getHour()));
        startMinuteSpinner
                .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, startTime.getMinute()));

        endDatePicker.setValue(endTime.toLocalDate());
        endHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, endTime.getHour()));
        endMinuteSpinner
                .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, endTime.getMinute()));

        // Initialize increment type box
        incrementTypeBox.getItems().addAll("Default (5%)", "Custom Amount");
        incrementTypeBox.setValue("Default (5%)");
        incrementTypeBox.setOnAction(e -> {
            boolean isCustom = "Custom Amount".equals(incrementTypeBox.getValue());
            customIncrementPane.setVisible(isCustom);
            customIncrementPane.setManaged(isCustom);
        });

        // Fetch seller's auctions on initialization
        fetchSellerAuctions();
        refreshWalletBalance();
    }

    @FXML
    public void handleRefresh() {
        fetchSellerAuctions();
        refreshWalletBalance();
    }

    private void updateAuctionGrid(List<Auction> auctions) {
        // Build a safe map of existing cards keyed by their auction id (userData)
        Map<String, VBox> existingAuctionCards = new java.util.HashMap<>();
        for (var node : auctionGrid.getChildren()) {
            if (node instanceof VBox) {
                Object ud = node.getUserData();
                if (ud != null) {
                    existingAuctionCards.put(ud.toString(), (VBox) node);
                }
            }
        }

        // Iterate incoming auctions and update existing cards or add new ones
        for (Auction auction : auctions) {
            String id = auction.getId();
            if (existingAuctionCards.containsKey(id)) {
                // Update existing card with latest data
                VBox card = existingAuctionCards.get(id);
                Object priceObj = card.getProperties().get("priceLabel");
                if (priceObj instanceof Label) {
                    ((Label) priceObj).setText("Current Price: " + auction.getCurrentPrice());
                }
                Object statusObj = card.getProperties().get("statusLabel");
                if (statusObj instanceof Label) {
                    ((Label) statusObj).setText("Status: " + auction.getStatus());
                }
                Object endsObj = card.getProperties().get("endsLabel");
                if (endsObj instanceof Label) {
                    ((Label) endsObj).setText("Ends: " + formatTime(auction.getEndTime()));
                }
                existingAuctionCards.remove(id);
            } else {
                // Add new card
                VBox card = createAuctionCard(auction);
                card.setUserData(id);
                auctionGrid.getChildren().add(card);
            }
        }

        // Remove cards for auctions that no longer exist
        auctionGrid.getChildren().removeAll(existingAuctionCards.values());
    }

    private VBox createAuctionCard(Auction auction) {
        ImageView imageView = new ImageView();
        Label nameLabel = new Label();
        Label priceLabel = new Label();
        Label statusLabel = new Label();
        Label startsAtLabel = new Label();
        Label endsAtLabel = new Label();
        VBox card = new VBox(10);

        imageView.setFitHeight(150);
        imageView.setFitWidth(150);
        card.getStyleClass().add("auction-card");
        nameLabel.getStyleClass().add("item-name");
        priceLabel.getStyleClass().add("item-price");
        statusLabel.getStyleClass().add("item-status");
        startsAtLabel.getStyleClass().add("item-ends-in");
        endsAtLabel.getStyleClass().add("item-ends-in");
        VBox itemDetails = new VBox(5, nameLabel, priceLabel, statusLabel, startsAtLabel, endsAtLabel);
        card.getChildren().addAll(imageView, itemDetails);

        // Store labels in properties for easy updating
        card.getProperties().put("priceLabel", priceLabel);
        card.getProperties().put("statusLabel", statusLabel);
        card.getProperties().put("endsLabel", endsAtLabel);

        if (auction.getItem().getImageUrl() != null && !auction.getItem().getImageUrl().isEmpty()) {
            imageView.setImage(new Image(auction.getItem().getImageUrl(), 150, 150, true, true));
        }
        nameLabel.setText(auction.getItem().getName());
        priceLabel.setText("Current Price: " + auction.getCurrentPrice());
        statusLabel.setText("Status: " + auction.getStatus());
        startsAtLabel.setText("Starts: " + formatTime(auction.getStartTime()));
        endsAtLabel.setText("Ends: " + formatTime(auction.getEndTime()));

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
                BufferedImage cropped = Scalr.crop(originalImage, (originalImage.getWidth() - size) / 2,
                        (originalImage.getHeight() - size) / 2, size, size);
                BufferedImage resized = Scalr.resize(cropped, 200);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(resized, getFileExtension(selectedImageFile.getName()), baos);
                croppedImageBytes = baos.toByteArray();

                Image image = new Image(selectedImageFile.toURI().toString());
                itemImageView.setImage(image);
            } catch (Exception e) {
                e.printStackTrace();
                alertService.showAlert("Error", "Failed to process image.", welcomeLabel);
            }
        }
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
            BigDecimal startingPrice;

            if (name.isEmpty()) {
                alertService.showAlert("Error", "Missing information!", welcomeLabel);
                return;
            }

            try {
                startingPrice = new BigDecimal(price);
            } catch (NumberFormatException e) {
                alertService.showAlert("Error", "Starting price must be a valid number!", welcomeLabel);
                return;
            }

            if (startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                alertService.showAlert("Error", "Starting price must be greater than 0!", welcomeLabel);
                return;
            }

            if (selectedImageFile == null) {
                alertService.showAlert("Error", "Please upload an image for the item.", welcomeLabel);
                return;
            }

            if (name.length() < 3 || desc.length() < 3) {
                alertService.showAlert("Error", "Name and description must have at least 3 characters!", welcomeLabel);
                return;
            }

            Instant startTime;
            Instant endTime;

            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();

            if (startDate == null || endDate == null) {
                alertService.showAlert("Error", "Missing date information!", welcomeLabel);
                return;
            }

            int startHour = startHourSpinner.getValue();
            int startMinute = startMinuteSpinner.getValue();
            int endHour = endHourSpinner.getValue();
            int endMinute = endMinuteSpinner.getValue();

            startTime = startDate.atTime(startHour, startMinute).atZone(ZoneId.systemDefault()).toInstant();
            endTime = endDate.atTime(endHour, endMinute).atZone(ZoneId.systemDefault()).toInstant();

            if (startTime.isAfter(endTime)) {
                alertService.showAlert("Error", "Start time must be before end time!", welcomeLabel);
                return;
            }

            Instant minStartTime = Instant.now().plusSeconds(60);
            if (startTime.isBefore(minStartTime)) {
                alertService.showAlert("Error", "Start time must be at least 1 minute from now!", welcomeLabel);
                return;
            }

            Map<String, String> data = new HashMap<>();
            data.put("name", name);
            data.put("price", price);
            data.put("startTime", startTime.toString());
            data.put("endTime", endTime.toString());
            data.put("category", categoryBox.getValue().name());
            data.put("username", ctx.getCurrentUser().getUsername());
            data.put("description", desc);
            data.put("incrementType", incrementTypeBox.getValue());
            if ("Custom Amount".equals(incrementTypeBox.getValue())) {
                String customInc = customIncrementField.getText();
                if (customInc == null || customInc.isEmpty()) {
                    alertService.showAlert("Error", "Please enter custom increment amount!", welcomeLabel);
                    return;
                }
                try {
                    BigDecimal inc = new BigDecimal(customInc);
                    if (inc.compareTo(BigDecimal.ZERO) <= 0) {
                        alertService.showAlert("Error", "Minimum increment must be greater than 0!", welcomeLabel);
                        return;
                    }
                    BigDecimal defaultMinIncrement = startingPrice.multiply(new BigDecimal("0.05"));
                    if (inc.compareTo(defaultMinIncrement) < 0) {
                        alertService.showAlert("Error", "Custom increment must be greater than or equal to default minimum increment: " + defaultMinIncrement.toPlainString(), welcomeLabel);
                        return;
                    }
                    data.put("minIncrement", customInc);
                } catch (NumberFormatException e) {
                    alertService.showAlert("Error", "Custom increment must be a valid number!", welcomeLabel);
                    return;
                }
            }

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

            Response response = ctx.sendRequestAndWait(req, 15);

            System.out.println("MESSAGE = " + response.getMessage());
            if ("SUCCESS".equals(response.getStatus())) {
                fetchSellerAuctions(); // Refresh the grid
                alertService.showAlert("OK", "Auction created successfully!", welcomeLabel);
                itemNameField.clear();
                startPriceField.clear();
                descField.clear();
                startDatePicker.setValue(null);
                endDatePicker.setValue(null);

                // Set default timing values for next auction
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime nextStartTime = now.plusMinutes(5);
                LocalDateTime nextEndTime = nextStartTime.plusMinutes(5);

                startDatePicker.setValue(nextStartTime.toLocalDate());
                startHourSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, nextStartTime.getHour()));
                startMinuteSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, nextStartTime.getMinute()));

                endDatePicker.setValue(nextEndTime.toLocalDate());
                endHourSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, nextEndTime.getHour()));
                endMinuteSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, nextEndTime.getMinute()));

                categoryBox.setValue(null);
                dynamicForm.getChildren().clear();
                itemImageView.setImage(null);
                selectedImageFile = null;
                croppedImageBytes = null;
            } else {
                alertService.showAlert("Lỗi", response.getMessage(), welcomeLabel);
            }

        } catch (Exception e) {
            alertService.showAlert("Error", "Invalid data!", welcomeLabel);
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
        ChangePasswordSupport.showDialog(ctx, welcomeLabel);
    }

    @FXML
    public void handleLogout() {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("username", ctx.getCurrentUser().getUsername());
            Request req = new Request("LOGOUT", data);
            ctx.sendRequestAndWait(req, 15);
        } catch (Exception e) {
            // Ignore, proceed with logout
        }
        ctx.setCurrentUser(null);
        Navigator.switchSceneStatic("login.fxml");
    }

    // ================= ALERT =================
    private void showAlert(String title, String message) {
        alertService.showAlert(title, message, welcomeLabel);
    }

    private String formatTime(java.time.Instant instant) {
        if (instant == null)
            return "Unknown";
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return dateTime.format(formatter);
    }

    // ================= FETCH SELLER AUCTIONS =================
    private void fetchSellerAuctions() {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("username", ctx.getCurrentUser().getUsername());
            Request req = new Request("GET_SELLER_AUCTIONS", data);
            Response response = ctx.sendRequestAndWait(req, 15);
            if ("SUCCESS".equals(response.getStatus())) {
                List<Auction> auctions = gson.fromJson(response.getMessage(), new TypeToken<List<Auction>>() {
                }.getType());
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
            Response response = ctx.sendRequestAndWait(new Request("GET_WALLET_BALANCE", data), 15);
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
        content.getChildren().addAll(new Label("Amount"), amountField, new Label("Bank Name"), bankNameComboBox,
                new Label("Account Number"), accountNumberField);
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
                        alertService.showAlert("Error", "Amount must be greater than 0", welcomeLabel);
                        return;
                    }
                    if (bankName.trim().isEmpty() || accountNumber.trim().isEmpty()) {
                        alertService.showAlert("Error", "Bank name and account number cannot be empty", welcomeLabel);
                        return;
                    }

                    Map<String, String> data = new HashMap<>();
                    data.put("username", ctx.getCurrentUser().getUsername());
                    data.put("amount", amount.toPlainString());
                    data.put("bankName", bankName.trim());
                    data.put("accountNumber", accountNumber.trim());
                    Response response = ctx.sendRequestAndWait(new Request("CREATE_WITHDRAW_REQUEST", data), 15);
                    if ("SUCCESS".equals(response.getStatus())) {
                        alertService.showAlert("Success", response.getMessage(), welcomeLabel);
                    } else {
                        alertService.showAlert("Error", response.getMessage(), welcomeLabel);
                    }
                } catch (Exception e) {
                    alertService.showAlert("Error", "Please enter valid data", welcomeLabel);
                }
            }
        });
    }
}
