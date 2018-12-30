package GeneralClasses;

import java.util.HashMap;

/**
 * This class represents a query for parsing through it in the Parse class
 */
public class Query {

    private String queryText;
    private HashMap<String, int[]> queryTermDictionary;

    public Query(String queryText) {
        this.queryText = queryText;
    }

    /**
     * gets the query's text
     * @return - the query's text
     */
    public String getQueryText() {
        return queryText;
    }

    /**
     * gets the query's term dictionary
     * @return - the query's term dictionary
     */
    public HashMap<String, int[]> getQueryTermDictionary() {
        return queryTermDictionary;
    }

    /**
     * sets the query's term dictionary
     * @param queryTermDictionary - the query's term dictionary
     */
    public void setQueryTermDictionary(HashMap<String, int[]> queryTermDictionary) {
        this.queryTermDictionary = queryTermDictionary;
    }
}
