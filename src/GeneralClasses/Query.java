package GeneralClasses;

/**
 * This class represents a query for parsing through it in the Parse class
 */
public class Query {

    private String queryText;

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
}
