package Part_2;

import Controller.Controller;
import Part_1.Indexer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

/**
 * This class ranks all the documents in the corpus according to a given query
 */
public class Ranker {
    private double avgAllDocsLength;
    private double numberOfDocs;

    public Ranker(){
        this.numberOfDocs = Indexer.totalDocuments;
        avgAllDocsLength = calcAvgDocLength(numberOfDocs);
    }

    /**calculate corpus average document length
     * @param numberOfDocs
     * @return the average document length in the corpus.
     */
    private double calcAvgDocLength(double numberOfDocs) {
        double ans = 0.0;
        Collection<String[]> docs = Indexer.documentDictionary.values();
        Iterator<String[]> employeeSalaryIterator = docs.iterator();
        while (employeeSalaryIterator.hasNext()) {
            String[] entry = employeeSalaryIterator.next();
            ans =ans + Integer.valueOf(entry[4]);
        }
        ans = ans / numberOfDocs;
        return ans;
    }

    Queue<String> rank(String[] query) {
        Queue<String> rankToReturn = new LinkedList<>();
        HashMap<String, String[]>[] allMaps = new HashMap[query.length];
        //PriorityQueue[] allQueryTerms = new PriorityQueue[query.length];
        RandomAccessFile ToCheck = null;
        if (Indexer.isDictionaryStemmed) {           //First,Open RandomAccessFile//

            try {
                ToCheck = new RandomAccessFile(Controller.postingPathText + "\\postingFilesWithStemming\\mainPosting", "r");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            try {
                ToCheck = new RandomAccessFile(Controller.postingPathText + "\\postingFilesWithoutStemming\\mainPosting", "r");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < query.length; i++) {  //For every term in the query:
            allMaps[i] = new HashMap<>();
            HashMap<String, String[]> allDocsInQuery = new HashMap<>(); //HashMap contains Document Number and all info that need for the calculation//
            String temp = query[i];
            int termToFind;
            if(Indexer.termDictionary.containsKey(temp))
                termToFind = Indexer.termDictionary.get(temp)[2];
            else if(Indexer.termDictionary.containsKey(temp.toUpperCase())) {
                termToFind = Indexer.termDictionary.get(temp.toUpperCase())[2];
                temp = temp.toUpperCase();
            }
            else {
                termToFind = 0;
                continue;
            }

            try {
                ToCheck.seek(termToFind);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String termLine = null;
            try {
                termLine = ToCheck.readLine(); //finds from posting file all documents with this specific term//
            } catch (IOException e) {
                e.printStackTrace();
            }
            int StartIndex = termLine.indexOf(':');
            termLine = termLine.substring(StartIndex +1 ); //cuts the beginning of the string //
            String[] allTermDocs = termLine.split(";"); //divides the info to all documents with this term to array of strings//
            for (int j = 0; j < allTermDocs.length; j++) {
                String[] DocInfo = allTermDocs[j].split(","); //divides all info of this term in specific document to string array//
                //-----NOW WE WILL PUT THE INFO NEEDED IN THE STRING ARRAY----//
                String[] docInfoToPut = new String[7];
                docInfoToPut[0] = DocInfo[1]; //tf
                docInfoToPut[1] = Indexer.documentDictionary.get(DocInfo[0])[4]; //docLength
                docInfoToPut[2] = String.valueOf(Indexer.termDictionary.get(temp)[0]); //docFrequency
                docInfoToPut[3] ="";
                docInfoToPut[3] += DocInfo[2].charAt(2); //10%
                docInfoToPut[4] = "";
                docInfoToPut[4] += DocInfo[2].charAt(1);//Title
                if(Indexer.documentDictionary.get(DocInfo[0])[2]!= null && temp.equals( Indexer.documentDictionary.get(DocInfo[0])[2]))
                    docInfoToPut[5] ="1";//city
                else
                    docInfoToPut[5] = "0";//city
                if(Indexer.documentDictionary.get(DocInfo[0])[3]!= null && temp.equals( Indexer.documentDictionary.get(DocInfo[0])[3]))
                    docInfoToPut[6] ="1";//date
                else
                    docInfoToPut[6] = "0";//date
                allDocsInQuery.put(DocInfo[0], docInfoToPut);

            }
            allMaps[i] = allDocsInQuery;
        }
        //-------Merging all HashMaps------//
        ArrayList<String> AllDocsInString = new ArrayList<>(); //ArrayList with all document numbers that has at least one of the query terms//
        Set<String> mergedDocNums = new HashSet<String>() ;
        for(int o = 0 ; o < allMaps.length ; o++ ){
            mergedDocNums.addAll(allMaps[o].keySet());
        }
        AllDocsInString.addAll(mergedDocNums);
        HashMap<String,String[][]> mergedDataByDoc = new HashMap<>(); //Main HashMap
        for (String DocNum: AllDocsInString)
        {
            String[][] toInsert = new String[query.length][7];
            for(int q = 0 ; q < query.length ; q++){
                toInsert[q] = allMaps[q].get(DocNum);
            }
            mergedDataByDoc.put(DocNum,toInsert);
        }
        PriorityQueue<String[]> docsAfterBM25 = new PriorityQueue<>(new Comparator<String[]>() {
            @Override
            public int compare(String[] o1, String[] o2) {
                int ans = 0;
                double ans1 = 0.0;
                ans1 =  (Double.valueOf(o1[1]) -Double.valueOf(o2[1]));
                if(ans1 > 0)
                    return 1;
                if(ans1 < 0)
                    return -1;
                return ans;
            }
        });
        for (String DocNum: AllDocsInString)
        {
            String[] arr = new String[2];
            arr[0] = DocNum;
            double[] tfs = new double[query.length];
            double[] docLength = new double[query.length];
            double[] docFrequency = new double[query.length];
            int[] doc10Present = new int[query.length];
            int[] docTitle = new int[query.length];
            int[] docCity = new int[query.length];
            int[] docDate = new int[query.length];
            for(int k = 0 ; k < tfs.length ; k++){
                if(mergedDataByDoc.get(DocNum)[k] != null)
                    tfs[k] = Double.parseDouble(mergedDataByDoc.get(DocNum)[k][0]);
                if(mergedDataByDoc.get(DocNum)[k] != null)
                    docLength[k] = Double.parseDouble(mergedDataByDoc.get(DocNum)[k][1]);
                if(mergedDataByDoc.get(DocNum)[k] != null)
                    docFrequency[k] = Double.parseDouble(mergedDataByDoc.get(DocNum)[k][2]);
                if(mergedDataByDoc.get(DocNum)[k] != null)
                    doc10Present[k] = Integer.parseInt(mergedDataByDoc.get(DocNum)[k][3]);
                if(mergedDataByDoc.get(DocNum)[k] != null)
                    docTitle[k] = Integer.parseInt(mergedDataByDoc.get(DocNum)[k][4]);
                if(mergedDataByDoc.get(DocNum)[k] != null)
                    docCity[k] = Integer.parseInt(mergedDataByDoc.get(DocNum)[k][5]);
                if(mergedDataByDoc.get(DocNum)[k] !=null )
                    docDate[k] = Integer.parseInt(mergedDataByDoc.get(DocNum)[k][6]);
            }
            arr[1] = Double.toString(getScore(tfs,numberOfDocs,docLength,avgAllDocsLength,docFrequency,docCity,docDate,doc10Present,docTitle));
            if(Double.parseDouble(arr[1]) > 0)
                docsAfterBM25.add(arr);
        }
        while(!docsAfterBM25.isEmpty()){
            rankToReturn.add(docsAfterBM25.poll()[0]);
        }
        return rankToReturn;
    }
    /**function that returns the rank of a specific term in a specific document according to the query(BM25).
     * @param tf  term frequency
     * @param numOfDocs number of documents
     * @param docLength document length
     * @param avgDocLength average all document length
     * @param docFrequency number of documents contains Q_i
     * @return idf
     */
    public double getRankIdf(double tf, double numOfDocs, double docLength, double avgDocLength, double docFrequency){
        double k_1 = 1.2;
        double b = 0.75;
        double k = k_1 * ( (  1 - b ) + ( ( b * docLength ) / avgDocLength ) );
        double weight = ( ( ( k_1 + 1 ) * tf ) / (k + tf ) ) ;
        //calculate IDF//
        double idf = weight * Math.log ( ( numOfDocs - docFrequency + 0.5) / ( docFrequency + 0.5));
        return idf;
    }

    /**returns the whole Query score by in a specific document.
     * @param tf term frequency
     * @param numOfDocs number of documents in the corpus
     * @param docLength the length of the document
     * @param avgDocLength average document length in the corpus
     * @param docFrequency number of documents contains Q_i
     * @return
     */
    public double getScore( double[]  tf, double numOfDocs,double[] docLength, double avgDocLength, double[] docFrequency,int[] docCity,int[] docDate,int[] doc10Percent,int[] docTitle ){
        double ans = 0.0;
        for(int i = 0 ; i < tf.length ; i++){
            double tempAns = getRankIdf(tf[i], numOfDocs, docLength[i], avgDocLength,docFrequency[i]  );
            double tempAnsToMultiply = 0.1*tempAns;
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
}