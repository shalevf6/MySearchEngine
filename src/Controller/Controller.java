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
import java.util.HashMap;

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
    private TextField tempPostingPath = new TextField();

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
                            File stopWords = new File(dirPath + "\\corpus\\stop_words.txt");
                            if (stopWords.exists()) {
                                Parse.stemming = stemmingCheckBox.isSelected();
                                if (Parse.stemming) {
                                    (new File(postingPathText + "\\postingFilesWithStemming")).mkdir();
                                    alreadyIndexedWithStemming = true;
                                } else {
                                    (new File(postingPathText + "\\postingFilesWithoutStemming")).mkdir();
                                    alreadyIndexedWithoutStemming = true;
                                }
                                (new File(postingPathText + "\\postingForCities")).mkdir();
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
                                Parse parse = new Parse(dirPath + "\\corpus\\stop_words.txt");
                                ReadFile readFile = new ReadFile(dirPath);
                                Indexer indexer = new Indexer();
                                Thread readFileThread = new Thread(readFile);
                                Thread parseThread = new Thread(parse);
                                Thread indexThread = new Thread(indexer);
                                long startTime = System.nanoTime();
                                startsIndexing = true;
                                parseThread.start();
                                readFileThread.start();
                                indexThread.start();
                                try {
                                    readFileThread.join();
                                    parseThread.join();
                                    indexThread.join();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } finally {
                                    // ------ THE FINAL ALERT BOX INDICATING THE INDEXING PROCESS IS DONE ------
                                    startsIndexing = false;
                                    if (Parse.stemming)
                                        alreadyIndexedWithStemming = true;
                                    else
                                        alreadyIndexedWithoutStemming = true;
                                    double totalTimeInSeconds = (System.nanoTime() - startTime) * Math.pow(10, -9);
                                    int totalTimeInMinutes = (int) (totalTimeInSeconds / 60);
                                    int remainingSeconds = (int) (totalTimeInSeconds % 60);
                                    String totalTime = totalTimeInMinutes + " minutes and " + remainingSeconds + " seconds.";
                                    String docCount = String.valueOf(Indexer.totalDocuments);
                                    String termCount = String.valueOf(Indexer.totalUniqueTerms);
                                    Alert doneIndexing = new Alert(Alert.AlertType.INFORMATION);
                                    doneIndexing.setHeaderText("Indexing Done!");
                                    doneIndexing.setContentText("Total time to index: " + totalTime + "\n\"Total documents indexed: " +
                                            docCount + "\nTotal unique words found: " + termCount);
                                    doneIndexing.show();
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
            alreadyIndexedWithStemming = false;
            alreadyIndexedWithoutStemming = false;
            Parse.resetAll();
            Indexer.resetAll();
        }
    }

    /**
     * shows the dictionary for the corpus
     * @param actionEvent - unused
     */
    public void onDictionaryShow(ActionEvent actionEvent) {
        if (!startsIndexing && (alreadyIndexedWithStemming || alreadyIndexedWithoutStemming)) {
            try {
                Stage stage = new Stage();
                stage.setTitle("The Corpus's Dictionary");
                FXMLLoader fxmlLoader = new FXMLLoader();
                Parent root = fxmlLoader.load(getClass().getResource("/fxml/dictionary.fxml"));
                if (Indexer.isDictionaryStemmed)
                    DictionaryController.setDictionary(Indexer.readDictionaryForShowToMemory(postingPathText +
                            "\\postingFilesWithStemming\\termDictionaryForShow"));
                else
                    DictionaryController.setDictionary(Indexer.readDictionaryForShowToMemory(postingPathText +
                        "\\postingFilesWithoutStemming\\termDictionaryForShow"));
                fxmlLoader.setController(new DictionaryController());
                Scene scene = new Scene(root, 600, 400);
                stage.setScene(scene);
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.show();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
            showErrorAlert("No dictionary to show!");
    }

    /**
     * loads a new dictionary for the corpus
     * @param actionEvent - unused
     */
    public void onDictionaryLoad(ActionEvent actionEvent) {
        if (!startsIndexing && (!alreadyIndexedAll())) {
            boolean stemming = stemmingCheckBox.isSelected();
            chooseAndSaveDirectoryPath(tempPostingPath);
            if ((stemming && alreadyIndexedWithStemming) || (!stemming && alreadyIndexedWithoutStemming))
                showErrorAlert("Already loaded / indexed this option!");
            else {
                try {
                    boolean loadCityDictionary = true;
                    if (stemming) {
                        // to know if i need to load also the city dictionary
                        if (alreadyIndexedWithoutStemming) {
                            boolean checksOut = checkIfTheSameAndLoaded(tempPostingPath.getText(), true);
                            loadCityDictionary = false;
                            if (!checksOut)
                                return;
                        } else
                            postingPathText = tempPostingPath.getText();
                        alreadyIndexedWithStemming = true;
                    } else {
                        // to know if i need to load also the city dictionary
                        if (alreadyIndexedWithStemming) {
                            boolean checksOut = checkIfTheSameAndLoaded(tempPostingPath.getText(), false);
                            loadCityDictionary = false;
                            if (checksOut)
                                return;
                        } else
                            postingPathText = tempPostingPath.getText();
                        alreadyIndexedWithoutStemming = true;
                    }
                    if (loadCityDictionary)
                        Indexer.readDictionaryToMemory(tempPostingPath.getText() + "\\postingForCities\\cityDictionary", 3);
                    Indexer.loadAllDictionariesToMemory(tempPostingPath.getText(), stemming);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setContentText("Dictionary Loaded Successfully!");
                    alert.show();
                } catch (IOException | ClassNotFoundException e) {
                    showErrorAlert("Not all dictionary files found in path! Try again");
                }
            }
        }
    }

    /**
     * checks if the load of the dictionaries has the same corpus details as the last load
     * @param path - the posting files path
     * @param stemming - is the dictionary loaded is stemmed or not
     * @return - true if everything is in order. else - false
     */
    private boolean checkIfTheSameAndLoaded(String path, boolean stemming) {
        if (!path.equals(postingPathText)) {
            showErrorAlert("Path not the same!\n Hit reset in order to load a new dictionary!");
            return false;
        }
        if (Indexer.corpusCityDictionary.size() == 0) {
            showErrorAlert("City dictionary doesn't exist from last load / indexing!\n Hit reset in order to load a new dictionary!");
            return false;
        }
        File documentDictionary;
        if (stemming)
            documentDictionary = new File (postingPathText + "\\postingFilesWithStemming\\documentDictionary");
        else
            documentDictionary = new File (postingPathText + "\\postingFilesWithoutStemming\\documentDictionary");
        ObjectInputStream objectInputStream;
        HashMap<String,int[]> documentDictionaryObject = null;
        try {
            objectInputStream = new ObjectInputStream(new FileInputStream(documentDictionary));
            documentDictionaryObject = (HashMap<String, int[]>) objectInputStream.readObject();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (documentDictionaryObject.size() != Indexer.totalDocuments) {
            showErrorAlert("Document dictionary doesn't have the same document amount from last load / indexing!\n Hit reset in order to load a new dictionary!");
            return false;
        }
        return true;
    }

    /**
     * chooses and sets a path for a given directory
     * @param path - the text field of the given path
     */
    private void chooseAndSaveDirectoryPath(TextField path) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose a path");
        File selectedDirectory = directoryChooser.showDialog(postingPath.getScene().getWindow());
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

    public static void main(String[] args) {

    }
}