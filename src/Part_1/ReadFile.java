package Part_1;

import javafx.scene.control.Alert;
import java.io.*;

/**
 * This class reads all the corpus's files and parses through them
 */

public class ReadFile implements Runnable {

    private String dirPath;
    private StringBuilder allDocumentLines;
    public static int docCount = 0; // TODO: maybe erase docCount

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
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(tempFiles[0])));
                            String line;
                            allDocumentLines = new StringBuilder();
                            while ((line = bufferedReader.readLine()) != null) {
                                allDocumentLines.append(line);
                            }
                            bufferedReader.close();
                            int docStart = allDocumentLines.indexOf("<DOC>");
                            parseThroughDoc(docStart);
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            System.out.println("Number of document files: " + subDirs.length); // TODO: erase tracking
            System.out.println("Number of documents: " + docCount); // TODO: erase tracking
            // inform the parse class it shouldn't wait for any more documents to parse through
            Parse.stop();
        }
        else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("You must choose an existing corpus folder path!");
            alert.show();
        }
    }

    /**
     * parses through each document and finds its ID, all its text tags (could be more than 1),
     * its title (if exists), its date (if exists), and its city (if exists)
     * @param docStart - a given document's start index
     */
    private void parseThroughDoc(int docStart) {
        // checks if there are any more document's to fetch from the file
        while (docStart != -1) {
            docCount++;
            String docString = "";
            String docNumber = "";
            String docText = "";
            String docTitle = "";
            String docDate = "";
            String docCity = "";

            int docEnd = allDocumentLines.indexOf("</DOC>", docStart);
            docString = allDocumentLines.substring(docStart + 5,docEnd);

            int docNumberStart = docString.indexOf("<DOCNO>");
            int docNumberEnd = docString.indexOf("</DOCNO>");

            // gets the document's number
            docNumber = (docString.substring(docNumberStart + 7, docNumberEnd)).trim();

//            System.out.println(docNumber);

            GeneralClasses.Document newDoc = new GeneralClasses.Document(docNumber);

            // gets the contents of the document's title (if there is one)
            if (docString.contains("<TI>")) {
                int titleStart = docString.indexOf("<TI>");
                int titleEnd = docString.indexOf("</TI>");
                docTitle = (docString.substring(titleStart + 4, titleEnd)).trim();
                if (!docTitle.equals(""))
                    newDoc.setDocTitle(docTitle);
            }

            // gets the document's date (if there is one)
            if (docString.contains("<DATE1>")) {
                int dateStart = docString.indexOf("<DATE1>");
                int dateEnd = docString.indexOf("</DATE1>");
                docDate = (docString.substring(dateStart + 7, dateEnd)).trim();
            }
            else {
                if (docString.contains("<DATE>")) {
                    int dateStart = docString.indexOf("<DATE>");
                    int dateEnd = docString.indexOf("</DATE>");
                    docDate = (docString.substring(dateStart + 6, dateEnd)).trim();
                }
            }

            if (!docDate.equals(""))
                newDoc.setDocDate(docDate);

            // gets the document's city (if there is one)s
            if (docString.contains("<F P=104>")) {
                int cityStart = docString.indexOf("<F P=104>");
                int cityEnd = docString.indexOf("</F>",cityStart);
                String[] docCityArr = (docString.substring(cityStart + 9, cityEnd)).split("[\\s]+");
                for (String city : docCityArr) {
                    if (!city.equals("")) {
                        docCity = city.toUpperCase();
                        newDoc.setCity(docCity);
                        break;
                    }
                }
            }

            // gets the document's <TEXT></TEXT> tags
            if (docString.contains("<TEXT>")) {
                int textStart = docString.indexOf("<TEXT>");
                while (textStart != -1) {
                    int textEnd = docString.indexOf("</TEXT>");
                    docText = docString.substring(textStart + 6, textEnd);
                    if (docText.contains("<F P=106>")) {
                        int tempDocStart = docText.indexOf("[Text]");
                        docText = docText.substring(tempDocStart + 6);
                    }
                    textStart = docString.indexOf("<TEXT>", textEnd);
                    if (!docText.equals(""))
                        newDoc.addDocText(docText);
                }
            }

            // add the document to the static Document Queue in the Parse class
            try {
                Parse.docQueue.put(newDoc);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // gets the next document's start index
            docStart = allDocumentLines.indexOf("<DOC>", docEnd);
        }
    }

    @Override
    public void run() {
        readThroughFiles();
    }

    public static void main(String[] args) {
        String path = "C:\\Users\\Shalev\\Desktop";
        ReadFile readFile = new ReadFile(path);
        long startTime = System.nanoTime();
        readFile.readThroughFiles();
        System.out.println();
        Parse parse = new Parse(path + "\\stop words");
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
        finally {
            System.out.println("Time to read all files: " + (System.nanoTime() - startTime)*Math.pow(10,-9));
        }
    }
}