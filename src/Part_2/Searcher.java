package Part_2;

import GeneralClasses.Document;
import GeneralClasses.Query;
import Part_1.Parse;
import java.util.HashMap;

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
    }

}
