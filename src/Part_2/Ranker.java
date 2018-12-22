package Part_2;

import Controller.Controller;
import Part_1.Indexer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * will rank all the documents in the corpus according to given query
 */
public class Ranker {

    public Ranker(){

    }
    private HashMap<Integer,String> rank(String[] query) throws IOException {
        HashMap<Integer,String> rankToreturn = new HashMap<>();
        PriorityQueue[] allQueryTerms = new PriorityQueue[query.length];
        for (PriorityQueue allQueryTerm : allQueryTerms) {
            allQueryTerm = new PriorityQueue<>(new Comparator<String[]>() {
                @Override
                public int compare(String[] o1, String[] o2) {
                    return Integer.valueOf(o1[2]) - Integer.valueOf(o2[2]);
                }
            });
        }

        for (int queueIndex = 0; queueIndex < allQueryTerms.length; queueIndex++) {
            RandomAccessFile ToCheck;
            if(Indexer.isDictionaryStemmed) {
                ToCheck = new RandomAccessFile(Controller.postingPathText + "\\postingFilesWithStemming\\mainPosting", "r");
            }
            else {
                ToCheck = new RandomAccessFile(Controller.postingPathText + "\\postingFilesWithoutStemming\\mainPosting", "r");
            }
            for (int i = 0; i < query.length; i++) {
                String temp = query[i];
                int termToFind = Indexer.termDictionary.get(temp)[2];
                ToCheck.seek(termToFind);
                String termLine = ToCheck.readLine();
                int StartIndex = termLine.indexOf(':');
                termLine = termLine.substring(StartIndex+1);
                String[] allTermDocs = termLine.split(";");
                for(int j = 0 ; j <allQueryTerms.length ; j++){
                    String[] allTermInfoInArray = allTermDocs[i].split(",");
                    allQueryTerms[queueIndex].add(allQueryTerms);
                }
            }

        }
        PriorityQueue priorityQueue = new PriorityQueue<>(new Comparator<String[]>() {
            @Override
            public int compare(String[] o1, String[] o2) {
                return Integer.valueOf(o1[2]) - Integer.valueOf(o2[2]);
            }
        });
        return rankToreturn;
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
    public double getScore( double[]  tf, double numOfDocs,double[] docLength, double avgDocLength, double[] docFrequency){
        double ans = 0.0;
        for(int i = 0 ; i < tf.length ; i++){
            ans = ans + getRankIdf(tf[i], numOfDocs, docLength[i], avgDocLength,docFrequency[i]  );
        }
        return ans;
    }
}
