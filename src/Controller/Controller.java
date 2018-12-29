package Controller;

import Part_1.Indexer;
import Part_1.Parse;
import Part_1.ReadFile;
import Part_2.Searcher;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.*;
import java.util.*;

/**
 * This class controls all the GUI elements from the main.fxml file
 */
public class Controller {

    public TextField postingPath;
    public TextField corpusPath;
    public CheckBox stemmingCheckBox;
    public TextField queryPath;
    public CheckBox semanticTreatmentCheckBox;
    public Text part2Text;
    public Text loadQueryText;
    public Button browseQueryButton;
    public Button showCitiesButton;
    public Text orText;
    public Text insertQueryText;
    public Button runButton;
    public Button loadStopWordsButton;
    public Text stopWordsInstructionsText;
    private String stopWordsPath;
    public static String postingPathText;
    private boolean startsIndexing = false;
    private boolean alreadyIndexedWithStemming = false;
    private boolean alreadyIndexedWithoutStemming = false;
    private TextField tempPostingPath = new TextField();
    public static List<String> languages = new LinkedList<>();
    public static HashMap<String, Integer> citiesToFilter = new HashMap<>();
    private HashMap<String, Queue<String>> queryResults = new HashMap<>();

    /**
     * opens a Directory Chooser window in order to choose a directory path for the corpus and for the stop words file
     */
    public void onCorpusBrowse() {
        if (!startsIndexing)
            chooseAndSaveDirectoryPath(corpusPath);
    }

    /**
     * opens a Directory Chooser window in order to choose a directory path for the posting files
     */
    public void onPostingBrowse() {
        if (!startsIndexing)
            chooseAndSaveDirectoryPath(postingPath);
    }

