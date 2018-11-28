package GeneralClasses;

import java.util.*;

/**
 * This class represents a document
 */
public class Document {

    private String docId;
    private String[] docText;
    private int docTextSize;
    private String docTitle;
    private String docDate;
    private String city;
    private ArrayList<String> termList;
    private HashMap<String,int[]> termDictionary;
    private int max_tf;
    private int uniqueWords;

    /**
     * A constructor for the Document class which recieves the document's ID
     * @param docId - the document's ID
     */
    public Document(String docId) {
        this.docId = docId;
        docTextSize = 0;
    }

    /**
     * adds another <text></text> String to the Document
     * @param text - a new <text></text> String to the Document to be added to the document
     */
    public void addDocText(String text) {
        if (docTextSize == 0) {
            docText = new String[1];
            docText[0] = text;
            docTextSize++;
        }
        else {
            String[] tempArr = new String[docTextSize];
            for (int i = 0; i < docTextSize; i++) {
                tempArr[i] = docText[i];
            }
            docText = new String[docTextSize + 1];
            for (int i = 0; i < docTextSize; i++) {
                docText[i] = tempArr[i];
            }
            docText[docTextSize] = text;
            docTextSize++;
        }
    }
    /**
     * sets the document's title (if exists)
     * @param docTitle - the document's title
     */
    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }

    /**
     * sets the document's date (if exists)
     * @param docDate - the document's date
     */
    public void setDocDate(String docDate) {
        this.docDate = docDate;
    }

    /**
     * sets the document's term list
     * @param termList - the document's term list
     */
    public void setTermList(ArrayList<String> termList) {
        this.termList = termList;
    }

    /**
     * sets the document's term dictionary, unique words count and max_tf
     * @param termDictionary - the document's term dictionary
     * @param max_tf - ther document's max_tf
     */
    public void setTfAndTermDictionary(HashMap<String,int[]> termDictionary, int max_tf) {
        this.termDictionary = termDictionary;
        uniqueWords = termDictionary.keySet().size();
        this.max_tf = max_tf;
    }

    /**
     * sets the doucment's city (if exists)
     * @param city
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * gets the document's id
     * @return the document's id
     */
    public String getDocId() {
        return docId;
    }

    /**
     * gets the <text></text> String array
     * @return the document's <text></text> String array
     */
    public String[] getDocText() {
        return docText;
    }

    /**
     * gets the document's title (if exists)
     * @return - the document's title if exists. Otherwise - null
     */
    public String getDocTitle() {
        return docTitle;
    }

    /**
     * gets the document's date (if exists)
     * @return - the document's date if exists. Otherwise - null
     */
    public String getDocDate() {
        return docDate;
    }

    /**
     * gets the document's term list
     * @return - the document's term list
     */
    public ArrayList<String> getTermList() {
        return termList;
    }

    /**
     * gets the document's term dictionary
     * @return - the document's term dictionary
     */
    public HashMap<String,int[]> getTermDictionary() {
        return termDictionary;
    }

    /**
     * gets the document's city (if exists)
     * @return - the document's city if exists. Otherwise - null
     */
    public String getCity() {
        return city;
    }

    /**
     * gets the document's max_tf value
     * @return - the document's max_tf value
     */
    public int getMax_tf() {
        return max_tf;
    }

    /**
     * gets the document's unique words count
     * @return - the document's unique words count
     */
    public int getUniqueWords() {
        return uniqueWords;
    }
}