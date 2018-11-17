package Controller;

import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.CheckBox;
import javafx.stage.DirectoryChooser;

import java.io.File;

public class Controller {

    public TextField postingPath;
    public TextField corpusPath;
    public CheckBox stemmingCheckBox;
    public ChoiceBox<String> languageChoiceBox;

    public void onCorpusBrowse(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose a path");
        File selectedDirectory = directoryChooser.showDialog(corpusPath.getScene().getWindow());
        corpusPath.setText(selectedDirectory.getAbsolutePath());
    }

    public void onPostingBrowse(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose a path");
        File selectedDirectory = directoryChooser.showDialog(postingPath.getScene().getWindow());
        postingPath.setText(selectedDirectory.getAbsolutePath());
    }

    public void onActivate(ActionEvent actionEvent) {
        boolean stemming = stemmingCheckBox.isSelected();
        if (postingPath.getText().equals("") || corpusPath.getText().equals("")) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("You must fill all necessary paths!");
            alert.show();
        }
        else {
            if (stemming) {

            } else {

            }
        }
    }

    public void onReset(ActionEvent actionEvent) {
        corpusPath.setText("");
        postingPath.setText("");
    }

    public void onDictionaryShow(ActionEvent actionEvent) {
    }

    public void onDictionaryLoad(ActionEvent actionEvent) {

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose a path");
        File selectedDirectory = directoryChooser.showDialog(postingPath.getScene().getWindow());
        postingPath.setText(selectedDirectory.getAbsolutePath());
    }
}