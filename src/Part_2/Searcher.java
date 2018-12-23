package Part_2;

import GeneralClasses.Query;
import Part_1.Parse;
import java.util.HashMap;
import java.util.Queue;
import java.util.Set;

public class Searcher {

    public static HashMap<String, int[]> queryDictionary;
    private String query;

    public Searcher(String query) {
        queryDictionary = new HashMap<>();
        this.query = query;
    }

    public void sendQueryToParse() {
        Query queryObject = new Query(query);
        Parse parse = new Parse("",true, queryObject); // TODO : FIND THE PATH OF THE STOP WORDS FILE FOR THE PARSE CLASS
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

        Queue<String> docs = ranker.rank(queryTerms);

    }
}