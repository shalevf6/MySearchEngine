package Part_2;

import Controller.Controller;

import java.util.ArrayList;

/**this class will find out which documents from the corpus are more likely to fit to given Query.
 *
 */
public class Searcher {

    private static boolean onlyCities;


    public Searcher(){
        if(Controller.citiesToFilter.size()>0){
            onlyCities =true;
        }
        else {
            onlyCities = false;
        }

    }

    /**Main searcher function
     * @param query is the Query given from the user
     * @return Array list with 50 most likely to fit document to the query
     */
    public ArrayList<String> queryResults(String query){

        //-----SPECIFIC CITIES CHOSEN-----//
        if(onlyCities){
            return queryResultsWithCities(query);
        }
        //--------WITHOUT CITY DEPENDENCY-------//
        else{
            return queryResultsWithoutCities(query);
        }

    }

    /**Searcher function when the user doesn't want to include cities dependency.
     * @param query a query given.
     * @return ArrayList with top 50 documents relevant to the query that contains cities chosen.
     */
    private ArrayList<String> queryResultsWithoutCities(String query) {
        ArrayList<String> toReturn = new ArrayList<String>(50);
        return toReturn;
    }

    /**Searcher function when the user want to include cities dependency.
     * @param query a query given.
     * @return ArrayList with top 50 documents relevant to the query that contains cities chosen.
     */
    private ArrayList<String> queryResultsWithCities(String query) {
        ArrayList<String> toReturn = new ArrayList<String>(50);

        return toReturn;
    }
}
