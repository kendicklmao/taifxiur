package client;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Auction House");
        stage.setMinWidth(900);
        stage.setMinHeight(700);

        Navigator.setStage(stage);
        Navigator.switchScene("login.fxml");

        stage.show();
    }
    public static void main(String[] args) { 
        launch(args); 
    }
}
