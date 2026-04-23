package client;

import javafx.stage.Stage;

public interface INavigator {
    void setStage(Stage stage);
    void switchScene(String fxml);
}

