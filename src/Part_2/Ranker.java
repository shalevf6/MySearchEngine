package Part_2;

import Controller.Controller;
import Part_1.Indexer;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

/**
 * This class ranks all the documents in the corpus according to a given query
 */
class Ranker {
    private double avgAllDocsLength;
    private double numberOfDocs;

    /**
     * default constructor for the ranker class
     */
    Ranker() {
        this.numberOfDocs = Indexer.totalDocuments;
        avgAllDocsLength = calcAvgDocLength(numberOfDocs);
    }

    /**
     * calculate corpus average document length
     *
     * @param numberOfDocs
     * @return the average document length in the corpus.
     */
    private double calcAvgDocLength(double numberOfDocs) {
        double ans = 0.0;
        Collection<String[]> docs = Indexer.documentDictionary.values();
        Iterator<String[]> employeeSalaryIterator = docs.iterator();
        while (employeeSalaryIterator.hasNext()) {
            String[] entry = employeeSalaryIterator.next();
            ans = ans + Integer.valueOf(entry[4]);
        }
        ans = ans / numberOfDocs;
        return ans;
    }

    /**
     * main ranking function in the Ranker class
     * @param query is the query that the user typed after parsing and semantics if needed
     * @return queue with the best   documents for the query given
     */
    Queue<String> rank(String[] query) {
        Queue<String> rankToReturn = new LinkedList<>();
        HashMap[] allMaps = new HashMap[query.length];
        //PriorityQueue[] allQueryTerms = new PriorityQueue[query.length];
        RandomAccessFile ToCheck;
        try {
            if (Indexer.isDictionaryStemmed)          //First,Open RandomAccessFile//
                ToCheck = new RandomAccessFile(Controller.postingPathText + "\\postingFilesWithStemming\\mainPosting.txt", "r");
            else
                ToCheck = new RandomAccessFile(Controller.postingPathText + "\\postingFilesWithoutStemming\\mainPosting.txt", "r");
            for (int i = 0; i < query.length; i++) {  //For every term in the query:
                allMaps[i] = new HashMap<String,String[]>();
                HashMap<String, String[]> allDocsInQuery = new HashMap<>(); //HashMap contains Document Number and all info that need for the calculation//
                String temp = query[i];
                int termToFind;
                // term exists in term dictionary as is
                if (Indexer.termDictionary.containsKey(temp))
                    termToFind = Indexer.termDictionary.get(temp)[2];
                else {
                    if (temp.equals(temp.toUpperCase())) {
                        // term exists in term dictionary in lower case letters
                        if (Indexer.termDictionary.containsKey(temp.toLowerCase())) {
                            termToFind = Indexer.termDictionary.get(temp.toLowerCase())[2];
                            temp = temp.toLowerCase();
                        }
                        else
                            continue;
                    } else {
                        // term exists in term dictionary in upper case letters
                        if (Indexer.termDictionary.containsKey(temp.toUpperCase())) {
                            termToFind = Indexer.termDictionary.get(temp.toUpperCase())[2];
                            temp = temp.toUpperCase();
                        } else
                            continue;
                    }
                }
                ToCheck.seek(termToFind);
                String termLine = ToCheck.readLine(); //finds from posting file all documents with this specific term//
                int StartIndex = termLine.indexOf(':');
                termLine = termLine.substring(StartIndex + 1); //cuts the beginning of the string //
                String[] allTermDocs = termLine.split(";"); //divides the info to all documents with this term to array of strings//
                for (String allTermDoc : allTermDocs) {
                    String[] DocInfo = allTermDoc.split(","); //divides all info of this term in specific document to string array//
                    //-----NOW WE WILL PUT THE INFO NEEDED IN THE STRING ARRAY----//
                    String[] docInfoToPut = new String[7];
                    docInfoToPut[0] = DocInfo[1]; //tf
                    docInfoToPut[1] = Indexer.documentDictionary.get(DocInfo[0])[4]; //docLength
                    docInfoToPut[2] = String.valueOf(Indexer.termDictionary.get(temp)[0]); //docFrequency
                    docInfoToPut[3] = "";
                    docInfoToPut[3] += DocInfo[2].charAt(2); //10%
                    docInfoToPut[4] = "";
                    docInfoToPut[4] += DocInfo[2].charAt(1);//Title
                    if (Indexer.documentDictionary.get(DocInfo[0])[2] != null && temp.equals(Indexer.documentDictionary.get(DocInfo[0])[2]))
                        docInfoToPut[5] = "1";//city
                    else
                        docInfoToPut[5] = "0";//city
                    if (Indexer.documentDictionary.get(DocInfo[0])[3] != null && temp.equals(Indexer.documentDictionary.get(DocInfo[0])[3]))
                        docInfoToPut[6] = "1";//date
                    else
                        docInfoToPut[6] = "0";//date
                    allDocsInQuery.put(DocInfo[0], docInfoToPut);

                }
                allMaps[i] = allDocsInQuery;
            }
            //-------Merging all HashMaps------//
            Set<String> mergedDocNums = new HashSet<>();
            for (HashMap allMap : allMaps) {
                mergedDocNums.addAll(allMap.keySet());
            }
            // defining a priority queue for sorting the documents retrieved by rank
            PriorityQueue<String[]> docsAfterBM25 = new PriorityQueue<>((o1, o2) -> {
                double ans;
                ans = (Double.valueOf(o1[1]) - Double.valueOf(o2[1]));
                if (ans > 0)
                    return -1;
                if (ans < 0)
                    return 1;
                return 0;
            });

            // creates an ArrayList containing all documents relevant to the query
            ArrayList<String> AllDocsInString = new ArrayList<>(mergedDocNums);
            // goes through every document in order to create its rank
            for (String DocNum : AllDocsInString) {
                String[][] queryTermInfo = new String[query.length][7];
                // an array with the rank of each document
                String[] scoreArray = new String[2];
                scoreArray[0] = DocNum;
                double[] tfs = new double[query.length];
                double[] docLength = new double[query.length];
                double[] docFrequency = new double[query.length];
                int[] doc10Present = new int[query.length];
                int[] docTitle = new int[query.length];
                int[] docCity = new int[query.length];
                int[] docDate = new int[query.length];
                // goes through each query term if it existed in the document
                for (int q = 0; q < query.length; q++) {
                    queryTermInfo[q] = (String[]) allMaps[q].get(DocNum);
                    if (queryTermInfo[q] != null) {
                        // frees some more memory space
                        allMaps[q].remove(DocNum);
                        // gets the query term's frequency in the document
                        tfs[q] = Double.parseDouble(queryTermInfo[q][0]);
                        // gets the document's length
                        docLength[q] = Double.parseDouble(queryTermInfo[q][1]);
                        // gets the query term's document frequency
                        docFrequency[q] = Double.parseDouble(queryTermInfo[q][2]);
                        // checks if the query term is in the first 10% of the document's text
                        doc10Present[q] = Integer.parseInt(queryTermInfo[q][3]);
                        // checks if the query term is in the title of the document
                        docTitle[q] = Integer.parseInt(queryTermInfo[q][4]);
                        // checks if the query term is in the city of the document
                        docCity[q] = Integer.parseInt(queryTermInfo[q][5]);
                        // checks if the query term is in the date of the document
                        docDate[q] = Integer.parseInt(queryTermInfo[q][6]);
                    }
                }
                // sends all the document's data for calculation in order to get its rank
                scoreArray[1] = Double.toString(getScore(tfs, numberOfDocs, docLength, avgAllDocsLength, docFrequency, docCity, docDate, doc10Present, docTitle));
                // adds the document to the priority queue
                if (Double.parseDouble(scoreArray[1]) > 0)
                    docsAfterBM25.add(scoreArray);
            }

            // frees some more memory space
            allMaps = null;

            // adds all the documents to the final queue
            int i = 0;
            // checks and filters the documents according to the cities the user chose
            if (Controller.citiesToFilter.size() > 0) {
                while (!docsAfterBM25.isEmpty() && i < 50) {
                    String document = docsAfterBM25.poll()[0];
                    if (containsCities(document)) {
                        rankToReturn.add(document);
                        i++;
                    }
                }
                return rankToReturn;
            }
            else {
                while (!docsAfterBM25.isEmpty() && i < 50) {
                    rankToReturn.add(docsAfterBM25.poll()[0]);
                    i++;
                }
                return rankToReturn;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return rankToReturn;
        }
    }

    /**
     * function that returns the rank of a specific term in a specific document according to the query(BM25).
     *
     * @param tf           term frequency
     * @param numOfDocs    number of documents
     * @param docLength    document length
     * @param avgDocLength average all document length
     * @param docFrequency number of documents contains Q_i
     * @return idf
     */
    private double getRankIdf(double tf, double numOfDocs, double docLength, double avgDocLength, double docFrequency) {
        double k_1 = 1.6;
        double b = 0.75;
        double k = k_1 * ((1 - b) + ((b * docLength) / avgDocLength));
        double weight = (((k_1 + 1) * tf) / (k + tf));
        //calculate IDF//
        double idf = weight * Math.log10((numOfDocs - docFrequency + 0.5) / (docFrequency + 0.5));
        return idf;
    }


    /**
     * returns the whole Query's score in a specific document.
     *
     * @param tf           term frequency
     * @param numOfDocs    number of documents in the corpus
     * @param docLength    the length of the document
     * @param avgDocLength average document length in the corpus
     * @param docFrequency number of documents contains Q_i
     * @param docCity      array of ints-- 1 if term is in the city doc, 0 if not
     * @param docDate      array of ints-- 1 if term is in the Date doc, 0 if not
     * @param doc10Percent array of ints-- 1 if term is in the first 10% of the text,0 if not
     * @param docTitle     array of ints- 1 if term is in the title of the doc, 0 if not
     * @return the rank of the whole query.
     */
    private double getScore(double[] tf, double numOfDocs, double[] docLength, double avgDocLength, double[] docFrequency, int[] docCity, int[] docDate, int[] doc10Percent, int[] docTitle) {
        double ans = 0.0;
        for (int i = 0; i < tf.length; i++) {
            double tempAns = getRankIdf(tf[i], numOfDocs, docLength[i], avgDocLength, docFrequency[i]);
            double tempAnsToMultiply = 0.05*tempAns;
            //----if is in the City----//
            if(docCity[i]==1)
                tempAns+=tempAnsToMultiply;
            //----if is in the Date----//
            if(docDate[i]==1)
                tempAns+=tempAnsToMultiply;
            //----if is in the 10 Percent----//
            if(doc10Percent[i]==1)
                tempAns+=tempAnsToMultiply;
            //----if is in the Title----//
            if(docTitle[i]==1)
                tempAns+=tempAnsToMultiply;
            ans = ans + tempAns;
        }
        return ans;
    }

    /**
     * checks whether a given document has any of the cities the user chose to filter the query with
     * @param document - a given document
     * @return - true if it contains one of the cities to filter. Else - false
     */
    private boolean containsCities(String document) {
        try {
            RandomAccessFile raf = new RandomAccessFile(Controller.postingPathText + "\\postingForCities\\mainCityPosting.txt", "r");
            Set<String> citiesToFilter = Controller.citiesToFilter.keySet();
            String cityOfDocument = Indexer.documentDictionary.get(document)[2];
            int pointerToCity;
            // goes through all the cities to filter
            boolean[] hasCities = new boolean[citiesToFilter.size()];
            for (int i = 0; i < hasCities.length; i++)
                hasCities[i] = false;
            int i = 0;
            for (String cityToFilter : citiesToFilter) {
                // checks whether the current city is the document's city
                if (cityOfDocument != null && cityOfDocument.equals(cityToFilter)) {
                    hasCities[i] = true;
                    i++;
                    continue;
                }
                // checks if the city exists somewhere else in the document by accessing the cities posting file
                pointerToCity = Integer.valueOf(Indexer.corpusCityDictionary.get(cityToFilter)[3]);
                raf.seek(pointerToCity);
                String cityPostingLine = raf.readLine();
                String[] splitByDocs = (cityPostingLine.split(":"))[1].split(";");
                for (String docFromPosting : splitByDocs) {
                    if ((docFromPosting.split(",")[0]).equals(document)) {
                        hasCities[i] = true;
                        break;
                    }
                }
                // to check whether there was a city with no appearances in the doc at all
                if (hasCities[i])
                    i++;
                else
                    return false;
            }

            // TODO : probably an unnecessary check. Consider removing it
            for (i = 0; i < hasCities.length; i++)
                if (!hasCities[i])
                    return false;

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}