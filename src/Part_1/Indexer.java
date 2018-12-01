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
    static private boolean isDictionaryStemmed;
    static public int totalUniqueTerms = 0;
    private int totalTempPostingFiles = 0;
    private BufferedReader[] bufferedReaders;

    /**
     * Used for sorting the temp posting lines before writing them to the temp posting file
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
     * Used for sorting the temp posting lines before writing them to the main posting file
     */
    private PriorityQueue<String[]> toMainPosting = new PriorityQueue<>(new Comparator<String[]>() {
        @Override
        public int compare(String[] o1, String[] o2) {
//            System.out.println("Between " + o1 + " to " + o2); TODO: - DELETE DOCUMENTATION
            String o1Line = o1[0];
            String o2Line = o2[0];
            int toCut1 = o1Line.indexOf(':');
            int toCut2 = o2Line.indexOf(':');
            String term1 = o1Line.substring(0, toCut1);
            String term2 = o2Line.substring(0, toCut2);
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
                    docData[0] = Integer.toString(doc.getMax_tf());
                    docData[1] = Integer.toString(doc.getUniqueWords());
                    String city = doc.getCity();
                    if (city != null)
                        docData[2] = city;
                    String date = doc.getDocDate();
                    if (date != null)
                        docData[3] = date;
                    String docId = doc.getDocId();
                    documentDictionary.put(docId, docData);
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
                        tempPostingFile = new File(postingPath + "\\postingFilesWithStemming\\tempPosting" + tempPostingNum);
                        tempPostingFile.createNewFile();
                    } else {
                        tempPostingFile = new File(postingPath + "\\postingFilesWithoutStemming\\tempPosting" + tempPostingNum);
                        tempPostingFile.createNewFile();
                    }
                    tempPostingNum++;
                    FileOutputStream fileOutputStream = new FileOutputStream(tempPostingFile.getAbsolutePath());
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                    BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
                    String postingLine = toPosting.poll();
                    // for not writing a new line at the end of the temp posting file
                    if (postingLine != null)
                        bufferedWriter.write(postingLine);
                    while (postingLine != null) {
                        bufferedWriter.write('\n' + postingLine);
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

        // ------ START: COPY ALL THE TERMS AND DF VALUES FROM THE CORPUS DICTIONARY (FROM PARSE) TO THE TERM DICTIONARY ------
        HashMap<String, Integer> corpusDictionary = Parse.corpusDictionary;
        Parse.corpusDictionary = null;
        Set<String> termSet = corpusDictionary.keySet();
        for (String term : termSet) {
            int[] termData = new int[2];
            termData[0] = corpusDictionary.get(term);
            termDictionary.put(term, termData);
        }
        totalUniqueTerms = termDictionary.keySet().size();
        // ------ END: ADD DF VALUES FOR ALL THE TERMS FROM THE CORPUS DICTIONARY FROM PARSE ------

        // ------ START: MERGE ALL WRITTEN POSTING TEMP FILES AND WRITE DICTIONARY TO A FILE ------
        totalTempPostingFiles = tempPostingNum;
        if (Parse.stemming) {
            isDictionaryStemmed = true;
            mergePostingFiles(postingPath + "\\postingFilesWithStemming\\tempPosting");
            writeDictionaryToFile(postingPath + "\\postingFilesWithStemming\\termDictionary", termDictionary);
        }
        else {
            isDictionaryStemmed = false;
            mergePostingFiles(postingPath + "\\postingFilesWithoutStemming\\tempPosting");
            writeDictionaryToFile(postingPath + "\\postingFilesWithoutStemming\\termDictionary", termDictionary);
        }
        // ------ END: MERGE ALL WRITTEN POSTING TEMP FILES ------
    }

    /**
     * merges all the temp Posting Files into one
     * @param postingPath - the path for the location of all the temp posting files
     */
    private void mergePostingFiles(String postingPath) {
        File mainPostingFile = new File(postingPath + "\\mainPosting");
        try {
            mainPostingFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int counter = 1;
        bufferedReaders = new BufferedReader[totalTempPostingFiles];
        String[][] postingLines = new String[totalTempPostingFiles][2];

        // create a buffered reader for each of the sorted temp posting files
        while (counter <= totalTempPostingFiles) {
            try {
                bufferedReaders[counter - 1] = new BufferedReader(new InputStreamReader(new FileInputStream(new File(postingPath + counter))));
                counter++;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // read a line from each of the sorted temp posting files
        counter = 0;
        for (BufferedReader br : bufferedReaders) {
            try {
                postingLines[counter][0] = br.readLine();
                postingLines[counter][1] = Integer.toString(counter);
                toMainPosting.add(postingLines[counter]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        BufferedWriter bw;
        try {

            // creating a Buffered Writer for the main posting file
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mainPostingFile)));

            // check and merge all duplicate posting lines that are of the same term
            String postingLineToAdd = checkAndMergePostingLines();

            // check if the term should be in capital letters
            int toCut = postingLineToAdd.indexOf(':');
            String term = postingLineToAdd.substring(0, toCut);
            if (!termDictionary.containsKey(term)) {
                postingLineToAdd = term.toUpperCase() + postingLineToAdd.substring(toCut + 1);
                term = term.toUpperCase();
            }

            int postingPointer = 1;
            // write first line separately so the main posting file won't end with a \n (new line)
            bw.write(postingLineToAdd);

            // update the posting pointer in the dictionary to refer to the line in the posting file
            int[] termData = termDictionary.get(term);
            termData[1] = postingPointer;
            postingPointer++;

            // keep writing more posting lines
            while (!toMainPosting.isEmpty()) {

                // checks and merges all duplicate posting lines that are of the same term
                postingLineToAdd = checkAndMergePostingLines();

                // checks if the term should be in capital letters
                toCut = postingLineToAdd.indexOf(':');
                term = postingLineToAdd.substring(0, toCut);
                if (!termDictionary.containsKey(term))
                    postingLineToAdd = term.toUpperCase() + postingLineToAdd.substring(toCut + 1);

                // writes the posting line
                bw.write('\n' + postingLineToAdd);

                // update the posting pointer in the dictionary to refer to the line in the posting file
                termData = termDictionary.get(term);
                termData[1] = postingPointer;
                postingPointer++;
            }

            // closing all Buffered Readers
            for (BufferedReader br : bufferedReaders) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // closes the Buffered Writer
            bw.close();

            // clearing memory space by removing the Buffered Reader array reference
            bufferedReaders = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * checks and merges all the posting lines of the same term
     */
    private String checkAndMergePostingLines() {
        String postingLineToAdd[] = getAndAddToQueue();
        String nextPostingLineToAdd[];
        boolean hasDuplicates = true;
        while (!toMainPosting.isEmpty() && hasDuplicates) {
            // using 'peek' for the case which the term of the next line is not the same as the term of the current line
            nextPostingLineToAdd = toMainPosting.peek();
            String line1 = postingLineToAdd[0];
            String line2 = nextPostingLineToAdd[0];
            int toCut1 = line1.indexOf(':');
            int toCut2 = line2.indexOf(':');
            String term1 = line1.substring(0, toCut1);
            String term2 = line2.substring(0, toCut2);

            // checks if the 2 terms are the same
            if (term1.compareTo(term2) == 0) {
                getAndAddToQueue();
                // merges all the posting details for the term from both of the posting lines
                postingLineToAdd[0] =  postingLineToAdd[0].substring(0, line1.length()) + line2.substring(toCut2 + 1);
            }
            else
                hasDuplicates = false;
        }
        return postingLineToAdd[0];
    }

    /**
     * gets a posting line from the queue, while adding a new posting line from the same temp sorted
     * posting file the line received was from
     * @return - a string array which has a posting line and a buffered reader number in it
     */
    private String[] getAndAddToQueue() {
        String[] postingLine = toMainPosting.poll();
        int brNum = Integer.valueOf(postingLine[1]);
        try {
            toMainPosting.add(new String[] {bufferedReaders[brNum].readLine(), postingLine[1]});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return postingLine;
    }

    /**
     * turns the dictionary into a sorted string
     * @param stemming - to see if the dictionary we need to return is stemmed or not
     * @return - a string representing the sorted dictionary
     */
    public static String getDictionaryString(boolean stemming) {
        HashMap<String, int[]> currDictionary = getTermDictionary(stemming);
        Set<String> termSet = currDictionary.keySet();

        // adding all the terms and df data to the priority queue for sorting
        for (String term : termSet) {
            dictionarySort.add(term + " ; " + currDictionary.get(term)[0] + "documents;");
        }

        // starting to build the dictionary sorted string with the first entry
        String nextTerm = dictionarySort.poll();
        StringBuilder dictionary = new StringBuilder();
        if (nextTerm != null) {
            dictionary.append(nextTerm);
            nextTerm = dictionarySort.poll();
        }

        // continue building the dictionary sorted string
        while (nextTerm != null) {
            dictionary.append('\n').append(nextTerm);
            nextTerm = dictionarySort.poll();
        }
        return dictionary.toString();
    }

    /**
     * gets a requested dictionary. If it's not on the main memory, pulls it from the appropriate file
     * @param stemming - is the dictionary looked for is stemmed or not
     * @return - the looked for dictionary
     */
    public static HashMap<String, int[]> getTermDictionary(boolean stemming) {
        if (stemming) {
            if (isDictionaryStemmed)
                return termDictionary;
            else {
                termDictionary = readDictionaryFromFile(Controller.postingPathText + "\\postingFilesWithStemming\\termDictionary");
                return termDictionary;
            }
        }
        if (!isDictionaryStemmed)
            return termDictionary;
        else {
            termDictionary = readDictionaryFromFile(Controller.postingPathText + "\\postingFilesWithoutStemming\\termDictionary");
            return termDictionary;
        }
    }

    /**
     * writes a given dictionary into a file
     * @param path - the path in which the file will be created
     * @param toWrite - the dictionary to write
     */
    private void writeDictionaryToFile(String path, HashMap<String,int[]> toWrite) {
        File dictionary = new File(path);
        try {
            dictionary.createNewFile();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(dictionary));
            objectOutputStream.writeObject(toWrite);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * reads a dictionary from a file
     * @return - the dictionary
     */
    private static HashMap<String,int[]> readDictionaryFromFile(String path) {
        File dictionary = new File(path);
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(dictionary.getAbsolutePath()));
            return (HashMap<String,int[]>)objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
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
}
