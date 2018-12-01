package Controller;

import Part_1.Indexer;
import Part_1.Parse;
import Part_1.ReadFile;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.CheckBox;
import javafx.stage.DirectoryChooser;
import java.io.File;

/**
 * This class controlls all the GUI elements from the main.fxml file
 */
public class Controller {

    public TextField postingPath;
    public TextField corpusPath;
    public CheckBox stemmingCheckBox;
    public ChoiceBox<String> languageChoiceBox;
    public static String postingPathText;

    /**
     * opens a Directory Chooser window in order to choose a directory path for the corpus and for the stop words file
     * @param actionEvent - unused
     */
    public void onCorpusBrowse(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose a path");
        File selectedDirectory = directoryChooser.showDialog(corpusPath.getScene().getWindow());
        if (selectedDirectory != null)
            corpusPath.setText(selectedDirectory.getAbsolutePath());
    }

    /**
     * opens a Directory Chooser window in order to choose a directory path for the posting files
     * @param actionEvent - unused
     */
    public void onPostingBrowse(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose a path");
        File selectedDirectory = directoryChooser.showDialog(postingPath.getScene().getWindow());
        if (selectedDirectory != null)
            postingPath.setText(selectedDirectory.getAbsolutePath());
    }

    /**
     * activates the indexing process
     * @param actionEvent - unused
     */
    public void onActivate(ActionEvent actionEvent) {
        Parse.stemming = stemmingCheckBox.isSelected();
        if (postingPath.getText().equals("") || corpusPath.getText().equals("")) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("You must fill all necessary paths!");
            alert.show();
        }
        else {
            if ((new File(postingPath.getText())).exists()) {
                postingPathText = postingPath.getText();
                String dirPath = corpusPath.getText();
                File stopWords = new File(dirPath + "\\stop words");
                if (stopWords.exists()) {
                    Parse.stemming = stemmingCheckBox.isSelected();
                    if (Parse.stemming)
                        (new File(postingPathText + "\\postingFilesWithStemming")).mkdir();
                    else
                        (new File(postingPathText + "\\postingFilesWithoutStemming")).mkdir();
                    Parse parse = new Parse(dirPath + "\\stop words");
                    ReadFile readFile = new ReadFile(dirPath);
                    Indexer indexer = new Indexer();
                    Thread readFileThread = new Thread(readFile);
                    Thread parseThread = new Thread(parse);
                    Thread indexThread = new Thread(indexer);
                    long startTime = System.nanoTime();
                    parseThread.start();
                    readFileThread.start();
                    indexThread.start();
                    try {
                        readFileThread.join();
                        parseThread.join();
                        indexThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // ------ THE FINAL ALERT BOX INDICATING THE INDEXING PROCESS IS DONE ------
                    finally {
                        double totalTimeInSeconds = (System.nanoTime() - startTime)*Math.pow(10,-9);
                        int totalTimeInMinutes = (int)(totalTimeInSeconds / 60);
                        int remainingSeconds = (int)(totalTimeInSeconds % 60);
                        String totalTime = "Total time: " + totalTimeInMinutes + " minutes and " + remainingSeconds + " seconds.";
                        String docCount = "Total documents indexed: " + ReadFile.docCount;
                        String termCount = "Total unique words found: " + Indexer.totalUniqueTerms;
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setHeaderText("Indexing Done!");
                        alert.setContentText(totalTime + "\n" + docCount + "\n" + termCount);
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("You must choose an existing stop words file path!");
                    alert.show();
                }
            }
            else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("You must choose an existing posting files path!");
                alert.show();
            }
        }
    }

    /**
     * resets all the data saved on the disk regarding the corpus and posting files
     * @param actionEvent - unused
     */
    public void onReset(ActionEvent actionEvent) {
        corpusPath.setText("");
        postingPath.setText("");
        stemmingCheckBox.setSelected(true);
        File dir = new File(postingPathText);
        for (File dir2 : dir.listFiles()) {
            for (File f : dir2.listFiles())
                f.delete();
            dir2.delete();
        }
        Indexer.termDictionary = null;
        Indexer.documentDictionary = null;
    }

    /**
     * shows the dictionary for the corpus
     * @param actionEvent - unused
     */
    public void onDictionaryShow(ActionEvent actionEvent) {
    }

    /**
     * loads a new dictionary for the corpus
     * @param actionEvent - unused
     */
    public void onDictionaryLoad(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose a path");
        File selectedDirectory = directoryChooser.showDialog(postingPath.getScene().getWindow());
        if (selectedDirectory != null)
            postingPath.setText(selectedDirectory.getAbsolutePath());
    }
}