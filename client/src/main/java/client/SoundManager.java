package client;

import javafx.scene.media.AudioClip;

public class SoundManager {

    private static final AudioClip clickSound =
            new AudioClip(
                    SoundManager.class
                            .getResource("/sounds/click.mp3")
                            .toExternalForm()
            );

    public static void playClick() {

        clickSound.setVolume(0.7);

        clickSound.play();
    }
}
