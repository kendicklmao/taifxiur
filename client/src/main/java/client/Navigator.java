package client;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Navigator {
    private static Stage stage;
    
    public static void setStage(Stage s){
        stage = s;
    }
    public static void switchScene(String fxml){
        try{
            boolean isMaximized = stage.isMaximized();

            double width = stage.getWidth();
            double height = stage.getHeight();
            double x = stage.getX();
            double y = stage.getY();

            FXMLLoader loader = new FXMLLoader(
                Navigator.class.getClassLoader().getResource(fxml)
            );

            Parent root = loader.load();
            Scene scene = new Scene(root);

            stage.setScene(scene);

            Platform.runLater(() -> {

                if (isMaximized) {
                    stage.setMaximized(true); // 🔳 giữ full màn hình
                } else {
                    stage.setWidth(width);
                    stage.setHeight(height);
                    stage.setX(x);
                    stage.setY(y);
                }

            });

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}