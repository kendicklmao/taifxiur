package client;

import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class FakeNavigator implements INavigator {
    private final List<String> switchedScenes = new ArrayList<>();

    @Override
    public void setStage(Stage stage) {
        // Not needed for tests
    }

    @Override
    public void switchScene(String fxml) {
        switchedScenes.add(fxml);
    }

    public List<String> getSwitchedScenes() {
        return switchedScenes;
    }

    public String getLastSwitchedScene() {
        if (switchedScenes.isEmpty()) {
            return null;
        }
        return switchedScenes.get(switchedScenes.size() - 1);
    }
}

