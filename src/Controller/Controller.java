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
    private boolean alreadyIndexed = false;

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
        if (!alreadyIndexed && !startsIndexing) {
            Parse.stemming = stemmingCheckBox.isSelected();
            if (postingPath.getText().equals("") || corpusPath.getText().equals("")) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("You must fill all necessary paths!");
                alert.show();
            } else {
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
                        Parse parse = new Parse(dirPath + "\\stop words");
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
                        }
                        // ------ THE FINAL ALERT BOX INDICATING THE INDEXING PROCESS IS DONE ------
                        finally {
                            startsIndexing = false;
                            alreadyIndexed = true;
                            double totalTimeInSeconds = (System.nanoTime() - startTime) * Math.pow(10, -9);
                            int totalTimeInMinutes = (int) (totalTimeInSeconds / 60);
                            int remainingSeconds = (int) (totalTimeInSeconds % 60);
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
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("You must choose an existing posting files path!");
                    alert.show();
                }
            }
        }
    }

    /**
     * resets all the data saved on the disk regarding the corpus and posting files
     * @param actionEvent - unused
     */
    public void onReset(ActionEvent actionEvent) {
        if (!startsIndexing) {
            corpusPath.setText("");
            postingPath.setText("");
            stemmingCheckBox.setSelected(true);
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("/fxml/postingPath"))));
                File dir = new File(bufferedReader.readLine());
                bufferedReader.close();
                for (File dir2 : dir.listFiles()) {
                    for (File f : dir2.listFiles())
                        f.delete();
                    dir2.delete();
                }
                Indexer.termDictionary = null;
                Indexer.documentDictionary = null;
                alreadyIndexed = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * shows the dictionary for the corpus
     * @param actionEvent - unused
     */
    public void onDictionaryShow(ActionEvent actionEvent) {
        if (!startsIndexing) {
            try {
                Stage stage = new Stage();
                stage.setTitle("The Corpus's Dictionary");
                FXMLLoader fxmlLoader = new FXMLLoader();
                Parent root = fxmlLoader.load(getClass().getResource("/fxml/dictionary.fxml"));
                fxmlLoader.setController(new DictionaryController(Indexer.getDictionaryString()));
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
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose a path");
            File selectedDirectory = directoryChooser.showDialog(postingPath.getScene().getWindow());
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
}