package client;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class Navigator implements INavigator {

    private static Stage stage;
    private static String globalStylesheet;

    private static final Navigator instance = new Navigator();

    public static Navigator getInstance() {
        return instance;
    }

    public static void setStage(Stage s){

        stage = s;

        // Load CSS
        try {

            globalStylesheet =
                    Navigator.class
                            .getClassLoader()
                            .getResource("styles.css")
                            .toExternalForm();

            System.out.println("CSS loaded: " + globalStylesheet);

        } catch (Exception e) {

            System.err.println("ERROR: Could not find styles.css!");
            e.printStackTrace();
        }
    }

    @Override
    public void switchScene(String fxml) {
        switchSceneStatic(fxml);
    }

    public static void switchSceneStatic(String fxml){

        try{

            boolean isMaximized = stage.isMaximized();

            double width = stage.getWidth();
            double height = stage.getHeight();
            double x = stage.getX();
            double y = stage.getY();

            FXMLLoader loader =
                    new FXMLLoader(
                            Navigator.class
                                    .getClassLoader()
                                    .getResource(fxml)
                    );

            Parent root = loader.load();

            Scene scene = stage.getScene();

            // CREATE SCENE FIRST TIME
            if (scene == null) {

                scene = new Scene(root, width, height);

                // APPLY CSS
                if (globalStylesheet != null) {

                    scene.getStylesheets().add(globalStylesheet);

                    System.out.println("Stylesheet applied");
                }

                // GLOBAL CLICK SOUND
                applyGlobalClickSound(scene);

                stage.setScene(scene);

            } else {

                // CHANGE ROOT ONLY
                scene.setRoot(root);
            }

            Platform.runLater(() -> {

                if (isMaximized) {

                    stage.setMaximized(true);

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

    // =========================================
    // GLOBAL CLICK SOUND
    // =========================================

    private static void applyGlobalClickSound(Scene scene){

        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {

            SoundManager.playClick();
        });
    }
}