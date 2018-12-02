package Controller;

import Part_1.Indexer;
import Part_1.Parse;
import Part_1.ReadFile;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.CheckBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;

/**
 * This class controlls all the GUI elements from the main.fxml file
 */
public class Controller {

    public TextField postingPath;
    public TextField corpusPath;
    public CheckBox stemmingCheckBox;
    public ChoiceBox<String> languageChoiceBox;
    public static String postingPathText;
    private boolean startsIndexing = false;
    private boolean alreadyIndexedWithStemming = false;
    private boolean alreadyIndexedWithoutStemming = false;

    /**
     * opens a Directory Chooser window in order to choose a directory path for the corpus and for the stop words file
     * @param actionEvent - unused
     */
    public void onCorpusBrowse(ActionEvent actionEvent) {
        if (!startsIndexing)
            chooseAndSaveDirectoryPath(corpusPath);
    }

    /**
     * opens a Directory Chooser window in order to choose a directory path for the posting files
     * @param actionEvent - unused
     */
    public void onPostingBrowse(ActionEvent actionEvent) {
        if (!startsIndexing)
            chooseAndSaveDirectoryPath(postingPath);
    }

    /**
     * activates the indexing process
     * @param actionEvent - unused
     */
    public void onActivate(ActionEvent actionEvent) {
        if (!alreadyIndexedAll() && !startsIndexing) {
            Parse.stemming = stemmingCheckBox.isSelected();
            String alert = checkIfIndexed();
            if (!alert.equals(""))
                showErrorAlert(alert);
            else {
                if (postingPath.getText().equals("") || corpusPath.getText().equals("")) {
                    showErrorAlert("You must fill all necessary paths!");
                } else {
                    if ((new File(postingPath.getText())).exists()) {
                        postingPathText = postingPath.getText();
                        String dirPath = corpusPath.getText();
                        File corpusDirectory = new File(dirPath + "\\corpus");
                        if (!corpusDirectory.exists()) {
                            showErrorAlert("You must choose an existing corpus folder path!");
                        } else {
                            File stopWords = new File(dirPath + "\\corpus\\stop words");
                            if (stopWords.exists()) {
                                Parse.stemming = stemmingCheckBox.isSelected();
                                if (Parse.stemming) {
                                    (new File(postingPathText + "\\postingFilesWithStemming")).mkdir();
                                    alreadyIndexedWithStemming = true;
                                } else {
                                    (new File(postingPathText + "\\postingFilesWithoutStemming")).mkdir();
                                    alreadyIndexedWithoutStemming = true;
                                }
                        /*
                        File postingPathFile = new File (ClassLoader.g"\\fxml\\postingPath");
                        try {
                            postingPathFile.createNewFile();
                            FileOutputStream fileOutputStream = new FileOutputStream(postingPathFile.getAbsolutePath());
                            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
                            bufferedWriter.write(postingPathText);
                            bufferedWriter.close();
                            outputStreamWriter.close();
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        */
                                Parse parse = new Parse(dirPath + "\\corpus\\stop words");
                                ReadFile readFile = new ReadFile(dirPath, parse);
                                Indexer indexer = new Indexer();
                                Thread readFileThread = new Thread(readFile);
                                Thread indexThread = new Thread(indexer);
                                long startTime = System.nanoTime();
                                startsIndexing = true;
                                readFileThread.start();
                                indexThread.start();
                                try {
                                    readFileThread.join();
                                    indexThread.join();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                // ------ THE FINAL ALERT BOX INDICATING THE INDEXING PROCESS IS DONE ------
                                finally {
                                    startsIndexing = false;
                                    if (Parse.stemming)
                                        alreadyIndexedWithStemming = true;
                                    else
                                        alreadyIndexedWithoutStemming = true;
                                    double totalTimeInSeconds = (System.nanoTime() - startTime) * Math.pow(10, -9);
                                    int totalTimeInMinutes = (int) (totalTimeInSeconds / 60);
                                    int remainingSeconds = (int) (totalTimeInSeconds % 60);
                                    String totalTime = "Total time: " + totalTimeInMinutes + " minutes and " + remainingSeconds + " seconds.";
                                    String docCount = "Total documents indexed: " + ReadFile.docCount;
                                    String termCount = "Total unique words found: " + Indexer.totalUniqueTerms;
                                    Alert doneIndexing = new Alert(Alert.AlertType.INFORMATION);
                                    doneIndexing.setHeaderText("Indexing Done!");
                                    doneIndexing.setContentText("Total time to index: " + totalTime + "\nTotal document count: " +
                                            docCount + "\nTotal unique words found: " + termCount);
                                }
                            } else {
                                showErrorAlert("You must choose an existing stop words file path!");
                            }
                        }
                    } else {
                        showErrorAlert("You must choose an existing posting files path!");
                    }
                }

            }
        }
    }

    /**
     * check if there was already an indexing done before
     * @return - the appropriate error if true. else - returns an empty string
     */
    private String checkIfIndexed() {
        boolean stemming = Parse.stemming;
        if (stemming && alreadyIndexedWithStemming)
            return "Already indexed with stemming!";
        if (!stemming && alreadyIndexedWithoutStemming)
            return "Already indexed without stemming!";
        return "";
    }

    /**
     * resets all the data saved on the disk regarding the corpus and posting files
     * @param actionEvent - unused
     */
    public void onReset(ActionEvent actionEvent) {
        if (!startsIndexing && (alreadyIndexedWithStemming || alreadyIndexedWithoutStemming)) {
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
            alreadyIndexedWithStemming = false;
            alreadyIndexedWithoutStemming = false;
        }
    }

    /**
     * shows the dictionary for the corpus
     * @param actionEvent - unused
     */
    public void onDictionaryShow(ActionEvent actionEvent) {
        if (!startsIndexing && (alreadyIndexedWithStemming || alreadyIndexedWithoutStemming)) {
            try {
                boolean stemming = stemmingCheckBox.isSelected();
                Stage stage = new Stage();
                stage.setTitle("The Corpus's Dictionary");
                FXMLLoader fxmlLoader = new FXMLLoader();
                Parent root = fxmlLoader.load(getClass().getResource("/fxml/dictionary.fxml"));
                fxmlLoader.setController(new DictionaryController(Indexer.getDictionaryString(stemming)));
                Scene scene = new Scene(root, 600, 400);
                stage.setScene(scene);
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.show();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * loads a new dictionary for the corpus
     * @param actionEvent - unused
     */
    public void onDictionaryLoad(ActionEvent actionEvent) {
        if (!startsIndexing) {
            boolean stemming = stemmingCheckBox.isSelected();
            if ((stemming && !alreadyIndexedWithStemming) || (!stemming && !alreadyIndexedWithoutStemming))
                showErrorAlert("Requested dictionary has yet to be created. You must first run indexing!");
            else {
                Indexer.getTermDictionary(stemming);
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setContentText("Dictionary Loaded Successfully!");
            }
        }
    }

    /**
     * chooses and sets a path for a given directory
     * @param path - the text field of the given path
     */
    private void chooseAndSaveDirectoryPath(TextField path) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose a path");
        File selectedDirectory = directoryChooser.showDialog(path.getScene().getWindow());
        if (selectedDirectory != null)
            path.setText(selectedDirectory.getAbsolutePath());
    }

    /**
     * checks if the corpus has already been indexed by every option available
     * @return - true ir true. else - false
     */
    private boolean alreadyIndexedAll() {
        return alreadyIndexedWithStemming && alreadyIndexedWithoutStemming;
    }

    private void showErrorAlert(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(error);
        alert.show();
    }
}