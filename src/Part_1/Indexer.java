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
    static boolean indexedCities = false;
    static public int totalUniqueTerms = 0;
    static BlockingQueue<Document> docQueue = new ArrayBlockingQueue<>(1000);
    static public HashMap<String, String[]> corpusCityDictionary = new HashMap<>();
    static public HashMap<String, int[]> termDictionary = new HashMap<>();
    static public HashMap<String, String[]> documentDictionary = new HashMap<>();
    public static boolean isDictionaryStemmed;
    private HashMap<String, String> tempTermDictionary = new HashMap<>();
    private int totalTempPostingFiles = 0;
    private BufferedReader[] bufferedReaders;

    /**
     * Used for sorting the entries of a posting line according to normalized tf
     */
    private PriorityQueue<String> entriesSort = new PriorityQueue<>((o1, o2) -> {
        String[] data1 = o1.split(",");
        String[] data2 = o2.split(",");
        return data1[1].compareTo(data2[1]);
    });

    /**
     * Used for sorting the temp posting lines before writing them to the temp posting file
     */
    private PriorityQueue<String> toPosting = new PriorityQueue<>((o1, o2) -> {
        int toCut1 = o1.indexOf(':');
        int toCut2 = o2.indexOf(':');
        String term1 = o1.substring(0, toCut1);
        String term2 = o2.substring(0, toCut2);
        return term1.compareTo(term2);
    });

    /**
     * Used for sorting the temp posting lines before writing them to the main posting file
     */
    private PriorityQueue<String[]> toMainPosting = new PriorityQueue<>((o1, o2) -> {
        String o1Line = o1[0];
        String o2Line = o2[0];
        int toCut1 = o1Line.indexOf(':');
        int toCut2 = o2Line.indexOf(':');
        String term1 = o1Line.substring(0, toCut1);
        String term2 = o2Line.substring(0, toCut2);
        return term1.compareTo(term2);
    });

    /**
     * Used for sorting the dictionary before presenting it to the user
     */
    static private PriorityQueue<String> dictionarySort = new PriorityQueue<>((o1, o2) -> {
        int toCut1 = o1.indexOf(';');
        int toCut2 = o2.indexOf(';');
        String term1 = o1.substring(0, toCut1 - 1);
        String term2 = o2.substring(0, toCut2 - 1);
        return term1.compareTo(term2);
    });

    /**
     * the primary index function
     * @param postingPath - the path for saving the posting files
     */
    private void indexAll(String postingPath){
        // ------ START: CREATE ALL TEMP POSTING FILES ------
        int tempPostingNum = 1;
        while (true) {
            if (!docQueue.isEmpty()) {
                int counter = 10000;
                while (counter > 0 && !docQueue.isEmpty()) {
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
                    HashMap<String, short[]> docTermDictionary = doc.getTermDictionary();
                    // SAVE MEMORY - change docDictionary reference in Document class to null
                    doc.deleteDictionary();
                    // ------ START: ADD POSTING ENTRIES FROM EACH TERM IN THE DOCUMENT ------
                    Set<String> termSet = docTermDictionary.keySet();
                    double max_tf = doc.getMax_tf();
                    for (String term : termSet) {
                        if (!term.equals("")) {
                            short[] termData = docTermDictionary.get(term);
                            String postingValue;
                            double normalizedTf = termData[0];
                            normalizedTf = normalizedTf / max_tf;
                            // ---- it's THE FIRST posting entry for this term ----
                            if (!tempTermDictionary.containsKey(term)) {
                                postingValue = term + ":" + docId + "," + normalizedTf + "," + termData[1] + termData[2] + termData[3] + ";";
                                tempTermDictionary.put(term, postingValue);
                            }
                            // ---- it's NOT THE FIRST posting entry for this term ----
                            else {
                                postingValue = tempTermDictionary.get(term);
                                postingValue = postingValue + docId + "," + normalizedTf + "," + termData[1] + termData[2] + termData[3] + ";";
//                                postingValue = sortByTf(postingValue);
                                tempTermDictionary.put(term, postingValue);
                            }
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
                    File tempPostingCityFile;
                    boolean firstEntry = true;
                    BufferedWriter cityBufferedWriter = null;
                    // --- IN ORDER TO NOT CREATE THE CITIES' POSTING FILES MORE THAN ONCE ---
                    if (!indexedCities) {
                        tempPostingCityFile = new File(postingPath + "\\postingForCities\\tempPostingCity" + tempPostingNum);
                        cityBufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempPostingCityFile)));
                    }
                    tempPostingNum++;
                    FileOutputStream fileOutputStream = new FileOutputStream(tempPostingFile);
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                    BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
                    String postingLine = toPosting.poll();
                    // for not writing a new line at the end of the temp posting file
                    if (postingLine != null) {
                        bufferedWriter.write(postingLine);
                        // for not writing a new line at the end of the temp city posting file
                        if (!indexedCities && corpusCityDictionary.containsKey(postingLine.split(":")[0])) {
                            cityBufferedWriter.write(postingLine);
                            firstEntry = false;
                        }
                    }
                    postingLine = toPosting.poll();
                    while (postingLine != null) {
                        bufferedWriter.write('\n' + postingLine);
                        // for not writing the cities' posting files twice
                        if (!indexedCities && corpusCityDictionary.containsKey(postingLine.split(":")[0])) {
                            if (!firstEntry)
                                cityBufferedWriter.write('\n' + postingLine);
                            else {
                                cityBufferedWriter.write(postingLine);
                                firstEntry = false;
                            }
                        }
                        postingLine = toPosting.poll();
                    }
                    if (!indexedCities)
                        cityBufferedWriter.close();
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

        // ------ START: UPDATE THE TOTAL UNIQUE TERMS FOR THE CORPUS FROM THE TERM DICTIONARY ------
        totalUniqueTerms = termDictionary.size();
        // ------ END: UPDATE THE TOTAL UNIQUE TERMS FOR THE CORPUS FROM THE TERM DICTIONARY ------

        // ------ START: MERGE ALL WRITTEN POSTING TEMP FILES AND WRITE DICTIONARIES TO FILES ------
        totalTempPostingFiles = tempPostingNum;
        if (Parse.stemming) {
            isDictionaryStemmed = true;
            mergePostingFiles(postingPath + "\\postingFilesWithStemming");

            writeDictionaryToDisk(postingPath + "\\postingFilesWithStemming\\termDictionary", 1);
            writeTermDictionaryForShowToDisk(postingPath + "\\postingFilesWithStemming\\termDictionaryForShow", true);
            writeDictionaryToDisk(postingPath + "\\postingFilesWithStemming\\documentDictionary", 2);
        }
        else {
            isDictionaryStemmed = false;
            mergePostingFiles(postingPath + "\\postingFilesWithoutStemming");
            writeDictionaryToDisk(postingPath + "\\postingFilesWithoutStemming\\termDictionary", 1);
            writeTermDictionaryForShowToDisk(postingPath + "\\postingFilesWithoutStemming\\termDictionaryForShow", false);
            writeDictionaryToDisk(postingPath + "\\postingFilesWithoutStemming\\documentDictionary", 2);
        }
        if (!indexedCities) {
            mergeCityPostingFiles(postingPath + "\\postingForCities");
            writeDictionaryToDisk(postingPath + "\\postingForCities\\cityDictionary", 3);
            indexedCities = true;
        }
        deleteAllTempFiles(postingPath);
        // ------ END: MERGE ALL WRITTEN POSTING TEMP FILES AND WRITE DICTIONARIES TO FILES ------
    }

    /**
     * deletes all the temp files from the posting path
     * @param postingPath - the posting path which the files are saved in
     */
    private void deleteAllTempFiles(String postingPath) {
        File mainDir = new File(postingPath);
        File[] dirs = mainDir.listFiles();
        if (dirs != null && dirs.length > 0) {
            for (File dir : dirs) {
                File[] files = dir.listFiles();
                for (int i = 0; i < files.length; i++) {
                    if (files[0].getName().contains("temp"))
                        files[0].delete();
                }
            }

        }
    }

    /**
     * sorts the posting line entries by the normalized tf value
     * @param dictionaryValue - the posting line before sorting
     * @return - the posting line after sorting
     */
    private String sortByTf(String dictionaryValue) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] splitByTerm = dictionaryValue.split(":");
        String[] splitByEntries = splitByTerm[1].split(";");
        stringBuilder.append(splitByTerm[0] + ":");
        // enters all the entries into the priority queue for sorting
        for (String entry : splitByEntries) {
            entriesSort.add(entry);
        }
        // gets the sorted entries out of the priority queue
        while (!entriesSort.isEmpty())
            stringBuilder.append(entriesSort.poll() + ";");
        return stringBuilder.toString();
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
        while (counter < totalTempPostingFiles) {
            try {
                bufferedReaders[counter - 1] = new BufferedReader(new InputStreamReader(new FileInputStream(new File(postingPath + "\\tempPosting" + counter))));
                counter++;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // reads a line from each of the sorted temp posting files
        readFirstLines(bufferedReaders, postingLines);

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

            // sorts the posting line's entries by normalized tf
//            postingLineToAdd = sortByTf(postingLineToAdd);

            // write first line separately so the main posting file won't end with a \n (new line)
            bw.write(postingLineToAdd);

            // initiates the bytes counter for the posting pointer
            int postingPointer = 0;

            // updates the posting pointer in the dictionary to refer to the line in the posting file
            int[] termData = termDictionary.get(term);
            termData[2] = postingPointer;

            // updates the bytes counter for the posting pointer
            postingPointer = postingPointer + postingLineToAdd.getBytes().length;

            // keep writing more posting lines
            while (!toMainPosting.isEmpty()) {

                // checks and merges all duplicate posting lines that are of the same term
                postingLineToAdd = checkAndMergePostingLines();

                // checks if the term should be in capital letters
                toCut = postingLineToAdd.indexOf(':');
                term = postingLineToAdd.substring(0, toCut);
                if (!termDictionary.containsKey(term)) {
                    postingLineToAdd = term.toUpperCase() + postingLineToAdd.substring(toCut + 1);
                }

                // writes the posting line
                bw.write('\n' + postingLineToAdd);

                // updates the posting pointer in the dictionary to refer to the line in the posting file
                termData = termDictionary.get(term);
                termData[1] = postingPointer;

                // updates the bytes counter for the posting pointer
                postingPointer = postingPointer + postingPointer;
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
     * merges all the temp city Posting Files into one
     * @param postingPath - the path for the location of all the temp city posting files
     */
    private void mergeCityPostingFiles(String postingPath) {
        File mainCityPostingFile = new File(postingPath + "\\mainCityPosting");
        try {
            mainCityPostingFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int counter = 1;
        bufferedReaders = new BufferedReader[totalTempPostingFiles];
        String[][] postingLines = new String[totalTempPostingFiles][2];

        // create a buffered reader for each of the sorted temp posting files
        while (counter < totalTempPostingFiles) {
            try {
                bufferedReaders[counter - 1] = new BufferedReader(new InputStreamReader(new FileInputStream(new File(postingPath + "\\tempPostingCity" + counter))));
                counter++;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // reads a line from each of the sorted temp posting files
        readFirstLines(bufferedReaders, postingLines);

        BufferedWriter bw;
        try {

            // creating a Buffered Writer for the main posting file
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mainCityPostingFile)));

            // check and merge all duplicate posting lines that are of the same term
            String postingLineToAdd = checkAndMergePostingLines();

            // sorts the posting line's entries by normalized tf
//            postingLineToAdd = sortByTf(postingLineToAdd);

            // initiates the bytes counter for the posting pointer
            int postingPointer = 0;

            // write first line separately so the main posting file won't end with a \n (new line)
            bw.write(postingLineToAdd);

            // updates the posting pointer in the dictionary to refer to the line in the posting file
            int toCut = postingLineToAdd.indexOf(':');
            String term = postingLineToAdd.substring(0, toCut);
            String[] termData = corpusCityDictionary.get(term);
            termData[3] = String.valueOf(postingPointer);
            // updated the bytes counter for the posting pointer
            postingPointer = postingLineToAdd.getBytes().length;

            // keep writing more posting lines
            while (!toMainPosting.isEmpty()) {

                // checks and merges all duplicate posting lines that are of the same term
                postingLineToAdd = checkAndMergePostingLines();

                // writes the posting line
                bw.write('\n' + postingLineToAdd);

                // updates the posting pointer in the dictionary to refer to the line in the posting file
                toCut = postingLineToAdd.indexOf(':');
                term = postingLineToAdd.substring(0, toCut);
                termData = corpusCityDictionary.get(term);
                termData[3] = String.valueOf(postingPointer);

                // updated the bytes counter for the posting pointer
                postingPointer = postingPointer + postingLineToAdd.getBytes().length;
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
     * reads the first posting lines from each of the temp posting files into a given postingLines array
     * @param bufferedReaders - a buffered readers array for reading posting lines from each of the temp posting files
     * @param postingLines - a string array to insert input of the posting lines from the temp posting files
     */
    private void readFirstLines(BufferedReader[] bufferedReaders, String[][] postingLines) {
        // read a line from each of the sorted temp posting files
        int counter = 0;
        for (BufferedReader br : bufferedReaders) {
            try {
                postingLines[counter][0] = br.readLine();
                postingLines[counter][1] = Integer.toString(counter);
                toMainPosting.add(postingLines[counter]);
                counter++;
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                // removes the posting line with the same term from the queue and adds a new line instead
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
        Set<String> termSet = termDictionary.keySet();

        // adding all the terms and df data to the priority queue for sorting
        for (String term : termSet) {
            dictionarySort.add(term + " ; " + termDictionary.get(term)[1] + " total appearances;");
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
     */
    private static void setTermDictionary(boolean stemming) {
        if (stemming) {
            if (!isDictionaryStemmed) {
                readDictionaryToMemory(Controller.postingPathText + "\\postingFilesWithStemming\\termDictionary", 1);
                totalUniqueTerms = termDictionary.size();
                isDictionaryStemmed = true;
            }
        } else if (isDictionaryStemmed) {
            readDictionaryToMemory(Controller.postingPathText + "\\postingFilesWithoutStemming\\termDictionary", 1);
            totalUniqueTerms = termDictionary.size();
            isDictionaryStemmed = false;
        }
    }

    /**
     * writes a certain dictionary (term / document / city) into a file in the disk
     * @param path - the path in which the file will be created
     * @param whatToWrite - to know which dictionary to write to a file in the disk
     */
    private void writeDictionaryToDisk(String path, int whatToWrite) {
        File dictionary = new File(path);
        try {
            dictionary.createNewFile();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(dictionary));
            if (whatToWrite == 1)
            objectOutputStream.writeObject(termDictionary);
            if (whatToWrite == 2)
                objectOutputStream.writeObject(documentDictionary);
            if (whatToWrite == 3)
                objectOutputStream.writeObject(corpusCityDictionary);
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * write the dictionary's string to a file in the disk
     * @param path - the path which the dictionary's string should be written to
     * @param stemming - is the dictionary stemmed or not
     */
    private void writeTermDictionaryForShowToDisk(String path, boolean stemming) {
        File dictionaryForShow = new File(path);
        try {
            dictionaryForShow.createNewFile();
            // creating a Buffered Writer for writing the dictionary's string
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dictionaryForShow)));
            bw.write(getDictionaryString(stemming));
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * reads a dictionary's sorted string (to show) from a file in the disk to the main memory
     * @return - the dictionary's string
     */
    public static String readDictionaryForShowToMemory(String path) {
        File dictionary = new File(path);
        try {
            // creating a Buffered Writer for writing the dictionary's string
            BufferedReader bw = new BufferedReader(new InputStreamReader(new FileInputStream(dictionary)));
            StringBuilder dictionaryString = new StringBuilder();
            String line;
            while ((line = bw.readLine()) != null) {
                dictionaryString.append(line);
            }
            bw.close();
            return dictionaryString.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * reads a dictionary from a file in the disk (term / document / city) to the main memory
     * @param whatToRead - indicates to what dictionary to insert the object we read
     */
    public static void readDictionaryToMemory(String path, int whatToRead) {
        File dictionary = new File(path);
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(dictionary));
            if (whatToRead == 1)
                termDictionary = (HashMap<String,int[]>)objectInputStream.readObject();
            if (whatToRead == 2)
                documentDictionary = (HashMap<String,String[]>)objectInputStream.readObject();
            if (whatToRead == 3)
                corpusCityDictionary = (HashMap<String,String[]>)objectInputStream.readObject();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * loads the term dictionary and the document dictionary from the disk to the main memory
     * @param tempPostingPath - the temp posting path
     * @param stemming - is the dictionaries are stemmed or not
     */
    public static void loadAllDictionariesToMemory(String tempPostingPath, boolean stemming) {
        // read the term dictionary to the memory
        setTermDictionary(stemming);
        // reads the document dictionary to the memory
        if (stemming)
            readDictionaryToMemory(tempPostingPath + "\\postingFilesWithStemming\\documentDictionary", 2);
        else
            readDictionaryToMemory(tempPostingPath + "\\postingFilesWithoutStemming\\documentDictionary", 2);
        ReadFile.docCount = documentDictionary.size();
    }

    /**
     * stops creating the indexes
     */
    static void stop() {
        stop = true;
    }

    /**
     * resets all the static variables
     */
    public static void resetAll() {
        stop = false;
        corpusCityDictionary = new HashMap<>();
        termDictionary = new HashMap<>();
        documentDictionary = new HashMap<>();
        indexedCities = false;
        totalUniqueTerms = 0;
        isDictionaryStemmed = false;
    }

    /**
     * resets parts of the static variables in order to index again, with / without stemming
     */
    static void resetPartially() {
        stop = false;
        termDictionary = new HashMap<>();
        documentDictionary = new HashMap<>();
        indexedCities = false;
        totalUniqueTerms = 0;
    }

    @Override
    public void run() {
        indexAll(Controller.postingPathText);
    }
}