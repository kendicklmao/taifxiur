package client;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Navigator {
    private static Stage stage;
    private static String globalStylesheet;

    public static void setStage(Stage s){
        stage = s;
        // Load global stylesheet once
        try {
            globalStylesheet = Navigator.class.getClassLoader().getResource("styles.css").toExternalForm();
            System.out.println("✓ CSS loaded: " + globalStylesheet);
        } catch (Exception e) {
            System.err.println("✗ ERROR: Could not find styles.css!");
            System.err.println("  Message: " + e.getMessage());
            e.printStackTrace();
        }
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

            // Apply global stylesheet
            if (globalStylesheet != null) {
                scene.getStylesheets().add(globalStylesheet);
                System.out.println("✓ Stylesheet applied to scene");
            } else {
                System.err.println("✗ WARNING: globalStylesheet is null!");
            }

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