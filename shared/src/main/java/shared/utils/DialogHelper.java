package shared.utils;

import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

public class DialogHelper {
    public static <T> void applyCustomStyle(Dialog<T> dialog) {
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(DialogHelper.class.getResource("/styles.css").toExternalForm());
        pane.getStyleClass().add("my-dialog");
    }
}
