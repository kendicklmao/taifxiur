package client.support;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import shared.enums.BankList;
import shared.network.Request;
import shared.network.Response;
import shared.utils.DialogHelper;
import client.AppContext;
import client.service.IAlertService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TransactionDialogSupport {

    public enum Type {
        DEPOSIT,
        WITHDRAW
    }

    private TransactionDialogSupport() {
    }

    public static void showDialog(Type type, AppContext ctx, IAlertService alertService, Node ownerNode) {
        Dialog<String> dialog = new Dialog<>();
        if (ownerNode != null && ownerNode.getScene() != null) {
            dialog.initOwner(ownerNode.getScene().getWindow());
        }

        String title = type == Type.DEPOSIT ? "Deposit Request" : "Withdraw Request";
        String headerText = type == Type.DEPOSIT ? "Send a deposit request to admin" : "Send a withdraw request to admin";

        dialog.setTitle(title);
        dialog.setHeaderText(headerText);

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");
        amountField.getStyleClass().add("dashboard-input");

        ComboBox<BankList> bankNameComboBox = new ComboBox<>();
        bankNameComboBox.setPromptText("Select bank name");
        bankNameComboBox.setEditable(true);
        bankNameComboBox.getStyleClass().add("dashboard-choicebox");
        bankNameComboBox.setMaxWidth(Double.MAX_VALUE);
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
                bankNameComboBox.show();
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

        DialogHelper.applyCustomStyle(dialog);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Object value = bankNameComboBox.getValue();
                String selectedBank = "";
                if (value instanceof BankList) {
                    selectedBank = ((BankList) value).name();
                } else if (value != null) {
                    selectedBank = value.toString();
                }
                return amountField.getText() + "," + selectedBank + "," + accountNumberField.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result == null || result.isEmpty())
                return;

            String[] parts = result.split(",", 3);
            if (parts.length < 3 || parts[0].isEmpty() || parts[1] == null || parts[1].trim().isEmpty()
                    || parts[2].isEmpty()) {
                alertService.showAlert("Error", "Please fill in all fields.", ownerNode);
                return;
            }

            try {
                BigDecimal amount = new BigDecimal(parts[0]);
                String bankName = parts[1].trim();
                String accountNumber = parts[2].trim();

                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    alertService.showAlert("Error", "Amount must be greater than 0.", ownerNode);
                    return;
                }

                Map<String, String> data = new HashMap<>();
                data.put("username", ctx.getCurrentUser().getUsername());
                data.put("amount", amount.toPlainString());
                data.put("bankName", bankName);
                data.put("accountNumber", accountNumber);

                String requestAction = type == Type.DEPOSIT ? "CREATE_DEPOSIT_REQUEST" : "CREATE_WITHDRAW_REQUEST";
                int timeout = type == Type.DEPOSIT ? 15 : 20;
                Response response = ctx.sendRequestAndWait(new Request(requestAction, data), timeout);

                if ("SUCCESS".equals(response.getStatus())) {
                    alertService.showAlert("Success", response.getMessage(), ownerNode);
                } else {
                    alertService.showAlert("Error", response.getMessage(), ownerNode);
                }

            } catch (NumberFormatException e) {
                alertService.showAlert("Error", "Please enter a valid amount.", ownerNode);
            } catch (Exception e) {
                alertService.showAlert("Error", "An unexpected error occurred: " + e.getMessage(), ownerNode);
                e.printStackTrace();
            }
        });
    }
}
