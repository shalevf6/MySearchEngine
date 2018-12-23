package Part_2;

import Controller.Controller;
import GeneralClasses.Query;
import Part_1.Indexer;
import Part_1.Parse;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * This class processes a given query and check what relevant documents are in the corpus
 */
public class Searcher {

    public static HashMap<String, int[]> queryDictionary;
    private String query;

    public Searcher(String query) {
        queryDictionary = new HashMap<>();
        this.query = query;
    }

    /**
     * sends the query Parse to get parsed, and ranks the documents according to the query
     * @return - a queue of the relevant documents sorted by their rank (according to the query)
     */
    public Queue<String> processQuery() {
        Query queryObject = new Query(query);
        Parse parse = new Parse("", true, queryObject); // TODO : FIND THE PATH OF THE STOP WORDS FILE FOR THE PARSE CLASS
        parse.run();
        Ranker ranker = new Ranker();

        // turn all the query terms in to an array for ranking
        String[] queryTerms = new String[queryDictionary.size()];
        Set<String> queryKeys = queryDictionary.keySet();
        int i = 0;
        for (String query : queryKeys) {
            queryTerms[i] = query;
            i++;
        }

        Queue<String> docsFromRanker = ranker.rank(queryTerms);
        Queue<String> docsUpToFifty;

        // checks and filters the documents according to the cities the user chose
        if (Controller.citiesToFilter.size() > 0) {
            Queue<String> tempQueue = new LinkedList<>();
            while (!docsFromRanker.isEmpty()) {
                String document = docsFromRanker.poll();
                if (containsCity(document))
                    tempQueue.add(document);
            }
            docsFromRanker = tempQueue;
        }

        // makes sure there are no more than 50 documents in the document queue
        if (docsFromRanker.size() > 50) {
            docsUpToFifty = new LinkedList<>();
            i = 0;
            while (!docsFromRanker.isEmpty() && i < 50) {
                docsUpToFifty.add(docsFromRanker.poll());
                i++;
            }
        } else
            docsUpToFifty = docsFromRanker;

        return docsUpToFifty;
    }

    /**
     * checks whether a given document has any of the cities the user chose to filter the query with
     * @param document - a given document
     * @return - true if it contains one of the cities to filter. Else - false
     */
    private boolean containsCity(String document) {
        try {
            RandomAccessFile raf = new RandomAccessFile(Controller.postingPathText + "\\postingForCities\\mainCityPosting", "r");
            Set<String> citiesToFilter = Controller.citiesToFilter.keySet();
            String cityOfDocument = Indexer.documentDictionary.get(document)[2];
            int pointerToCity;
            // goes through all the cities to filter
            boolean[] hasCities = new boolean[citiesToFilter.size()];
            for (int i = 0; i < hasCities.length; i ++)
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