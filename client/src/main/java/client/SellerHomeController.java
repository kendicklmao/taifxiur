package client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.stage.FileChooser;
import org.imgscalr.Scalr;
import shared.models.Auction;
import shared.network.Request;
import shared.network.Response;
import shared.models.Item;
import shared.models.Electronic;
import shared.models.Vehicle;
import shared.models.Art;
import shared.models.Fashion;
import shared.models.Collectible;

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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.application.Platform;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import shared.enums.Category;
import shared.enums.ItemStatus;
import shared.utils.GsonUtils;
import shared.enums.BankList;

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
    @FXML
    private VBox auctionDetailPane;
    private File selectedImageFile;
    private byte[] croppedImageBytes;
    private final Gson gson = GsonUtils.createGson();

    private final AppContext ctx = AppContext.getInstance();
    private final IAlertService alertService = new AlertServiceImpl();
    private Consumer<String> messageListener;

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

        // Fetch all auctions on initialization
        fetchAllAuctions();
        refreshWalletBalance();

        messageListener = line -> {
            try {
                Response res = gson.fromJson(line, Response.class);
                if ("UPDATE_PRICE".equals(res.getStatus()) || "AUCTION_UPDATED".equals(res.getStatus())
                        || "AUCTION_FINISHED".equals(res.getStatus()) || "AUCTION_CREATED".equals(res.getStatus())) {
                    Platform.runLater(this::fetchAllAuctions);
                }
            } catch (Exception e) {
                // Ignore malformed broadcast
            }
        };
        ctx.addMessageListener(messageListener);
    }

    @FXML
    public void handleRefresh() {
        fetchAllAuctions();
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
                    ((Label) priceObj).setText("Current Price: $" + auction.getCurrentPrice());
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
        priceLabel.setText("Current Price: $" + auction.getCurrentPrice());
        statusLabel.setText("Status: " + auction.getStatus());
        startsAtLabel.setText("Starts: " + formatTime(auction.getStartTime()));
        endsAtLabel.setText("Ends: " + formatTime(auction.getEndTime()));

        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                showAuctionDetails(auction);
            }
        });

        return card;
    }

    private void showAuctionDetails(Auction auction) {
        auctionDetailPane.getChildren().clear();
        auctionDetailPane.setVisible(true);
        auctionDetailPane.setManaged(true);

        // Title and close button
        Label titleLabel = new Label("Auction Details");
        titleLabel.getStyleClass().add("dashboard-section-title");
        Button closeButton = new Button("X");
        closeButton.getStyleClass().add("dashboard-btn-ghost");
        closeButton.setOnAction(e -> {
            auctionDetailPane.setVisible(false);
            auctionDetailPane.setManaged(false);
        });
        HBox titleBox = new HBox(10, titleLabel, closeButton);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        // Image
        ImageView imageView = new ImageView();
        if (auction.getItem().getImageUrl() != null && !auction.getItem().getImageUrl().isEmpty()) {
            imageView.setImage(new Image(auction.getItem().getImageUrl(), 200, 200, true, true));
        }

        // Details grid
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
        detailsGrid.add(new Label(formatTime(auction.getStartTime())), 1, rowIndex++);
        detailsGrid.add(new Label("End Time:"), 0, rowIndex);
        detailsGrid.add(new Label(formatTime(auction.getEndTime())), 1, rowIndex++);
        detailsGrid.add(new Label("Status:"), 0, rowIndex);
        detailsGrid.add(new Label(auction.getStatus().toString()), 1, rowIndex++);
        detailsGrid.add(new Label("Seller:"), 0, rowIndex);
        detailsGrid.add(new Label(auction.getSeller().getUsername()), 1, rowIndex++);

        Item item = auction.getItem();
        if (item instanceof Electronic) {
            Electronic electronic = (Electronic) item;
            detailsGrid.add(new Label("Brand:"), 0, rowIndex);
            detailsGrid.add(new Label(electronic.getBrand()), 1, rowIndex++);
            detailsGrid.add(new Label("Item Status:"), 0, rowIndex);
            detailsGrid.add(new Label(electronic.getStatus().toString()), 1, rowIndex++);
        } else if (item instanceof Vehicle) {
            Vehicle vehicle = (Vehicle) item;
            detailsGrid.add(new Label("Brand:"), 0, rowIndex);
            detailsGrid.add(new Label(vehicle.getBrand()), 1, rowIndex++);
            detailsGrid.add(new Label("Model Year:"), 0, rowIndex);
            detailsGrid.add(new Label(String.valueOf(vehicle.getModel())), 1, rowIndex++);
            detailsGrid.add(new Label("KM Traveled:"), 0, rowIndex);
            detailsGrid.add(new Label(String.valueOf(vehicle.getKMTravel())), 1, rowIndex++);
        } else if (item instanceof Art) {
            Art art = (Art) item;
            detailsGrid.add(new Label("Artist:"), 0, rowIndex);
            detailsGrid.add(new Label(art.getArtist()), 1, rowIndex++);
            detailsGrid.add(new Label("Year Created:"), 0, rowIndex);
            detailsGrid.add(new Label(String.valueOf(art.getYearCreated())), 1, rowIndex++);
            detailsGrid.add(new Label("Original:"), 0, rowIndex);
            detailsGrid.add(new Label(art.getIsOriginal() ? "Yes" : "No"), 1, rowIndex++);
        } else if (item instanceof Fashion) {
            Fashion fashion = (Fashion) item;
            detailsGrid.add(new Label("Brand:"), 0, rowIndex);
            detailsGrid.add(new Label(fashion.getBrand()), 1, rowIndex++);
            detailsGrid.add(new Label("Item Status:"), 0, rowIndex);
            detailsGrid.add(new Label(fashion.getStatus().toString()), 1, rowIndex++);
        } else if (item instanceof Collectible) {
            Collectible collectible = (Collectible) item;
            detailsGrid.add(new Label("Year Created:"), 0, rowIndex);
            detailsGrid.add(new Label(String.valueOf(collectible.getYearCreated())), 1, rowIndex++);
        }

        Button terminateButton = new Button("Terminate Auction");
        terminateButton.getStyleClass().add("dashboard-btn-logout");
        terminateButton.setOnAction(e -> handleTerminateAuction(auction));

        // Only show terminate button if user owns the auction or is admin
        boolean canTerminate = auction.getSeller().getUsername().equals(ctx.getCurrentUser().getUsername());
        terminateButton.setVisible(canTerminate);
        terminateButton.setManaged(canTerminate);

        auctionDetailPane.getChildren().addAll(titleBox, imageView, detailsGrid, terminateButton);
    }

    private void handleTerminateAuction(Auction auction) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Termination");
        alert.setHeaderText("Are you sure you want to terminate this auction?");
        alert.setContentText("This action cannot be undone.");

        shared.utils.DialogHelper.applyCustomStyle(alert);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            terminateAuctionOnServer(auction);
        }
    }

    private void terminateAuctionOnServer(Auction auction) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("auctionId", auction.getId());
            data.put("username", ctx.getCurrentUser().getUsername());
            Request req = new Request("TERMINATE_AUCTION", data);
            Response response = ctx.sendRequestAndWait(req, 15);

            if ("SUCCESS".equals(response.getStatus())) {
                alertService.showAlert("Success", "Auction terminated successfully.", welcomeLabel);
                fetchAllAuctions();
                auctionDetailPane.setVisible(false);
                auctionDetailPane.setManaged(false);
            } else {
                alertService.showAlert("Error", "Failed to terminate auction: " + response.getMessage(), welcomeLabel);
            }
        } catch (Exception e) {
            alertService.showAlert("Error", "An error occurred while terminating the auction.", welcomeLabel);
        }
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
                fetchAllAuctions(); // Refresh the grid
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
        ctx.removeMessageListener(messageListener);
        ctx.setCurrentUser(null);
        Navigator.switchSceneStatic("login.fxml");
    }


    private String formatTime(java.time.Instant instant) {
        return shared.utils.FormatUtils.formatTime(instant);
    }

    // ================= FETCH ALL AUCTIONS =================
    private void fetchAllAuctions() {
        try {
            Request req = new Request("GET_AUCTIONS", new HashMap<>());
            Response response = ctx.sendRequestAndWait(req, 15);
            if ("SUCCESS".equals(response.getStatus())) {
                List<Auction> auctions = gson.fromJson(response.getMessage(), new TypeToken<List<Auction>>() {
                }.getType());
                List<Auction> activeAuctions = auctions.stream()
                        .filter(a -> a.getStatus() != shared.enums.AuctionStatus.CANCELED)
                        .collect(Collectors.toList());
                Platform.runLater(() -> updateAuctionGrid(activeAuctions));
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
        amountField.getStyleClass().add("dashboard-input");

        ComboBox<BankList> bankNameComboBox = new ComboBox<>();
        bankNameComboBox.setPromptText("Select bank name");
        bankNameComboBox.setEditable(true);
        bankNameComboBox.getStyleClass().add("dashboard-choicebox");
        ObservableList<BankList> bankOptions = FXCollections.observableArrayList(BankList.values());
        bankNameComboBox.setItems(bankOptions);

        bankNameComboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isEmpty()) {
                bankNameComboBox.setItems(bankOptions);
            } else {
                List<BankList> filteredList = Arrays.stream(BankList.values())
                        .filter(s -> s.name().toLowerCase().contains(newText.toLowerCase()))
                        .collect(Collectors.toList());
                bankNameComboBox.setItems(FXCollections.observableArrayList(filteredList));
            }
        });

        TextField accountNumberField = new TextField();
        accountNumberField.setPromptText("Enter account number");
        accountNumberField.getStyleClass().add("dashboard-input");

        VBox content = new VBox(15);
        content.getChildren().addAll(new Label("Amount"), amountField, new Label("Bank Name"), bankNameComboBox,
                new Label("Account Number"), accountNumberField);
        content.setStyle("-fx-padding: 20px;");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Apply custom dialog styling
        shared.utils.DialogHelper.applyCustomStyle(dialog);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Object value = bankNameComboBox.getValue();
                String bankName = "";
                if (value instanceof BankList) {
                    bankName = ((BankList) value).name();
                } else if (value != null) {
                    bankName = value.toString();
                }
                return amountField.getText() + "," + bankName + "," + accountNumberField.getText();
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
