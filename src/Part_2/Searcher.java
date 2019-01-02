package Part_2;

import Controller.Controller;
import GeneralClasses.Query;
import Part_1.Indexer;
import Part_1.Parse;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * This class processes a given query and check what relevant documents are in the corpus
 */
public class Searcher implements Runnable {

    public static HashSet<String> documentsAfterCityFiltering;
    private String query;
    private String description;
    private String stopWordsPath;
    private boolean semanticTreatment;
    private Queue<String> relevantDocuments;

    public Searcher(String query, String description, String stopWordsPath, boolean semanticTreatment) {
        this.query = query;
        this.description = description;
        this.stopWordsPath = stopWordsPath;
        this.semanticTreatment = semanticTreatment;
    }

    /**
     * sends the query Parse to get parsed, and ranks the documents according to the query
     */
    private void processQuery() {
        if (semanticTreatment)
            query = bigSemantic(query);
        Query queryObject;
        // if the query is from a file of queries, we add to the processing of the query its description
        if (description != null) {
            String[] descriptionSplit = description.split(" ");
            StringBuilder correctedDescription = new StringBuilder();
            int i = 0;
            while (i < descriptionSplit.length) {
                descriptionSplit[i] = removeExtraDelimiters(descriptionSplit[i]);
                String lowerCase = descriptionSplit[i].toLowerCase();
                // removes redundant query terms from the description
                if (lowerCase.equals("identify") || lowerCase.equals("documents") || lowerCase.equals("document") || lowerCase.equals("discuss") || lowerCase.equals("associated")
                        || lowerCase.equals("issues")) {
                    i++;
                    continue;
                }
                correctedDescription.append(" ").append(descriptionSplit[i]);
                i++;
            }
            description = correctedDescription.toString();
            queryObject = new Query(query + " " + description);
        }
        else
            queryObject = new Query(query);
        Parse parse = new Parse(stopWordsPath, true, queryObject, Indexer.isDictionaryStemmed);
        parse.run();
        Ranker ranker = new Ranker();

        // turn all the query terms in to an array for ranking
        HashMap<String, int[]> queryDictionary = queryObject.getQueryTermDictionary();
        String[] queryTerms = new String[queryDictionary.size()];
        Set<String> queryKeys = queryDictionary.keySet();
        int i = 0;
        for (String query : queryKeys) {
            queryTerms[i] = query;
            i++;
        }

        relevantDocuments = ranker.rank(queryTerms);
    }

    /**
     * returns 3 similar words for each word in the query
     * @param str - a given string
     * @return - its semantic addition
     */
    private String bigSemantic(String str) {
        String[] strings = str.split(" ");
        StringBuilder toReturn = new StringBuilder();
        int i = 0;
        while (i < strings.length) {
            strings[i] = removeExtraDelimiters(strings[i]);
            String result = semantic(strings[i]);
            if (result != null)
                toReturn.append(result);
            else
                toReturn.append(strings[i]);
            i++;
        }
        return toReturn.toString();
    }

    /**
     * removes any extra delimiters from a given word's start or end recursively
     * @param word - a given word
     * @return - the given word after the delimiter removal (if necessary)
     */
    private String removeExtraDelimiters(String word) {
        if (!word.equals("")) {

            int length = word.length();

            // checks if there is a delimiter at the start of the word
            if (word.charAt(0) > 122 || word.charAt(0) < 65 || (word.charAt(0) > 90 && word.charAt(0) < 97)) {
                return removeExtraDelimiters(word.substring(1));
            }

            // checks if there is a delimiter at the end of the word
            if (word.charAt(length - 1) > 122 || word.charAt(length - 1) < 65 || (word.charAt(length - 1) > 90 && word.charAt(length - 1) < 97))
                return removeExtraDelimiters(word.substring(0, length - 1));

            return word;
        }
        return word;
    }

    /**
     * finds similar words from the api to make the search better
     *
     * @param str the string that we want to find similar words to
     * @return the string given + 3 most similar words
     */
    private String semantic(String str) {
        StringBuilder res = new StringBuilder();
        String[] arr = str.split(" ");
        String toApi = "";
        for (int i = 0; i < arr.length; i++) {
            if (i == arr.length - 1)
                toApi = arr[i];
            else
                toApi = arr[i] + "+";
        }
        URL address;
        try {
            address = new URL("https://api.datamuse.com/words?ml=" + toApi);
            StringBuilder json = new StringBuilder("{\"result\":");
            Scanner scan = new Scanner(address.openStream());
            while (scan.hasNext())
                json.append(scan.nextLine());
            scan.close();
            json.append("}");
            JSONObject jsonObject = new JSONObject(json.toString());
            JSONArray result = jsonObject.getJSONArray("result");
            int i = 0;
            for (Object obj : result) {
                if (i == 1)
                    break;
                JSONObject data = (JSONObject) obj;
                res.append(" ").append(data.getString("word"));
                i++;
            }
            return res.toString();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * gets the relevant documents of the query
     * @return - a queue with the relevant documents of the query
     */
    public Queue<String> getRelevantDocuments() {
        return relevantDocuments;
    }

    @Override
    public void run() {
        processQuery();
    }
}