    /**
     * activates the indexing process
     */
    public void onActivate() {
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
                                stopWordsPath = stopWords.getAbsolutePath();
                                if (Parse.stemming) {
                                    (new File(postingPathText + "\\postingFilesWithStemming")).mkdir();
                                    alreadyIndexedWithStemming = true;
                                } else {
                                    (new File(postingPathText + "\\postingFilesWithoutStemming")).mkdir();
                                    alreadyIndexedWithoutStemming = true;
                                }
                                (new File(postingPathText + "\\postingForCities")).mkdir();
                                Parse parse = new Parse(dirPath + "\\corpus\\stop_words.txt", false, null);
                                ReadFile readFile = new ReadFile(dirPath);
                                Indexer indexer = new Indexer();
                                Parse.resetPartially();
                                Indexer.resetPartially();
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
                                    doneIndexing.setContentText("Total time to index: " + totalTime + "\nTotal documents indexed: " +
                                            docCount + "\nTotal unique words found: " + termCount);
                                    loadStopWordsButton.setVisible(false);
                                    stopWordsInstructionsText.setVisible(false);
                                    doneIndexing.show();
                                    showPart2();
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
        } else if (alreadyIndexedAll())
            showErrorAlert("Already indexed / loaded all options!!\n(stemming / non stemming)");
    }

    /**
     * check if there was already an indexing done before
     *
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
     */
    public void onReset() {
        if (!startsIndexing && (alreadyIndexedWithStemming || alreadyIndexedWithoutStemming)) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to reset?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES) {
                corpusPath.setText("");
                postingPath.setText("");
                stemmingCheckBox.setSelected(true);
                File dir = new File(postingPathText);

                File[] dirFiles = dir.listFiles();
                if (dirFiles != null) {
                    for (File dirFile : dirFiles) {
                        File[] dir2Files = dirFile.listFiles();
                        if (dir2Files != null) {
                            for (File dir2File : dir2Files) {
                                dir2File.delete();
                            }
                        }
                        dirFile.delete();
                    }
                }

                alreadyIndexedWithStemming = false;
                alreadyIndexedWithoutStemming = false;
                languages = new LinkedList<>();
                stopWordsPath = null;
                Parse.resetAll();
                Indexer.resetAll();
                tempPostingPath = new TextField();
                hidePart2();
            }
        } else {
            showErrorAlert("No data to be reset!");
        }
    }

    /**
     * shows the dictionary for the corpus
     */
    public void onDictionaryShow() {
        if (!startsIndexing && (alreadyIndexedWithStemming || alreadyIndexedWithoutStemming)) {
            try {
                Stage stage = new Stage();
                stage.setTitle("The Corpus's Dictionary");
                List<String> dictionaryList;
                if (Indexer.isDictionaryStemmed)
                    dictionaryList = Indexer.readDictionaryForShowToMemory(postingPathText + "\\postingFilesWithStemming\\termDictionaryForShow");
                else
                    dictionaryList = Indexer.readDictionaryForShowToMemory(postingPathText + "\\postingFilesWithoutStemming\\termDictionaryForShow");
                if (dictionaryList != null) {
                    ListView<String> listView = new ListView<>();
                    listView.getItems().setAll(FXCollections.observableList(dictionaryList));
                    listView.setEditable(false);
                    listView.prefWidth(805);
                    listView.prefHeight(500);
                    StackPane root = new StackPane();
                    root.prefWidth(805);
                    root.prefWidth(500);
                    Button backButton = new Button("Back");
                    Button demiButton1 = new Button("button");
                    Button demiButton2 = new Button("button");
                    Button demiButton3 = new Button("button");
                    Button demiButton4 = new Button("button");
                    Button demiButton5 = new Button("button");
                    Button demiButton6 = new Button("button");
                    demiButton1.setVisible(false);
                    demiButton2.setVisible(false);
                    demiButton3.setVisible(false);
                    demiButton4.setVisible(false);
                    demiButton5.setVisible(false);
                    demiButton6.setVisible(false);
                    backButton.setWrapText(true);
                    VBox vBox = new VBox();
                    HBox hBox = new HBox();
                    hBox.setSpacing(10);
                    hBox.getChildren().addAll(demiButton1, demiButton2, demiButton3, demiButton4, demiButton5, backButton);
                    vBox.getChildren().addAll(listView, hBox);
                    backButton.setOnAction(event -> stage.close());
                    root.getChildren().addAll(vBox);
                    Scene scene = new Scene(root, 805, 500);
                    stage.initModality(Modality.APPLICATION_MODAL);
                    stage.setScene(scene);
                    stage.show();
                }
                else {
                    showErrorAlert("Dictionary is null!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else
            showErrorAlert("No dictionary to show!");
    }

    /**
     * loads a new dictionary for the corpus
     */
    public void onDictionaryLoad() {
        if (!startsIndexing && (!alreadyIndexedAll())) {
            boolean stemming = stemmingCheckBox.isSelected();
            if ((stemming && alreadyIndexedWithStemming) || (!stemming && alreadyIndexedWithoutStemming))
                showErrorAlert("Already loaded / indexed this option!");
            else {
                chooseAndSaveDirectoryPath(tempPostingPath);
                if (!tempPostingPath.getText().equals(""))
                    // to check if all files exist in the path
                    if (allPostingFilesExist(tempPostingPath.getText(), stemming)) {
                        boolean tempWithStemming = alreadyIndexedWithStemming;
                        boolean tempWithoutStemming = alreadyIndexedWithoutStemming;
                        try {
                            boolean loadCityDictionary = true;
                            boolean firstLoad = false;
                            if (!alreadyIndexedWithStemming && !alreadyIndexedWithoutStemming)
                                firstLoad = true;
                            if (stemming) {
                                // to know if i need to load also the city dictionary
                                if (alreadyIndexedWithoutStemming) {
                                    boolean checksOut = checkIfTheSameAndLoaded(tempPostingPath.getText(), true);
                                    loadCityDictionary = false;
                                    if (!checksOut)
                                        return;
                                }
                                alreadyIndexedWithStemming = true;
                            } else {
                                // to know if i need to load also the city dictionary
                                if (alreadyIndexedWithStemming) {
                                    boolean checksOut = checkIfTheSameAndLoaded(tempPostingPath.getText(), false);
                                    loadCityDictionary = false;
                                    if (!checksOut)
                                        return;
                                }
                                alreadyIndexedWithoutStemming = true;
                            }
                            if (loadCityDictionary) {
                                Indexer.readDictionaryToMemory(tempPostingPath.getText() + "\\postingForCities\\cityDictionary", 3);
                                Indexer.readDictionaryForShowToMemory(tempPostingPath.getText() + "\\languages");
                            }
                            Indexer.loadAllDictionariesToMemory(tempPostingPath.getText(), stemming);
                            postingPathText = tempPostingPath.getText();
                            Indexer.indexedCities = true;
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setContentText("Dictionaries Loaded Successfully!");
                            alert.show();
                            if (firstLoad) {
                                loadStopWordsButton.setVisible(true);
                                stopWordsInstructionsText.setVisible(true);
                            }
                        } catch (IOException | ClassNotFoundException e) {
                            showErrorAlert("Not all dictionary files found in path! Try again");
                            alreadyIndexedWithStemming = tempWithStemming;
                            alreadyIndexedWithoutStemming = tempWithoutStemming;
                            tempPostingPath = new TextField();
                        }
                    } else
                        showErrorAlert("Not all files exist in given path!");
            }
        } else if (alreadyIndexedAll())
            showErrorAlert("Already indexed / loaded all options!!\n(stemming / non stemming)");
    }

    /**
     * checks all of the posting files on load exist
     *
     * @param postingPath - the path for the posting files
     * @param stemming    - stemming or not stemming
     * @return - true if all the files exist. else - false
     */
    private boolean allPostingFilesExist(String postingPath, boolean stemming) {
        if (stemming && (new File(postingPath + "\\postingFilesWithStemming")).exists() &&
                (new File(postingPath + "\\languages")).exists() &&
                (new File(postingPath + "\\postingForCities")).exists() &&
                (new File(postingPath + "\\postingForCities\\cityDictionary")).exists() &&
                (new File(postingPath + "\\postingForCities\\mainCityPosting.txt")).exists() &&
                (new File(postingPath + "\\postingFilesWithStemming\\mainPosting.txt")).exists() &&
                (new File(postingPath + "\\postingFilesWithStemming\\termDictionary")).exists() &&
                (new File(postingPath + "\\postingFilesWithStemming\\termDictionaryForShow")).exists() &&
                (new File(postingPath + "\\postingFilesWithStemming\\documentDictionary")).exists() &&
                (new File(postingPath + "\\postingFilesWithStemming\\documentToEntitiesPosting.txt")).exists())
            return true;
        return !stemming && (new File(postingPath + "\\postingFilesWithoutStemming")).exists() &&
                (new File(postingPath + "\\languages")).exists() &&
                (new File(postingPath + "\\postingForCities")).exists() &&
                (new File(postingPath + "\\postingForCities\\cityDictionary")).exists() &&
                (new File(postingPath + "\\postingForCities\\mainCityPosting.txt")).exists() &&
                (new File(postingPath + "\\postingFilesWithoutStemming\\mainPosting.txt")).exists() &&
                (new File(postingPath + "\\postingFilesWithoutStemming\\termDictionary")).exists() &&
                (new File(postingPath + "\\postingFilesWithoutStemming\\termDictionaryForShow")).exists() &&
                (new File(postingPath + "\\postingFilesWithoutStemming\\documentDictionary")).exists() &&
                (new File(postingPath + "\\postingFilesWithoutStemming\\documentToEntitiesPosting.txt")).exists();
    }

    /**
     * shows all the relevant buttons and text areas for part 2
     */
    private void showPart2() {
        part2Text.setVisible(true);
        loadQueryText.setVisible(true);
        browseQueryButton.setVisible(true);
        showCitiesButton.setVisible(true);
        orText.setVisible(true);
        insertQueryText.setVisible(true);
        runButton.setVisible(true);
        queryPath.setVisible(true);
        semanticTreatmentCheckBox.setVisible(true);
    }

    /**
     * hides all the relevant buttons and text areas for part 2
     */
    private void hidePart2() {
        part2Text.setVisible(false);
        loadQueryText.setVisible(false);
        browseQueryButton.setVisible(false);
        showCitiesButton.setVisible(false);
        orText.setVisible(false);
        insertQueryText.setVisible(false);
        runButton.setVisible(false);
        queryPath.setVisible(false);
        semanticTreatmentCheckBox.setVisible(false);
        loadStopWordsButton.setVisible(false);
        stopWordsInstructionsText.setVisible(false);
    }

    /**
     * opens a new window with a choice box of languages from the corpus (if the corpus was indexer / loaded before)
     */
    public void handleLanguagesChoosing() {
        if (!startsIndexing && (alreadyIndexedWithStemming || alreadyIndexedWithoutStemming)) {
            Stage stage = new Stage();
            stage.setTitle("Available Languages");
            ChoiceBox<String> choiceBox = new ChoiceBox<>();
            if (languages == null || languages.size() == 0)
                languages = Indexer.readDictionaryForShowToMemory(postingPathText + "\\languages");
            choiceBox.getItems().setAll(FXCollections.observableList(languages));
            choiceBox.setMaxWidth(200);
            AnchorPane root = new AnchorPane();
            root.getChildren().addAll(choiceBox);
            AnchorPane.setBottomAnchor(choiceBox, 0.0);
            AnchorPane.setTopAnchor(choiceBox, 0.0);
            AnchorPane.setRightAnchor(choiceBox, 0.0);
            AnchorPane.setLeftAnchor(choiceBox, 0.0);
            root.prefWidth(200);
            root.prefWidth(200);
            Scene scene = new Scene(root, 200, 200);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.show();
        } else
            showErrorAlert("Need to index / load at least\nonce to choose a language!");
    }

    /**
     * checks if the load of the dictionaries has the same corpus details as the last load
     *
     * @param path     - the posting files path
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
            documentDictionary = new File(postingPathText + "\\postingFilesWithStemming\\documentDictionary");
        else
            documentDictionary = new File(postingPathText + "\\postingFilesWithoutStemming\\documentDictionary");
        ObjectInputStream objectInputStream;
        HashMap<String, int[]> documentDictionaryObject;
        try {
            objectInputStream = new ObjectInputStream(new FileInputStream(documentDictionary));
            documentDictionaryObject = (HashMap<String, int[]>) objectInputStream.readObject();
            objectInputStream.close();
            if (documentDictionaryObject.size() != Indexer.totalDocuments) {
                showErrorAlert("Document dictionary doesn't have the same document amount from last load / indexing!\n Hit reset in order to load a new dictionary!");
                return false;
            }
        } catch (IOException | ClassNotFoundException e) {
            showErrorAlert("Document dictionary doesn't exist from last load / indexing!\n Hit reset in order to load a new dictionary!");
            tempPostingPath = new TextField();
        }
        return true;
    }

    /**
     * chooses and sets a path for a given directory
     *
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

    /**
     * Pops an an error alert containing a given string as a message
     * @param error - a given string
     */
    private void showErrorAlert(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(error);
        alert.show();
    }

    /**
     * Loads the stop words file if the posting files were loaded
     */
    public void onLoadStopWords() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a path");
        File selectedFile = fileChooser.showOpenDialog(postingPath.getScene().getWindow());
        if (selectedFile != null && selectedFile.exists() && selectedFile.getName().equals("stop_words.txt")) {
            stopWordsPath = selectedFile.getAbsolutePath();
            loadStopWordsButton.setVisible(false);
            stopWordsInstructionsText.setVisible(false);
            showPart2();
        } else
            showErrorAlert("You must choose a valid stop words file with the name: \"stop_words.txt\"!");
    }

    /**
     * Loads and runs through a file with queries
     */
    public void onQueryLoad() {
        if (alreadyIndexedWithStemming || alreadyIndexedWithoutStemming) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose a query file");
            File selectedFile = fileChooser.showOpenDialog(postingPath.getScene().getWindow());
            if (selectedFile != null) {
                BufferedReader bufferedReader;
                try {
                    // read all the text from the queries file
                    bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(selectedFile)));
                    StringBuilder allQueries = new StringBuilder();
                    String line;
                    line = bufferedReader.readLine();
                    if (line != null)
                        allQueries.append(line);
                    while ((line = bufferedReader.readLine()) != null) {
                        allQueries.append("\n").append(line);
                    }
                    bufferedReader.close();
                    int queryStart = allQueries.indexOf("<top>");
                    getAndRunQueries(queryStart, allQueries);
//                path.setText(selectedDirectory.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else
            showErrorAlert("Need to load \\ index a data set before running a query!");
    }

    /**
     * goes through the text from a query file, gets the queries from it and runs it through the corpus
     *
     * @param queryStart - the index of the first query in the text
     * @param allQueries - a StringBuilder which contains the text of all the text in a query file
     */
    private void getAndRunQueries(int queryStart, StringBuilder allQueries) {
        LinkedList<String> queries = new LinkedList<>();
        queryResults = new HashMap<>();
        while (queryStart != -1) {
            int queryLimit = allQueries.indexOf("</top>", queryStart);

            int queryBeginning = allQueries.indexOf("<title>", queryStart);
            int queryEnd = allQueries.indexOf("<desc>", queryStart);

            int queryNumStart = allQueries.indexOf("<num>", queryStart);
            int queryNumEnd = allQueries.indexOf("<title>", queryBeginning);

            int queryDescStart = allQueries.indexOf("<desc>", queryStart);
            int queryDescEnd = allQueries.indexOf("<narr>", queryBeginning);

            // gets the query's id
            String queryNum = ((allQueries.substring(queryNumStart + 5, queryNumEnd).trim()).split(":"))[1].trim();

            // gets the query from the text
            String queryString = allQueries.substring(queryBeginning + 7, queryEnd).trim();

            // gets the description of the query
            String queryDescription = allQueries.substring(queryDescStart + 9, queryDescEnd).trim();

            queries.add("Query: " + queryString + "  Query Number: " + queryNum);

            // add the description to the query for better results
            queryString = queryString + " " + queryDescription;

            // runs the query through the corpus
            queryResults.put(queryString, runQuery(queryString));


            queryStart = allQueries.indexOf("<top>", queryLimit);
        }

        ObservableList<String> list = FXCollections.observableArrayList(queries);
        ListView<String> listView = new ListView<>(list);
        Stage stage = new Stage();
        stage.setTitle("Inserted Queries:");
        stage.initModality(Modality.APPLICATION_MODAL);
        StackPane pane = new StackPane();
        Scene scene = new Scene(pane, 600, 500);
        stage.setScene(scene);
        Button saveButton = new Button("Save Results");
        Button backButton = new Button("Back");
        Button demiButton1 = new Button("button");
        Button demiButton2 = new Button("button");
        Button demiButton3 = new Button("button");
        Button demiButton4 = new Button("button");
        demiButton1.setVisible(false);
        demiButton2.setVisible(false);
        demiButton3.setVisible(false);
        demiButton4.setVisible(false);
        backButton.setWrapText(true);
        saveButton.setWrapText(true);
        VBox vBox = new VBox();
        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.getChildren().addAll(demiButton1, demiButton2, demiButton3, backButton, saveButton);
        vBox.getChildren().addAll(listView, hBox);
        pane.getChildren().add(vBox);
        saveButton.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose a path");
            File selectedDirectory = directoryChooser.showDialog(stage);
            if (selectedDirectory == null)
                showErrorAlert("No directory was chosen!");
            else {
                File resultsFile;
                int id = 1;
                while ((new File(selectedDirectory.getAbsolutePath() + "\\loadQueryResults" + id + ".txt")).exists())
                    id++;
                resultsFile = new File(selectedDirectory.getAbsolutePath() + "\\loadQueryResults" + id + ".txt");
                try {
                    resultsFile.createNewFile();
                    BufferedWriter toResultsFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultsFile)));
                    for (String queryLine : queries) {
                        int queryNumberIndex = queryLine.indexOf("Query Number: ");
                        int queryIndex = queryLine.indexOf("Query: ");
                        String query = queryLine.substring(queryIndex + 7, queryNumberIndex - 2);
                        String queryNumber = queryLine.substring(queryNumberIndex + 14);
                        Queue<String> relevantDocuments = queryResults.get(query);
                        for (String docNumber : relevantDocuments) {
                            toResultsFile.write(queryNumber + " 0 " + docNumber + " 0  0 id\n");
                        }
                    }
                    toResultsFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        backButton.setOnAction(event -> stage.close());
        listView.setCellFactory(param -> new queriesCell());
        stage.show();
    }

    /**
     * shows a chosen query's results
     *
     * @param lastItem - the query and query number line
     */
    private void showQuery(String lastItem) {
        int queryNumberIndex = lastItem.indexOf("Query Number: ");
        int queryIndex = lastItem.indexOf("Query: ");
        String query = lastItem.substring(queryIndex + 7, queryNumberIndex - 2);

        Queue<String> relevantDocuments = queryResults.get(query);

        if (relevantDocuments.isEmpty()) {
            showErrorAlert("No relevant documents found for the query: " + query);
        } else {
            ObservableList<String> list = FXCollections.observableArrayList(relevantDocuments);
            ListView<String> listView = new ListView<>(list);
            Stage stage = new Stage();
            stage.setTitle("Results for the query: " + query);
            stage.initModality(Modality.APPLICATION_MODAL);
            StackPane pane = new StackPane();
            Scene scene = new Scene(pane, 600, 500);
            stage.setScene(scene);
            Button backButton = new Button("Back");
            Button demiButton1 = new Button("button");
            Button demiButton2 = new Button("button");
            Button demiButton3 = new Button("button");
            Button demiButton4 = new Button("button");
            demiButton1.setVisible(false);
            demiButton2.setVisible(false);
            demiButton3.setVisible(false);
            demiButton4.setVisible(false);
            backButton.setWrapText(true);
            VBox vBox = new VBox();
            HBox hBox = new HBox();
            hBox.setSpacing(11);
            hBox.getChildren().addAll(demiButton1, demiButton2, demiButton3, backButton);
            vBox.getChildren().addAll(listView, hBox);
            pane.getChildren().add(vBox);
            backButton.setOnAction(event -> stage.close());
            listView.setCellFactory(param -> new entitiesCell());
            stage.show();
        }
    }

    /**
     * Runs a given query
     */
    public void onQueryRun() {
        if (alreadyIndexedWithStemming || alreadyIndexedWithoutStemming) {
            if (queryPath.getText().equals(""))
                showErrorAlert("You must fill a query in order to run it!");
            else {
                // gets the relevant documents for the query
                Queue<String> relevantDocuments = runQuery(queryPath.getText());

                // makes sure there are any relevant documents for the given query
                if (relevantDocuments.isEmpty()) {
                    showErrorAlert("No relevant documents found for the query: " + queryPath.getText());
                } else {
                    ObservableList<String> list = FXCollections.observableArrayList(relevantDocuments);
                    ListView<String> listView = new ListView<>(list);
                    Stage stage = new Stage();
                    stage.setTitle("Results for the query: " + queryPath.getText());
                    stage.initModality(Modality.APPLICATION_MODAL);
                    StackPane pane = new StackPane();
                    Scene scene = new Scene(pane, 600, 500);
                    stage.setScene(scene);
                    Button saveButton = new Button("Save Results");
                    Button backButton = new Button("Back");
                    Button demiButton1 = new Button("button");
                    Button demiButton2 = new Button("button");
                    Button demiButton3 = new Button("button");
                    Button demiButton4 = new Button("button");
                    demiButton1.setVisible(false);
                    demiButton2.setVisible(false);
                    demiButton3.setVisible(false);
                    demiButton4.setVisible(false);
                    backButton.setWrapText(true);
                    saveButton.setWrapText(true);
                    VBox vBox = new VBox();
                    HBox hBox = new HBox();
                    hBox.setSpacing(10);
                    hBox.getChildren().addAll(demiButton1, demiButton2, demiButton3, backButton, saveButton);
                    vBox.getChildren().addAll(listView, hBox);
                    pane.getChildren().add(vBox);
                    saveButton.setOnAction(event -> {
                        DirectoryChooser directoryChooser = new DirectoryChooser();
                        directoryChooser.setTitle("Choose a path");
                        File selectedDirectory = directoryChooser.showDialog(stage);
                        if (selectedDirectory == null)
                            showErrorAlert("No directory was chosen!");
                        else {
                            File resultsFile;
                            int id = 1;
                            while ((new File(selectedDirectory.getAbsolutePath() + "\\runQueryResults" + id + ".txt")).exists())
                                id++;
                            resultsFile = new File(selectedDirectory.getAbsolutePath() + "\\runQueryResults" + id + ".txt");
                            try {
                                resultsFile.createNewFile();
                                BufferedWriter toResultsFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultsFile)));
                                for (String docNumber : list) {
                                    toResultsFile.write("666" + " 0 " + docNumber + " 1  40 mt\n");
                                }
                                toResultsFile.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    backButton.setOnAction(event -> stage.close());
                    listView.setCellFactory(param -> new entitiesCell());
                    stage.show();
                }
            }
        } else
            showErrorAlert("Need to load \\ index a data set before running a query!");
    }

    /**
     * Shows all the corpus's cities and gets the choice\s of the use
     */
    public void onShowAndChooseCities() {
        // check if there are any cities in the corpus
        if (Indexer.corpusCityDictionary.size() > 0) {
            citiesToFilter = new HashMap<>();
            ObservableList<String> citiesToShow = FXCollections.observableArrayList();
            Set<String> cities = Indexer.corpusCityDictionary.keySet();
            ListView<String> listView = new ListView<>();
            listView.setItems(citiesToShow);

            Label instructions = new Label("Hold the control button while left clicking your choices for choosing cities, and than click \"Confirm Selection\"");
            instructions.setWrapText(true);
            Button confirmSelection = new Button("Confirm Selection");
            Button demiButton1 = new Button("button");
            Button demiButton2 = new Button("button");
            Button demiButton3 = new Button("button");
            Button demiButton4 = new Button("button");
            Button demiButton5 = new Button("button");
            Button demiButton6 = new Button("button");
            demiButton1.setVisible(false);
            demiButton2.setVisible(false);
            demiButton3.setVisible(false);
            demiButton4.setVisible(false);
            demiButton5.setVisible(false);
            demiButton6.setVisible(false);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);

            confirmSelection.setOnAction(event -> {
                List<String> selected = listView.getSelectionModel().getSelectedItems();
                for (String selectedCity : selected) {
                    citiesToFilter.put(selectedCity, 1);
                }
                stage.close();
            });

            // Create the VBox for the Buttons
            VBox buttons = new VBox();
            // Add the Buttons to the VBox
            buttons.getChildren().addAll(demiButton1, demiButton2, demiButton3, demiButton4, demiButton5, instructions, demiButton6, confirmSelection);
            // Create the Selection HBox
            HBox selection = new HBox();
            // Set Spacing to 10 pixels
            selection.setSpacing(10);
            // Add the List and the Buttons to the HBox
            selection.getChildren().addAll(listView, buttons);
            // Create the GridPane
            GridPane root = new GridPane();
            // Set the horizontal and vertical gaps between children
            root.setHgap(10);
            root.setVgap(5);
            // Add the HBox to the GridPane at position 0
            root.addColumn(0, selection);
            // Add the Buttons to the GridPane at position 1
            root.addColumn(1, buttons);

            listView.setPrefHeight(800);
            listView.setPrefWidth(605);
            // add all the cities to the ListView object
            for (String city : cities) {
                citiesToShow.add(city);
            }

            listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            Scene scene = new Scene(root, 805, 500);
            stage.setScene(scene);
            stage.show();
        } else
            showErrorAlert("No cities found in the corpus!");
    }

    /**
     * Runs a given query through the corpus
     *
     * @param query - a given query
     * @return - a hashmap with the query and a queue, sorted by rank, of retrieved document numbers according to the given query
     */
    private Queue<String> runQuery(String query) {
        Searcher searcher = new Searcher(query, stopWordsPath, semanticTreatmentCheckBox.isSelected());
        return searcher.processQuery();
    }

    /**
     * Shows the 5 most dominant entities in a document
     *
     * @param document - the document
     */
    private void showEntities(String document) {
        RandomAccessFile ram;
        try {
            if (Indexer.isDictionaryStemmed)
                ram = new RandomAccessFile(postingPathText + "\\postingFilesWithStemming\\documentToEntitiesPosting.txt", "r");
            else
                ram = new RandomAccessFile(postingPathText + "\\postingFilesWithoutStemming\\documentToEntitiesPosting.txt", "r");
            int postingLinePointer = Integer.valueOf(Indexer.documentDictionary.get(document)[5]);
            ram.seek(postingLinePointer);
            String postingLine = ram.readLine();
            ram.close();
            Queue<String> entitiesList = getEntitiesFromPostingLine(postingLine, document);
            if (entitiesList.isEmpty())
                showErrorAlert("No entities found in this document!");
            else {
                ObservableList<String> entities = FXCollections.observableArrayList(entitiesList);
                ListView<String> listView = new ListView<>(entities);
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                Button backButton = new Button("Back");
                Button demiButton1 = new Button("button");
                Button demiButton2 = new Button("button");
                Button demiButton3 = new Button("button");
                Button demiButton4 = new Button("button");
                demiButton1.setVisible(false);
                demiButton2.setVisible(false);
                demiButton3.setVisible(false);
                demiButton4.setVisible(false);
                backButton.setWrapText(true);
                VBox vBox = new VBox();
                HBox hBox = new HBox();
                hBox.setSpacing(10);
                hBox.getChildren().addAll(demiButton1, demiButton2, demiButton3, backButton);
                vBox.getChildren().addAll(listView, hBox);
                backButton.setOnAction(event -> stage.close());
                StackPane pane = new StackPane();
                Scene scene = new Scene(pane, 600, 500);
                stage.setScene(scene);
                pane.getChildren().add(vBox);
                stage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sorts and gets all the entities of a document
     *
     * @param postingLine - the posting line containing the entities of a dcoument
     * @param document    - the document
     * @return - a list of the entities of a document
     */
    private Queue<String> getEntitiesFromPostingLine(String postingLine, String document) {
        PriorityQueue<String[]> entitiesSortingQueue = new PriorityQueue<>((o1, o2) -> {
            double value1 = Double.valueOf(o1[1]);
            double value2 = Double.valueOf(o2[1]);
            return Double.compare(value2, value1);
        });

        // calculating the dominance for each entity and adding it to the priority queue
        RandomAccessFile ram;
        try {
            if (Indexer.isDictionaryStemmed)
                ram = new RandomAccessFile(postingPathText + "\\postingFilesWithStemming\\mainPosting.txt", "r");
            else
                ram = new RandomAccessFile(postingPathText + "\\postingFilesWithoutStemming\\mainPosting.txt", "r");
            // going through all the entities of the document and filtering the unwanted ones
            String[] entities = (postingLine.split(":")[1]).split(";");
            for (String entity : entities) {
                // filtering the unwanted entities
                if (Indexer.termDictionary.containsKey(entity)) {
                    String[] entityArr = new String[2];
                    entityArr[0] = entity;
                    int postingLinePointer = Indexer.termDictionary.get(entity)[2];
                    ram.seek(postingLinePointer);
                    String mainPostingLine = ram.readLine();
                    String[] mainPostingSplit = (mainPostingLine.split(":")[1]).split(";");
                    // searching for the tf of the entity in the document
                    for (String entry : mainPostingSplit) {
                        String[] entrySplit = entry.split(",");
                        if (entrySplit[0].equals(document)) {
                            double tf = (Double.valueOf(entrySplit[1])) / Double.valueOf(Indexer.documentDictionary.get(document)[0]);
                            double N = Indexer.totalDocuments;
                            double maxTf = Indexer.termDictionary.get(entity)[0];
                            double idf = Math.log10(N / maxTf);
                            double tfIdf = tf * idf;
                            double title = 0;
                            double tenPercent = 0;
                            if (entrySplit[2].charAt(1) == '1')
                                title = 1;
                            if (entrySplit[2].charAt(2) == '1')
                                tenPercent = 1;
                            // the final calculation for the dominance of the entity
                            double totalValue = (0.85 * tfIdf) + (0.1 * title) + (0.05 * tenPercent);
                            String totalValueOfString = String.valueOf(totalValue);
                            entityArr[1] = totalValueOfString;
                            break;
                        }
                    }
                    entitiesSortingQueue.add(entityArr);
                }
            }
            ram.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Queue<String> entitiesList = new LinkedList<>();

        // creating a normal queue for the 5 most dominant entities
        int i = 0;
        while (!entitiesSortingQueue.isEmpty() && i < 5) {
            String[] entity = entitiesSortingQueue.poll();
            String entityRank;
            if (entity != null) {
                entityRank = entity[1];
                if (entityRank.length() > 8) {
                    entityRank = entityRank.substring(0, 8);
                }
                entitiesList.add("The entity \"" + entity[0] + "\" has a dominance value of " + entityRank);
                i++;
            }
        }
        return entitiesList;
    }

    /**
     * A class for showing each doc number relevant to the query, with its ranking grade and a button to show its entities
     * The code for the class was found at: https://stackoverflow.com/questions/15661500/javafx-listview-item-with-an-image-button
     */
    class entitiesCell extends ListCell<String> {
        HBox hbox = new HBox();
        Label label = new Label("(empty)");
        Pane pane = new Pane();
        Button button = new Button("Identify Entities");

        String lastItem;

        entitiesCell() {
            super();
            hbox.getChildren().addAll(label, pane, button);
            HBox.setHgrow(pane, Priority.ALWAYS);
            button.setOnAction(event -> showEntities(lastItem));
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            if (empty) {
                lastItem = null;
                setGraphic(null);
            } else {
                lastItem = item;
                label.setText(item != null ? item : "<null>");
                setGraphic(hbox);
            }
        }
    }

    /**
     * A class for showing each doc number relevant to the query, with its ranking grade and a button to show its entities
     * The code for the class was found at: https://stackoverflow.com/questions/15661500/javafx-listview-item-with-an-image-button
     */
    class queriesCell extends ListCell<String> {
        HBox hbox = new HBox();
        Label label = new Label("(empty)");
        Pane pane = new Pane();
        Button button = new Button("Show Results");

        String lastItem;

        queriesCell() {
            super();
            hbox.getChildren().addAll(label, pane, button);
            HBox.setHgrow(pane, Priority.ALWAYS);
            button.setOnAction(event -> showQuery(lastItem));
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            if (empty) {
                lastItem = null;
                setGraphic(null);
            } else {
                lastItem = item;
                label.setText(item != null ? item : "<null>");
                setGraphic(hbox);
            }
        }
    }
}