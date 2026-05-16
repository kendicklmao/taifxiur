package client;

import javafx.application.Application;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static MediaPlayer mediaPlayer;

    @Override
    public void start(Stage stage) {

        Media media = new Media(getClass().getResource("/sounds/theme.mp3").toExternalForm());

        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // replay vô hạn
        mediaPlayer.setVolume(0.5); // âm lượng 50%
        mediaPlayer.play();

        stage.setTitle("Auction House");
        stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/logo.png")));
        stage.setMinWidth(900);
        stage.setMinHeight(700);
        stage.setMaximized(true);

        Navigator.setStage(stage);
        Navigator.switchSceneStatic("login.fxml");

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}