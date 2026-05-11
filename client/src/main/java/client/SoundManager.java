package client;

import javafx.scene.media.AudioClip;

public class SoundManager {

    private static final AudioClip clickSound =
            new AudioClip(
                    SoundManager.class
                            .getResource("/sounds/click.mp3")
                            .toExternalForm()
            );

    private static final AudioClip transitionSound =
            new AudioClip(
                    SoundManager.class
                            .getResource("/sounds/transition.mp3")
                            .toExternalForm()
            );

    public static void playClick() {

        clickSound.setVolume(0.7);

        clickSound.play();
    }

    public static void playTransition() {

        transitionSound.setVolume(0.6);

        transitionSound.play();
    }
}
