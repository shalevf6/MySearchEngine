package Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import java.util.List;

/**
 * this class represents the dictionary view window
 */
public class DictionaryController {

    public Button closeButton;
    public ListView<String> listView;

    /**
     * sets the dictionary to be viewed
     */
    public DictionaryController(List<String> dictionaryList) {
        ObservableList observableList = FXCollections.observableList(dictionaryList);
        listView = new ListView<>();
        listView.setLayoutX(82);
        listView.setLayoutY(71);
        listView.setPrefHeight(262);
        listView.setPrefHeight(418);
        listView.getItems().setAll(observableList);
    }

    /**
     * sets the dictionary to be viewed
     * @param dictionaryList - the sorted string list which represents the dictionary
     */
    void setDictionary(List<String> dictionaryList) {
        ObservableList observableList = FXCollections.observableList(dictionaryList);
        listView = new ListView<>();
        listView.setLayoutX(82);
        listView.setLayoutY(71);
        listView.setPrefHeight(262);
        listView.setPrefHeight(418);
        listView.getItems().setAll(observableList);
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
