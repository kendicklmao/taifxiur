package client;

import javafx.scene.media.AudioClip;

public class SoundManager {
    private static final AudioClip popupSound =
            new AudioClip(
                    SoundManager.class
                            .getResource("/sounds/popup.mp3")
                            .toExternalForm()
            );

    public static void playPopup() {
        popupSound.setVolume(0.5);
        popupSound.play();
    }

    public static void stopPopup() {
        popupSound.stop();
    }
}
