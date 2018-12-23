package Controller;

import Part_1.Indexer;
import Part_1.Parse;
import Part_1.ReadFile;
import Part_2.Searcher;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
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
    public static String postingPathText;
    private boolean startsIndexing = false;
    private boolean alreadyIndexedWithStemming = false;
    private boolean alreadyIndexedWithoutStemming = false;
    private TextField tempPostingPath = new TextField();
    public static List<String> languages = new LinkedList<>();
    public static HashMap<String,Integer> citiesToFilter = new HashMap<>();

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
        }
        else
            if(alreadyIndexedAll())
                showErrorAlert("Already indexed / loaded all options!!\n(stemming / non stemming)");
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
            if (dir != null)
                for (File dir2 : dir.listFiles()) {
                    if (dir2.listFiles() != null) {
                        for (File f : dir2.listFiles())
                            f.delete();
                    }
                    dir2.delete();
                }
            alreadyIndexedWithStemming = false;
            alreadyIndexedWithoutStemming = false;
            languages = new LinkedList<>();
            Parse.resetAll();
            Indexer.resetAll();
            tempPostingPath = new TextField();
            hidePart2();
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
                List<String> dictionaryList;
                if (Indexer.isDictionaryStemmed)
                    dictionaryList = Indexer.readDictionaryForShowToMemory(postingPathText + "\\postingFilesWithStemming\\termDictionaryForShow");
                else
                    dictionaryList = Indexer.readDictionaryForShowToMemory(postingPathText + "\\postingFilesWithoutStemming\\termDictionaryForShow");
                ListView<String> listView = new ListView<>();
                listView.getItems().setAll(FXCollections.observableList(dictionaryList));
                listView.setEditable(false);
                listView.prefWidth(805);
                listView.prefHeight(500);
                AnchorPane root = new AnchorPane();
                root.getChildren().addAll(listView);
                AnchorPane.setBottomAnchor(listView, 0.0);
                AnchorPane.setTopAnchor(listView, 0.0);
                AnchorPane.setRightAnchor(listView, 0.0);
                AnchorPane.setLeftAnchor(listView, 0.0);
                root.prefWidth(805);
                root.prefWidth(500);
                Scene scene = new Scene(root, 805, 500);
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setScene(scene);
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
            if ((stemming && alreadyIndexedWithStemming) || (!stemming && alreadyIndexedWithoutStemming))
                showErrorAlert("Already loaded / indexed this option!");
            else {
                chooseAndSaveDirectoryPath(tempPostingPath);
                if (!tempPostingPath.getText().equals("")) {
                    boolean tempWithStemming = alreadyIndexedWithStemming;
                    boolean tempWithoutStemming = alreadyIndexedWithoutStemming;
                    try {
                        boolean loadCityDictionary = true;
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
                        showPart2();
                    } catch (IOException | ClassNotFoundException e) {
                        showErrorAlert("Not all dictionary files found in path! Try again");
                        alreadyIndexedWithStemming = tempWithStemming;
                        alreadyIndexedWithoutStemming = tempWithoutStemming;
                        tempPostingPath = new TextField();
                    }
                }
            }
        }
        else
        if(alreadyIndexedAll())
            showErrorAlert("Already indexed / loaded all options!!\n(stemming / non stemming)");
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
    }

    /**
     * opens a new window with a choice box of languages from the corpus (if the corpus was indexer / loaded before)
     * @param actionEvent - unused
     */
    public void handleLanguagesChoosing(ActionEvent actionEvent) {
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
        }
        else
            showErrorAlert("Need to index / load at least\nonce to choose a language!");
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
        HashMap<String,int[]> documentDictionaryObject;
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

    /**
     * Loads and runs through a file with queries
     * @param actionEvent - unused
     */
    public void onQueryLoad(ActionEvent actionEvent) {
        if (alreadyIndexedWithStemming || alreadyIndexedWithoutStemming) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose a query file");
            File selectedFile = fileChooser.showOpenDialog(postingPath.getScene().getWindow());
            if (selectedFile != null) {
                BufferedReader bufferedReader = null;
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
        }
        else
            showErrorAlert("Need to load \\ index a data set before running a query!");
    }

    /**
     * goes through the text from a query file, gets the queries from it and runs it through the corpus
     * @param queryStart - the index of the first query in the text
     * @param allQueries - a StringBuilder which contains the text of all the text in a query file
     */
    private void getAndRunQueries(int queryStart, StringBuilder allQueries) {
        while (queryStart != -1) {
            int queryLimit = allQueries.indexOf("</top>", queryStart);

            int queryBeginning = allQueries.indexOf("<title>", queryStart);
            int queryEnd = allQueries.indexOf("<desc>", queryStart);

            int queryNumStart = allQueries.indexOf("<num>", queryStart);
            int queryNumEnd = allQueries.indexOf("<title>", queryNumStart);

            // gets the query's id
            String queryNum = ((allQueries.substring(queryNumStart + 5, queryNumEnd).trim()).split(":"))[1].trim();

            // gets the query from the text
            String queryString = allQueries.substring(queryBeginning + 7,queryEnd).trim();

            // runs the query through the corpus
            runQuery(queryString, queryNum);

            queryStart = allQueries.indexOf("<title>", queryLimit);
        }
    }

    /**
     * Runs a given query
     * @param actionEvent - unused
     */
    public void onQueryRun(ActionEvent actionEvent) {
        if (alreadyIndexedWithStemming || alreadyIndexedWithoutStemming) {
            if (queryPath.getText().equals(""))
                showErrorAlert("You must fill a query in order to run it!");
            else {
                runQuery(queryPath.getText(), null);
            }
        }
        else
            showErrorAlert("Need to load \\ index a data set before running a query!");
    }

    /**
     * Shows all the corpus's cities and gets the choice\s of the use
     * @param actionEvent - unused
     */
    public void onShowAndChooseCities(ActionEvent actionEvent) {
        // check if there are any cities in the corpus
        if (Indexer.corpusCityDictionary.size() > 0) {
            ObservableList<String> citiesToShow = FXCollections.observableArrayList();
            Set<String> cities = Indexer.corpusCityDictionary.keySet();
            ListView<String> listView = new ListView<String>();
            listView.setItems(citiesToShow);

            // add all the cities to the ListView object
            for (String city : cities) {
                citiesToShow.add(city);
            }

            listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            listView.setOnMouseClicked(new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {
                    ObservableList<String> selectedCities = listView.getSelectionModel().getSelectedItems();

                    for (String selectedCity : selectedCities)
                        citiesToFilter.put(selectedCity,1);
                }
            });

            Stage stage = new Stage();
            Pane root = new Pane();
            Scene scene = new Scene(root, 805,500);
            root.getChildren().add(listView);
            stage.setScene(scene);
            stage.show();
        }
        else
            showErrorAlert("No cities found in the corpus!");
    }

    /**
     * Runs a given query through the corpus
     * @param query - a given query
     * @param queryId - a given query's id
     */
    private void runQuery(String query, String queryId) {
        Searcher searcher = new Searcher(query);
        Queue<String> relevantDocs = searcher.processQuery();

        // makes sure there are any relevant documents for the given query
        if (relevantDocs.isEmpty())
            showErrorAlert("No relevant documents found for the query: " + query);
        else {
            ObservableList<String> list = FXCollections.observableArrayList(relevantDocs);
            ListView<String> listView = new ListView<>(list);
            Stage stage = new Stage();
            StackPane pane = new StackPane();
            Scene scene = new Scene(pane, 600, 500);
            stage.setScene(scene);
            pane.getChildren().add(listView);
            Button saveResultsButton = new Button("Save Results");
            saveResultsButton.setWrapText(true);
            saveResultsButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Choose a path");
                    File selectedDirectory = directoryChooser.showDialog(stage);
                    if (selectedDirectory != null)
                        showErrorAlert("No Directory was chose, so the results were not saved!");
                    else {
                        File resultsFile;
                        if (queryId == null) {
                            int id = 1;
                            while ((new File(selectedDirectory.getAbsolutePath() + "\\" + id)).exists())
                                id++;
                            resultsFile = new File(selectedDirectory.getAbsolutePath() + "\\" + id);
                        }
                        else
                            resultsFile = new File(selectedDirectory.getAbsolutePath() + "\\" + queryId);
                        try {
                            resultsFile.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            listView.setCellFactory(param -> new Cell());
            stage.show(); // TODO : MAKE SURE THAT FOR A QUERY FILE INPUT, IT WAITS UNTIL WINDOW CLOSES FOR NEXT QUERY
        }
    }

    /**
     * Shows the 5 most dominant entities in a document
     * @param document - the document
     */
    private void showEntities(String document) {
        List<String> entitiesList = new LinkedList<>(); // TODO : NEED REPLACE WITH THE ACTUAL ENTITIES OF THE GIVEN DOCUMENT
        ObservableList<String> entities = FXCollections.observableArrayList(entitiesList);
        ListView<String> listView = new ListView<>(entities);
        Stage stage = new Stage();
        StackPane pane = new StackPane();
        Scene scene = new Scene(pane, 600, 500);
        stage.setScene(scene);
        pane.getChildren().add(listView);
        stage.show();
    }

    /**
     * A class for showing each doc number relevant to the query, with its ranking grade and a button to show its entities
     * The code for the class was found at: https://stackoverflow.com/questions/15661500/javafx-listview-item-with-an-image-button
     */
    class Cell extends ListCell<String> {
        HBox hbox = new HBox();
        Label label = new Label("(empty)");
        Pane pane = new Pane();
        Button button = new Button("Identify Entities");

        String lastItem;

        Cell() {
            super();
            hbox.getChildren().addAll(label, pane, button);
            HBox.setHgrow(pane, Priority.ALWAYS);
            button.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    showEntities(lastItem.split(" ")[0]);
                }
            });
        }
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);  // No text in label of super class
            if (empty) {
                lastItem = null;
                setGraphic(null);
            } else {
                lastItem = item;
                label.setText(item!=null ? item : "<null>");
                setGraphic(hbox);
            }
        }

    }
}