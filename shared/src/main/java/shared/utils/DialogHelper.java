package shared.utils;

import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

public class DialogHelper {
    public static <T> void applyCustomStyle(Dialog<T> dialog) {
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(DialogHelper.class.getResource("/styles.css").toExternalForm());
        pane.getStyleClass().add("my-dialog");
        
        javafx.scene.Node cancelBtn = pane.lookupButton(javafx.scene.control.ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.getStyleClass().add("btn-ghost-white");
        }
    }
}