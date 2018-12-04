package Part_1;

import javafx.scene.control.Alert;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class reads all the corpus's files and parses through them
 */

public class ReadFile implements Runnable {

    private String dirPath;
    private StringBuilder allDocumentLines;

    /**
     * A construct  or for the ReadFile class
     * @param dirPath - the directory path in which the corpus is found
     */
    public ReadFile(String dirPath) {
        this.dirPath = dirPath;
    }

    /**
     * Reads and parses through all the corpus's files
     */
    private void readThroughFiles() {
        // get to the corpus directory
        File dir = new File(dirPath + "\\corpus");
        if (dir.exists()) {
            Parse.resetPartially();
            Indexer.resetPartially();
            // go once through the corpus to get all the city's in the tag <F P=104>
            if (!Indexer.indexedCities)
                getCityDictionary(dir);
            // get to all the corpus's sub-directories
            File[] subDirs = dir.listFiles();
            if (subDirs != null) {
                // if this is the second indexing run (stemming / not stemming), makes sure the stop variables are false.
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
     * go once through all the corpus in order to get information about all the cities in
     * the tag <F P=104></F>
     */
    private void getCityDictionary(File dir) {
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
                        parseThroughDocsForCities(docStart);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * parses through the documents to locate the tag <F P=104></F> if exists
     * @param docStart - the firs document's start indexs
     */
    private void parseThroughDocsForCities(int docStart) {
        // checks if there are any more document's to fetch from the file
        while (docStart != -1) {

            int docEnd = allDocumentLines.indexOf("</DOC>", docStart);
            String docString = allDocumentLines.substring(docStart + 5,docEnd);

            // gets the document's city (if there is one)
            if (docString.contains("<F P=104>")) {
                int cityStart = docString.indexOf("<F P=104>");
                int cityEnd = docString.indexOf("</F>", cityStart);
                String[] docCityArr = (docString.substring(cityStart + 9, cityEnd)).split("[\\s]+");
                if (docCityArr.length != 0) {
                    String city = docCityArr[0];
                    int counter = 0;
                    while (city.equals("") && counter + 1<docCityArr.length){
                        counter++;
                        city =docCityArr[counter];
                    }
                    if (!city.equals("") && isOnlyLetters(city)) {
                        String cityUpper = city.toUpperCase();
                        // checks if we already added this city to the dictionary
                        if (!Indexer.corpusCityDictionary.containsKey(cityUpper)) {
                            addToCityDictionary(cityUpper);
                        }
                        else {
                            int[] corpusTermData = Indexer.termDictionary.get(cityUpper);
                            corpusTermData[0] = corpusTermData[0] + 1;
                            corpusTermData[1] = corpusTermData[1] + 1;
                        }
                    }
                }
            }
            // gets the next document's start index
            docStart = allDocumentLines.indexOf("<DOC>", docEnd);
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

            // gets the document's city (if there is one)
            if (docString.contains("<F P=104>")) {
                int cityStart = docString.indexOf("<F P=104>");
                int cityEnd = docString.indexOf("</F>", cityStart);
                String[] docCityArr = (docString.substring(cityStart + 9, cityEnd)).split("[\\s]+");
                if (docCityArr.length != 0) {
                    docCity = docCityArr[0];
                    int counter = 0;
                    while (docCity.equals("") && counter + 1 < docCityArr.length){
                        counter++;
                        docCity = docCityArr[counter];
                    }
                    if (!docCity.equals("") && isOnlyLetters(docCity)) {
                        String cityUpper = docCity.toUpperCase();
                        newDoc.setCity(cityUpper);
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

    /**
     * adds the city's details to the city dictionary
     * @param city - a given string of a city
     */
    private void addToCityDictionary(String city) {
        URL url;
        try {
            url = new URL("http://getcitydetails.geobytes.com/GetCityDetails?fqcn=" + city);

            //make connection
            URLConnection urlc = url.openConnection();

            //use post mode
            urlc.setDoOutput(true);
            urlc.setAllowUserInteraction(false);

            //get result
            BufferedReader br = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
            String l = br.readLine();
            if (l != null) {
                int countryStartIndex = l.indexOf("geobytescountry") + 18;
                int countryFinishIndex = l.indexOf("\"",countryStartIndex);
                String country = "";
                if (countryFinishIndex - countryStartIndex != 0)
                    country = l.substring(countryStartIndex, countryFinishIndex);
                int coinStartIndex = l.indexOf("geobytescurrencycode") + 23;
                int coinFinishIndex = l.indexOf("\"",coinStartIndex);
                String coin = "";
                if (coinFinishIndex - coinStartIndex != 0)
                    coin = l.substring(coinStartIndex, coinFinishIndex);
                int populationStartIndex = l.indexOf("geobytespopulation") + 21;
                int populationFinishIndex = l.indexOf("\"",populationStartIndex);
                String population = "";
                if (populationFinishIndex - populationStartIndex != 0)
                    population = l.substring(populationStartIndex, populationFinishIndex);
                String[] cityData = new String[4];
                cityData[0] = country;
                cityData[1] = coin;
                cityData[2] = population;

                Indexer.corpusCityDictionary.put(city, cityData);
                int[] corpusTermData = new int[3];
                corpusTermData[0] = 1;
                corpusTermData[1] = 1;
                Indexer.termDictionary.put(city, corpusTermData);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * checks if a given word is composed only of letters or not
     * taken from https://stackoverflow.com/questions/5238491/check-if-string-contains-only-letters
     * @param current - a given word
     * @return - true if its only letters, else false
     */
    private boolean isOnlyLetters(String current) {
        char[] chars = current.toCharArray();
        for (char c : chars) {
            if(!Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void run() {
        readThroughFiles();
    }
}