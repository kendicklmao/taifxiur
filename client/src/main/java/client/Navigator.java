package client;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;

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

                stage.setScene(scene);

            } else {

                // CHANGE ROOT ONLY
                Parent oldRoot = scene.getRoot();

//
// OUTRO CHAOS
//

                FadeTransition fadeOut =
                        new FadeTransition(Duration.millis(1200), oldRoot);

                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);

                ScaleTransition shrink =
                        new ScaleTransition(Duration.millis(1200), oldRoot);

                shrink.setToX(0.25);
                shrink.setToY(0.25);

                RotateTransition spinOut =
                        new RotateTransition(Duration.millis(1200), oldRoot);

                spinOut.setByAngle(90);

                TranslateTransition flyOut =
                        new TranslateTransition(Duration.millis(1200), oldRoot);

                flyOut.setByX(-1200);
                flyOut.setByY(250);

                ParallelTransition outTransition =
                        new ParallelTransition(
                                fadeOut,
                                shrink,
                                spinOut,
                                flyOut
                        );

                Scene finalScene = scene;

                outTransition.setOnFinished(event -> {

                    //
                    // PREPARE NEW ROOT
                    //

                    root.setOpacity(0);

                    root.setScaleX(3.5);
                    root.setScaleY(3.5);

                    root.setRotate(-90);

                    root.setTranslateX(1400);
                    root.setTranslateY(-300);

                    finalScene.setRoot(root);

                    //
                    // INTRO INSANITY
                    //

                    FadeTransition fadeIn =
                            new FadeTransition(Duration.millis(1800), root);

                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);

                    ScaleTransition zoomIn =
                            new ScaleTransition(Duration.millis(1800), root);

                    zoomIn.setToX(1);
                    zoomIn.setToY(1);

                    RotateTransition spinIn =
                            new RotateTransition(Duration.millis(1800), root);

                    spinIn.setToAngle(0);

                    TranslateTransition slideIn =
                            new TranslateTransition(Duration.millis(1800), root);

                    slideIn.setToX(0);
                    slideIn.setToY(0);

                    ParallelTransition intro =
                            new ParallelTransition(
                                    fadeIn,
                                    zoomIn,
                                    spinIn,
                                    slideIn
                            );

                    //
                    // APOCALYPTIC SHAKE 😭
                    //

                    Timeline shake = new Timeline(

                            new KeyFrame(Duration.millis(0),
                                    new KeyValue(root.translateXProperty(), 0)),

                            new KeyFrame(Duration.millis(120),
                                    new KeyValue(root.translateXProperty(), -45)),

                            new KeyFrame(Duration.millis(240),
                                    new KeyValue(root.translateXProperty(), 45)),

                            new KeyFrame(Duration.millis(360),
                                    new KeyValue(root.translateXProperty(), -40)),

                            new KeyFrame(Duration.millis(480),
                                    new KeyValue(root.translateXProperty(), 40)),

                            new KeyFrame(Duration.millis(600),
                                    new KeyValue(root.translateXProperty(), -35)),

                            new KeyFrame(Duration.millis(720),
                                    new KeyValue(root.translateXProperty(), 35)),

                            new KeyFrame(Duration.millis(840),
                                    new KeyValue(root.translateXProperty(), -30)),

                            new KeyFrame(Duration.millis(960),
                                    new KeyValue(root.translateXProperty(), 30)),

                            new KeyFrame(Duration.millis(1080),
                                    new KeyValue(root.translateXProperty(), -25)),

                            new KeyFrame(Duration.millis(1200),
                                    new KeyValue(root.translateXProperty(), 25)),

                            new KeyFrame(Duration.millis(1320),
                                    new KeyValue(root.translateXProperty(), -20)),

                            new KeyFrame(Duration.millis(1440),
                                    new KeyValue(root.translateXProperty(), 20)),

                            new KeyFrame(Duration.millis(1560),
                                    new KeyValue(root.translateXProperty(), -15)),

                            new KeyFrame(Duration.millis(1680),
                                    new KeyValue(root.translateXProperty(), 15)),

                            new KeyFrame(Duration.millis(1800),
                                    new KeyValue(root.translateXProperty(), -10)),

                            new KeyFrame(Duration.millis(1920),
                                    new KeyValue(root.translateXProperty(), 10)),

                            new KeyFrame(Duration.millis(2040),
                                    new KeyValue(root.translateXProperty(), 0))
                    );

                    //
                    // FINAL EPIC BOUNCE
                    //

                    ScaleTransition bounce =
                            new ScaleTransition(Duration.millis(700), root);

                    bounce.setFromX(1);
                    bounce.setFromY(1);

                    bounce.setToX(1.15);
                    bounce.setToY(1.15);

                    bounce.setCycleCount(2);
                    bounce.setAutoReverse(true);

                    //
                    // FULL GAMER INTRO
                    //

                    SequentialTransition fullTransition =
                            new SequentialTransition(
                                    intro,
                                    shake,
                                    bounce
                            );

                    fullTransition.play();
                });
                outTransition.play();
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
}