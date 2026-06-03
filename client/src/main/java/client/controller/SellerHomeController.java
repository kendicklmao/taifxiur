package client.controller;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.imgscalr.Scalr;
import shared.models.Auction;
import shared.network.Request;
import shared.network.Response;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import com.google.gson.reflect.TypeToken;

import client.support.AuctionDetailViewBuilder;
import client.support.ChangePasswordSupport;
import client.support.TransactionDialogSupport;
import shared.enums.Category;
import shared.enums.ItemStatus;
import shared.enums.FieldDefinition;

public class SellerHomeController extends BaseHomeController {

    @Override
    protected void refreshData() {
        fetchAllAuctions();
    }

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
    private Spinner<Integer> startSecondSpinner;
    @FXML
    private Spinner<Integer> endHourSpinner;
    @FXML
    private Spinner<Integer> endMinuteSpinner;
    @FXML
    private Spinner<Integer> endSecondSpinner;
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
    private ImageView itemImageView;
    @FXML
    private Label walletBalanceLabel;
    private File selectedImageFile;
    private byte[] croppedImageBytes;

    @FXML
    public void initialize() {

        setupHome();

        categoryBox.getItems().addAll(Category.values());
        categoryBox.setOnAction(e -> updateForm());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.plusMinutes(5);
        LocalDateTime endTime = startTime.plusMinutes(30);

        startDatePicker.setValue(startTime.toLocalDate());
        startHourSpinner
                .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, startTime.getHour()));
        startMinuteSpinner
                .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, startTime.getMinute()));
        startSecondSpinner
                .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, startTime.getSecond()));

        endDatePicker.setValue(endTime.toLocalDate());
        endHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, endTime.getHour()));
        endMinuteSpinner
                .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, endTime.getMinute()));
        endSecondSpinner
                .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, endTime.getSecond()));

        incrementTypeBox.getItems().addAll("Default (5%)", "Custom Amount");
        incrementTypeBox.setValue("Default (5%)");
        incrementTypeBox.setOnAction(e -> {
            boolean isCustom = "Custom Amount".equals(incrementTypeBox.getValue());
            customIncrementPane.setVisible(isCustom);
            customIncrementPane.setManaged(isCustom);
        });

        walletBalanceLabel.setText("Loading...");

        PauseTransition delay = new PauseTransition(Duration.millis(300));
        delay.setOnFinished(e -> {
            refreshWalletBalance();
            fetchAllAuctions();
        });
        delay.play();

        javafx.animation.Timeline autoRefresh = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> {
                    handleRefresh();
                }));
        autoRefresh.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        autoRefresh.play();
    }

    @Override
    protected void onSocketMessage(Response res) {
        if ("UPDATE_PRICE".equals(res.getStatus()) || "AUCTION_UPDATED".equals(res.getStatus())
                || "AUCTION_FINISHED".equals(res.getStatus()) || "AUCTION_CREATED".equals(res.getStatus())) {
            fetchAllAuctions();
        }
    }

    @FXML
    public void handleRefresh() {
        fetchAllAuctions();
        refreshWalletBalance();
    }

    @Override
    protected TilePane getAuctionGrid() {
        return auctionGrid;
    }

    @Override
    protected void onAuctionCardDoubleClicked(Auction auction) {
        showAuctionDetails(auction);
    }

    private void showAuctionDetails(Auction auction) {
        this.selectedAuction = auction;

        Button terminateButton = new Button("Terminate Auction");
        terminateButton.getStyleClass().add("dashboard-btn-logout");
        terminateButton.setOnAction(e -> handleTerminateAuction(auction));

        boolean canTerminate = auction.getSeller().getUsername().equals(ctx.getCurrentUser().getUsername());
        terminateButton.setVisible(canTerminate);
        terminateButton.setManaged(canTerminate);

        AuctionDetailViewBuilder.populateFullDetails(auctionDetailPane, auction, () -> {
            this.selectedAuction = null;
        }, terminateButton);
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
                alertService.showAlert("Error", "Failed to process image.", welcomeLabel);
            }
        }
    }

    private void updateForm() {
        dynamicForm.getChildren().clear();
        Category c = categoryBox.getValue();

        if (c == null) {
            return;
        }

        for (FieldDefinition field : c.getFields()) {
            switch (field.getType()) {
                case TEXT -> addTextField(field.getLabel(), field.getId());
                case STATUS_CHOICE_BOX -> addStatusChoiceBox(field.getId());
                case CHECKBOX -> addCheckBox(field.getLabel(), field.getId());
            }
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

    // Xử lý tạo auction
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
            int startSecond = startSecondSpinner.getValue();
            int endHour = endHourSpinner.getValue();
            int endMinute = endMinuteSpinner.getValue();
            int endSecond = endSecondSpinner.getValue();

            startTime = startDate.atTime(startHour, startMinute, startSecond).atZone(ZoneId.systemDefault())
                    .toInstant();
            endTime = endDate.atTime(endHour, endMinute, endSecond).atZone(ZoneId.systemDefault()).toInstant();

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
                        alertService.showAlert("Error",
                                "Custom increment must be greater than or equal to default minimum increment: "
                                        + defaultMinIncrement.toPlainString(),
                                welcomeLabel);
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

            Response response = ctx.sendRequestAndWait(req, 30);

            System.out.println("MESSAGE = " + response.getMessage());
            if ("SUCCESS".equals(response.getStatus())) {
                fetchAllAuctions();
                alertService.showAlert("Success", "Auction created successfully!", welcomeLabel);
                itemNameField.clear();
                startPriceField.clear();
                descField.clear();
                startDatePicker.setValue(null);
                endDatePicker.setValue(null);

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime nextStartTime = now.plusMinutes(5);
                LocalDateTime nextEndTime = nextStartTime.plusMinutes(5);

                startDatePicker.setValue(nextStartTime.toLocalDate());
                startHourSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, nextStartTime.getHour()));
                startMinuteSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, nextStartTime.getMinute()));
                startSecondSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, nextStartTime.getSecond()));

                endDatePicker.setValue(nextEndTime.toLocalDate());
                endHourSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, nextEndTime.getHour()));
                endMinuteSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, nextEndTime.getMinute()));
                endSecondSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, nextEndTime.getSecond()));

                categoryBox.setValue(null);
                dynamicForm.getChildren().clear();
                itemImageView.setImage(null);
                selectedImageFile = null;
                croppedImageBytes = null;
            } else {
                alertService.showAlert("Error", response.getMessage(), welcomeLabel);
            }

        } catch (Exception e) {
            alertService.showAlert("Error", "Invalid data!", welcomeLabel);
        }
    }

    private String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }

        return fileName.substring(lastIndexOf + 1);
    }

    @FXML
    public void handleChangePassword() {
        ChangePasswordSupport.showDialog(ctx, welcomeLabel);
    }

    private void fetchAllAuctions() {
        if (ctx.getCurrentUser() == null) {
            return;
        }
        Task<List<Auction>> task = new Task<>() {
            @Override
            protected List<Auction> call() throws Exception {
                Map<String, String> data = new HashMap<>();
                data.put("username", ctx.getCurrentUser().getUsername());
                Response response = ctx.sendRequestAndWait(new Request("GET_SELLER_AUCTIONS", data), 30);
                Type type = new TypeToken<List<Auction>>() {
                }.getType();
                return gson.fromJson(response.getMessage(), type);
            }
        };

        task.setOnSucceeded(e -> {
            List<Auction> list = task.getValue();
            if (list != null) {
                updateAuctionGrid(list);
                if (selectedAuction != null) {
                    Auction updated = list.stream().filter(a -> a.getId().equals(selectedAuction.getId())).findFirst()
                            .orElse(null);
                    if (updated != null) {
                        showAuctionDetails(updated);
                    }
                }
            }
        });

        task.setOnFailed(e -> {
            alertService.showAlert("Error", "Cannot load auctions", auctionGrid);
        });

        new Thread(task).start();
    }

    private void refreshWalletBalance() {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                Map<String, String> data = new HashMap<>();
                data.put("username", ctx.getCurrentUser().getUsername());
                Response response = ctx.sendRequestAndWait(new Request("GET_WALLET_BALANCE", data), 30);
                if ("SUCCESS".equals(response.getStatus())) {
                    return response.getMessage();
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            String balance = task.getValue();
            if (balance != null) {
                walletBalanceLabel.setText("Balance: $" + balance);
            } else {
                walletBalanceLabel.setText("Balance unavailable");
            }
        });

        task.setOnFailed(e -> {
            walletBalanceLabel.setText("Balance unavailable");
        });

        new Thread(task).start();
    }

    @FXML
    public void handleWithdrawRequest() {
        TransactionDialogSupport.showDialog(TransactionDialogSupport.Type.WITHDRAW, ctx, alertService, welcomeLabel);
    }
}