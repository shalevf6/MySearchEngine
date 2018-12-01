package Controller;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

/**
 * this class represents the dictionary view window
 */
public class DictionaryController {

    public Button closeButton;
    public TextArea textArea;

    /**
     * sets the dictionary to be viewed
     * @param textToShow - the string which represents the dictionary
     */
    DictionaryController(String textToShow) {
        textArea.setText(textToShow);
        textArea.setWrapText(true);
        textArea.setEditable(false);
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
