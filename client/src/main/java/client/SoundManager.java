package client;

import javafx.scene.media.AudioClip;

public class SoundManager {
    private static final AudioClip transitionSound =
            new AudioClip(
                    SoundManager.class
                            .getResource("/sounds/transition.mp3")
                            .toExternalForm()
            );

    public static void playTransition() {

        transitionSound.setVolume(0.5);

        transitionSound.play();
    }
}
