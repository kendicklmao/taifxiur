import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ItemManagerApp extends Application {

    // Danh sách lưu trữ dữ liệu hiển thị trên bảng
    private ObservableList<Item> itemList = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Quản lý sản phẩm đấu giá");

        // --- 1. PHẦN BẢNG HIỂN THỊ (TableView) ---
        TableView<Item> table = new TableView<>();

        TableColumn<Item, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Item, String> colName = new TableColumn<>("Tên");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, Double> colBasePrice = new TableColumn<>("Giá gốc");
        colBasePrice.setCellValueFactory(new PropertyValueFactory<>("basePrice"));

        TableColumn<Item, Double> colCurrentPrice = new TableColumn<>("Giá hiện tại");
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));

        TableColumn<Item, String> colSeller = new TableColumn<>("Người bán");
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));

        TableColumn<Item, Boolean> colLegit = new TableColumn<>("Legit");
        colLegit.setCellValueFactory(new PropertyValueFactory<>("legitCheck"));

        table.getColumns().addAll(colId, colName, colBasePrice, colCurrentPrice, colSeller, colLegit);
        table.setItems(itemList);

        // --- 2. PHẦN FORM NHẬP LIỆU ---
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameInput = new TextField(); nameInput.setPromptText("Tên sản phẩm");
        TextField descInput = new TextField(); descInput.setPromptText("Mô tả");
        TextField basePriceInput = new TextField(); basePriceInput.setPromptText("Giá gốc");
        TextField sellerInput = new TextField(); sellerInput.setPromptText("Người bán");
        CheckBox legitInput = new CheckBox("Đã kiểm định (Legit)");

        // ComboBox cho Category (Giả định Category là một Enum hoặc class)
        ComboBox<Category> categoryInput = new ComboBox<>(FXCollections.observableArrayList(Category.values()));
        categoryInput.setPromptText("Chọn danh mục");

        Button addButton = new Button("Thêm sản phẩm");
        addButton.setOnAction(e -> {
            try {
                Item newItem = new Item(
                        nameInput.getText(),
                        descInput.getText(),
                        Double.parseDouble(basePriceInput.getText()),
                        Double.parseDouble(basePriceInput.getText()), // currentPrice ban đầu = basePrice
                        sellerInput.getText(),
                        legitInput.isSelected(),
                        categoryInput.getValue()
                );
                itemList.add(newItem);

                // Clear form sau khi thêm
                nameInput.clear();
                descInput.clear();
                basePriceInput.clear();
                sellerInput.clear();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Vui lòng nhập đúng định dạng số!");
                alert.show();
            }
        });

        // Sắp xếp các control vào Grid
        grid.add(new Label("Tên:"), 0, 0); grid.add(nameInput, 1, 0);
        grid.add(new Label("Giá:"), 0, 1); grid.add(basePriceInput, 1, 1);
        grid.add(new Label("Người bán:"), 0, 2); grid.add(sellerInput, 1, 2);
        grid.add(new Label("Danh mục:"), 2, 0); grid.add(categoryInput, 3, 0);
        grid.add(legitInput, 2, 1);
        grid.add(addButton, 3, 2);

        // --- 3. BỐ CỤC CHÍNH (Layout) ---
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));
        mainLayout.getChildren().addAll(new Label("DANH SÁCH SẢN PHẨM"), table, grid);

        Scene scene = new Scene(mainLayout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}