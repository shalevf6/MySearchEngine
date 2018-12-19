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
}
