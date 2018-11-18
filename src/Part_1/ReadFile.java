package Part_1;

import javafx.scene.control.Alert;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.nio.file.Files;

/**
 * This class reads all the corpus's files and parses through them
 */

public class ReadFile implements Runnable {

    private String dirPath;

    /**
     * A constructor for the ReadFile class
     * @param dirPath - the directory path in which the corpus is found
     */
    public ReadFile(String dirPath) {
        this.dirPath = dirPath;
    }

    /**
     * Reads and parses through all the corpuse's files
     */
    private void readThroughFiles() {
        // get to the corpus directory
        File dir = new File(dirPath + "\\corpus");
        if (dir.exists()) {
            // get to all the corpus's sub-directories
            File[] subDirs = dir.listFiles();
            if (subDirs != null) {
                for (File f : subDirs) {
                    // get to the file inside the corpus's sub-directory
                    File[] tempFiles = f.listFiles();
                    if (tempFiles != null) {
                        try {
                            // parse through the file and separate all the documents that are in it
                            Document file = Jsoup.parse(new String(Files.readAllBytes(tempFiles[0].toPath())));
                            Elements docs = file.getElementsByTag("DOC");
                            for (Element doc : docs) {
                                /* parse through each document and find its ID, all its text tags (could be more than 1),
                                   its title (if exists), and its date (if exists) */
                                Document document = Jsoup.parse(doc.text());
                                Elements docNumber = document.getElementsByTag("DOCNO");
                                Elements docTexts = document.getElementsByTag("TEXT");
                                Elements docTitle = document.getElementsByTag("TI");
                                Elements docDate = document.getElementsByTag("DATE1");
                                GeneralClasses.Document newDoc = new GeneralClasses.Document(docNumber.text());
                                String documentTitle = docTitle.text();
                                String documentDate = docDate.text();
                                if (!documentTitle.equals(""))
                                    newDoc.setDocTitle(documentTitle);
                                if (!documentDate.equals(""))
                                    newDoc.setDocDate(documentDate);
                                for (Element docText:docTexts) {
                                    String documentText = docText.text();
                                    newDoc.addDocText(documentText);
                                }
                                // add the document to the static Document Queue in the Parse class
                                Parse.docQueue.add(newDoc);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            // inform the parse class it shouldn't wait for any more documents to parse through
            Parse.stop();
        }
        else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("You must choose an existing corpus folder path!");
            alert.show();
        }
    }

    @Override
    public void run() {
        readThroughFiles();
    }
}