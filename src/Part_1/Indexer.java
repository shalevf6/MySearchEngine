package Part_1;

import Controller.Controller;
import GeneralClasses.Document;

import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * This class creates the posting files for the corpus
 */
public class Indexer implements Runnable {

    static private boolean stop = false;
    static public BlockingQueue<Document> docQueue = new ArrayBlockingQueue<>(1000);
    static public HashMap<String, int[]> termDictionary = new HashMap<>();
    static public HashMap<String, String> tempTermDictionary = new HashMap<>();
    static public HashMap<String, String[]> documentDictionary = new HashMap<>();
    static public int totalUniqueTerms = 0;

    /**
     * Used for sorting the posting lines before writing them to the posting file
     */
    private PriorityQueue<String> toPosting = new PriorityQueue<>(new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
//            System.out.println("Between " + o1 + " to " + o2); TODO: - DELETE DOCUMENTATION
            int toCut1 = o1.indexOf(':');
            int toCut2 = o2.indexOf(':');
            String term1 = o1.substring(0, toCut1);
            String term2 = o2.substring(0, toCut2);
            return term1.compareTo(term2);
        }
    });

    /**
     * Used for sorting the dictionary before presenting it to the user
     */
    static private PriorityQueue<String> dictionarySort = new PriorityQueue<>(new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            int toCut1 = o1.indexOf(';');
            int toCut2 = o2.indexOf(';');
            String term1 = o1.substring(0, toCut1 - 1);
            String term2 = o2.substring(0, toCut2 - 1);
            return term1.compareTo(term2);
        }
    });

    private void indexAll(String postingPath){
        // ------ START: CREATE ALL TEMP POSTING FILES ------
        int tempPostingNum = 1;
        while (true) {
            if (!docQueue.isEmpty()) {
                int counter = 10000;
                while (counter > 0 && !stop) {
                    // ------ START: ADD ALL DOCUMENT DETAILS TO DOCUMENT DICTIONARY ------
                    Document doc = null;
                    try {
                        doc = docQueue.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String[] docData = new String[4];
                    totalUniqueTerms = totalUniqueTerms + doc.getUniqueWords();
                    docData[0] = Integer.toString(doc.getMax_tf());
                    docData[1] = Integer.toString(doc.getUniqueWords());
                    String city = doc.getCity();
                    if (city != null)
                        docData[2] = city;
                    String date = doc.getDocDate();
                    if (date != null)
                        docData[3] = date;
                    String docId = doc.getDocId();
                    documentDictionary.put(docId,docData);
                    // ------ FINISH: ADD ALL DOCUMENT DETAILS TO DOCUMENT DICTIONARY ------
                    HashMap<String, int[]> docTermDictionary = doc.getTermDictionary();
                    // SAVE MEMORY - change docDictionary reference in Document class to null
                    doc.deleteDictionary();
                    // ------ START: ADD POSTING ENTRIES FROM EACH TERM IN THE DOCUMENT ------
                    Set<String> termSet = docTermDictionary.keySet();
                    for (String term : termSet) {
                        int[] termData = docTermDictionary.get(term);
                        String dictionaryValue;
                        // ---- it's THE FIRST posting entry for this term ----
                        if (!tempTermDictionary.containsKey(term)) {
                            dictionaryValue = term + ":" + docId + "," + termData[0] + "," + termData[1] + termData[2] + termData[3] + ";";
                            tempTermDictionary.put(term, dictionaryValue);
                        }
                        // ---- it's NOT THE FIRST posting entry for this term ----
                        else {
                            dictionaryValue = tempTermDictionary.get(term);
                            dictionaryValue = dictionaryValue + docId + "," + termData[0] + "," + termData[1] + termData[2] + termData[3] + ";";
                            tempTermDictionary.put(term, dictionaryValue);
                        }
                    }
                    // ------ END: ADD POSTING ENTRIES FROM EACH TERM IN THE DOCUMENT ------
                    counter--;
                }

                // ------ START: TRANSFER ALL POSTING LINES TO PRIORITY QUEUE FOR NATURAL ORDER SORTING ------
                Set<String> termSet = tempTermDictionary.keySet();
                Iterator<String> termIter = termSet.iterator();
                while (termIter.hasNext()) {
                    String term = termIter.next();
                    if (term != null && !term.equals(""))
                        toPosting.add(tempTermDictionary.get(term));
                    termIter.remove();
                }
                // ------ END: TRANSFER ALL POSTING LINES TO PRIORITY QUEUE FOR NATURAL ORDER SORTING ------

                // ------ START: WRITE A TEMP POSTING FILE ------
                try {
                    File tempPostingFile;
                    if (Parse.stemming) {
                        tempPostingFile = new File (postingPath + "\\postingFilesWithStemming\\tempPosting" + tempPostingNum);
                        tempPostingFile.createNewFile();
                    }
                    else {
                        tempPostingFile = new File (postingPath + "\\postingFilesWithoutStemming\\tempPosting" + tempPostingNum);
                        tempPostingFile.createNewFile();
                    }
                    tempPostingNum++;
                    FileOutputStream fileOutputStream = new FileOutputStream(tempPostingFile.getAbsolutePath());
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                    BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
                    String postingLine = toPosting.poll();
                    while (postingLine != null) {
                        bufferedWriter.write(postingLine + "\n");
                        postingLine = toPosting.poll();
                    }
                    bufferedWriter.close();
                    outputStreamWriter.close();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // ------ END: WRITE A TEMP POSTING FILE ------

            }
            // ---- check if there are no more documents to write to the temp posting files ----
            else {
                if (stop)
                    // ------ END: CREATED ALL TEMP POSTING FILES ------
                    break;
            }
        }

        HashMap<String, Integer> corpusDictionary = Parse.corpusDictionary;

        // ------ START: COPY ALL THE TERMS AND DF VALUES FROM THE CORPUS DICTIONARY (FROM PARSE) TO THE TERM DICTIONARY ------
        Parse.corpusDictionary = null;
        Set<String> termSet = corpusDictionary.keySet();
        for (String term : termSet) {
            int[] termData = new int[2];
            termData[0] = corpusDictionary.get(term);
            termDictionary.put(term, termData);
        }
        // ------ END: ADD DF VALUES FOR ALL THE TERMS FROM THE CORPUS DICTIONARY FROM PARSE ------

        // ------ START: MERGE ALL WRITTEN POSTING TEMP FILES ------

        // ------ END: MERGE ALL WRITTEN POSTING TEMP FILES ------

    }

    /**
     * stops creating the indexes
     */
    static void stop() {
        stop = true;
    }

    @Override
    public void run() {
        indexAll(Controller.postingPathText);
    }

    /**
     * turns the dictionary into a sorted string
     * @return - a string representing the sorted dictionary
     */
    public static String getDictionaryString() {
        Set<String> termSet = termDictionary.keySet();
        for (String term : termSet) {
            dictionarySort.add(term + " ; " + termDictionary.get(term)[0] + "documents;");
        }
        String nextTerm = dictionarySort.poll();
        StringBuilder dictionary = new StringBuilder();
        if (nextTerm != null) {
            dictionary.append(nextTerm);
            nextTerm = dictionarySort.poll();
        }
        while (nextTerm != null) {
            dictionary.append('\n').append(nextTerm);
            nextTerm = dictionarySort.poll();
        }
        return dictionary.toString();
    }
}
