package Controller;

import Part_1.Parse;
import Part_1.ReadFile;
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
    public static boolean stemming = true;

    public void onCorpusBrowse(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose a path");
        File selectedDirectory = directoryChooser.showDialog(corpusPath.getScene().getWindow());
        if (selectedDirectory != null)
            corpusPath.setText(selectedDirectory.getAbsolutePath());
    }

    public void onPostingBrowse(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose a path");
        File selectedDirectory = directoryChooser.showDialog(postingPath.getScene().getWindow());
        if (selectedDirectory != null)
            postingPath.setText(selectedDirectory.getAbsolutePath());
    }

    public void onActivate(ActionEvent actionEvent) {
        stemming = stemmingCheckBox.isSelected();
        if (postingPath.getText().equals("") || corpusPath.getText().equals("")) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("You must fill all necessary paths!");
            alert.show();
        }
        else {
            String dirPath = corpusPath.getText();
            File stopWords = new File(dirPath + "\\stop words");
            if (stopWords.exists()) {
                Parse parse = new Parse(dirPath + "\\stop words");
                ReadFile readFile = new ReadFile(dirPath);
                Thread readFileThread = new Thread(readFile);
                Thread parseThread = new Thread(parse);
                parseThread.start();
                readFileThread.start();
                try {
                    readFileThread.join();
                    parseThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("You must choose an existing stop words file path!");
                alert.show();
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
        if (selectedDirectory != null)
            postingPath.setText(selectedDirectory.getAbsolutePath());
    }
}