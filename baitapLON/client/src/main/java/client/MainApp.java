package client;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        Navigator.setStage(stage);
        Navigator.switchScene("login.fxml");
        stage.setTitle("Auction System");
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();
    }
    public static void main(String[] args) { 
        launch(args); 
    }
}
