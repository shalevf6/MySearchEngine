package Controller;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import java.util.List;
import java.util.Observable;

/**
 * this class represents the dictionary view window
 */
public class DictionaryController {

    public Button closeButton;
    public ListView<String> listView;
    public static ObservableList<String> dictionaryList;

    /**
     * sets the dictionary to be viewed
     */
    public DictionaryController() {
        listView = new ListView<>();
        listView.setLayoutX(82.0);
        listView.setLayoutY(71.0);
        listView.setPrefHeight(262.0);
        listView.setPrefWidth(418.0);
        listView.setItems(dictionaryList);
        dictionaryList = null;
    }

    /**
     * sets the dictionary to be viewed
     * @param listToShow - the sorted string list which represents the dictionary
     */
    public static void setDictionary(ObservableList<String> listToShow) {
        dictionaryList = listToShow;
    }

    /**
     * closes the dictionary view window
     * @param actionEvent - unused
     */
    public void closeWindow(ActionEvent actionEvent) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
