package client;

import java.util.ArrayList;
import java.util.List;

public class FakeNavigator implements INavigator {
    private List<String> switchedScenes = new ArrayList<>();

    @Override
    public void switchScene(String fxml) {
        switchedScenes.add(fxml);
    }

    public String getLastSwitchedScene() {
        if (switchedScenes.isEmpty()) return null;
        return switchedScenes.get(switchedScenes.size() - 1);
    }

    public List<String> getSwitchedScenes() {
        return switchedScenes;
    }
}
