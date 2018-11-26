package Part_1;

import GeneralClasses.Document;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * this class parses through the documents and creates a term list and a dictionary for every document
 */
public class Parse implements Runnable {

    private HashMap<String,Integer> StopWords;
    static public BlockingQueue<Document> docQueue = new ArrayBlockingQueue<>(1000);
    static private boolean stop = false;
    static public HashMap<String,Integer> corpusDictionary = new HashMap<>();
    static public int docNumber = 0;
    private HashMap<String,Integer> currentTermDictionary;
    private ArrayList<String> parsed;
    private String[] AfterSplit;

    /**
     * a constructor for the Parse class
     * @param path - the path to the stop words file
     */
    public Parse(String path){
        getStopWords(path);
    }

    /** This function generates the stop words Dictionary to Array List.
     * @param path is the location of the Dictionary
     */
    private void getStopWords(String path) {
        StopWords = new HashMap<>();
        File stopWordsFile = new File(path);
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(stopWordsFile));
            String stopWord = bufferedReader.readLine();
            while(stopWord != null) {
                stopWord = stopWord.trim();
                StopWords.put(stopWord,1);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This is the main Parse Function
     */
    public void parseAll(){
        while (true) {
            if (!docQueue.isEmpty()) {
                currentTermDictionary = new HashMap<>();
                boolean added = false;
                boolean dollar = false;
                parsed = new ArrayList<>();
                Document document = docQueue.remove();
                String[] documents = document.getDocText();
                docNumber++;
                for (String data:documents) {
                    String current;
                    int counter = 0;
                    //splits the string
                    AfterSplit = data.split("[?!:+{*}|<=>\"\\s;()_&\\\\\\[\\]]+");
                    int wordCounter = 0;
                    while (wordCounter < AfterSplit.length) {
                        AfterSplit[wordCounter] = removeExtraDelimiters(AfterSplit[wordCounter]);
                        wordCounter++;
                    }
                    // goes through every word in the document
                    while (counter <= AfterSplit.length - 1) {
                        current = AfterSplit[counter];
                        // checks if the current string is a stop word (and not the word between)
                        if (!(current.equals("between") || current.equals("Between") || current.equals("BETWEEN")) && StopWords.containsKey(current)) {
                            counter++;
                        }
                        else {
                            // checks if there aren't any numbers in the word
                            if (!isNumeric2(current)) {
                                // ------- 'BETWEEN NUMBER AND NUMBER' CHECK -------
                                // checks if the 1st word is "between"
                                if (current.equals("between") || current.equals("Between") || current.equals("BETWEEN")) {
                                    boolean caseDone = false;
                                    caseDone = handleBetweenNumberAndNumber(current, counter);
                                    if (caseDone) {
                                        counter = counter + 4;
                                        continue;
                                    } else {
                                        counter++;
                                        continue;
                                    }
                                }
                                // ------- 'WORD-WORD' | 'WORD-WORD-WORD' CHECK -------
                                if (current.contains("-")) {
                                    handleWordsWithDash(current);
                                }
                                // ------- 'WORD/WORD' CHECK -------
                                if (current.contains("/")) {
                                    String[] orSplit = current.split("/");
                                    for (String orWord : orSplit) {
                                        if (!StopWords.containsKey(orWord) && isOnlyLetters(orWord)) {
                                            handleAllLetters(orWord);
                                        } else {
                                            // TODO: 26/11/2018 - consider adding a function which adds an exceptional word
                                        }
                                    }
                                    counter++;
                                    continue;
                                }
                                // ------- CAPITAL LETTERS CHECK -------
                                if (isOnlyLetters(current)) {
                                    handleAllLetters(current);
                                    counter++;
                                    continue;
                                } else {
                                    // TODO: 26/11/2018 - consider adding a function which adds an exceptional word
                                }
                            }
                            // means it's a number:
                            else {
                                // checks if the number is the whole word
                                if (!isNumeric(current)) {
//                                if(!added) {
                                    // ------- PERCENTAGE CHECK -------
                                    // --- case 1: NUMBER% ---
                                    if (current.contains("%")) {
                                        parsed.add(current);
                                        added = true;
                                    } else {
                                        if (counter + 1 < AfterSplit.length) {
                                            // --- case 2, 3: NUMBER percent, NUMBER percentage ---
                                            if (AfterSplit[counter + 1].equals("percent") || AfterSplit[counter + 1].equals("percentage")) {
                                                current = current + "%";
                                                parsed.add(current);
                                                added = true;
                                                counter++;
                                            }
                                        } else {
//                                }
                                            String current2 = "";
                                            String current3 = "";
                                            String current4 = "";
                                            if (counter + 1 < AfterSplit.length)
                                                current2 = AfterSplit[counter + 1];
                                            if (counter + 2 < AfterSplit.length)
                                                current3 = AfterSplit[counter + 2];
                                            if (counter + 3 < AfterSplit.length)
                                                current4 = AfterSplit[counter + 3];
                                            // ------- PRICE CHECK -------
                                            dollar = checkIfMoney(current, current2, current3, current4);
                                            // --- all cases: Price Dollars, Price Fraction Dollars, $price,....
                                            if (dollar) {
                                                /* !!! need to update counter according to term !!! */
                                                current = change_to_price(current, current2, current3, current4);
                                                parsed.add(current);
                                                added = true;
                                            } else {
                                                // ------- NUMBER CHECK -------
                                                // NEED TO CHECK CURRENT2 = MILLION \ BILLION \ TRILLION \ THOUSAND
                                                if (current.contains(",") && !added) {
                                                    current = changeNumToRegularNum(current);
                                                    parsed.add(current);
                                                    added = true;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            added = false;
                            dollar = false;
                            counter++;
                        }
                    }
                }
                document.setTermList(parsed);
                Indexer.docQueue.add(document);
            }
            else {
                if (stop) {
                    Indexer.stop();
                    break;
                }
            }
        }
    }

    private void handleWordsWithDash(String current) {
        String[] dashSplit = current.split("-");
        boolean allWords = true;
        int dashSplitLength = dashSplit.length;
        // if there are 2 or 3 words between the '-' delimiter
        if (dashSplitLength == 2 || dashSplitLength == 3) {
            for (int i = 0; i < dashSplitLength; i++)
                if (!StopWords.containsKey(dashSplit[i])) { // TODO might be unnecessary
                    if (!isOnlyLetters(dashSplit[i])) {
                        allWords = false;
                        handleNormalLetters(dashSplit[i]);
                    } else
                        handleAllLetters(dashSplit[i]);
                }
            // if all the words between the '-' delimiter are letters
            if (allWords) {
                handleNormalLetters(current);
            }
        }
        // if there are more than 3 words between the '-' delimiter
        else {
            for (String dashWord : dashSplit) {
                if (!StopWords.containsKey(dashWord)) {
                    if(isOnlyLetters(dashWord))
                        handleAllLetters(dashWord);
                } else
                    handleNormalLetters(dashWord);
            }
        }
    }

    /**
     * checks and handles the 'BETWEEN NUMBER AND NUMBER' term
     * @param current1 - the first word of the term
     * @param counter - the counter for the words in the text
     * @return - true if the term was found to be true, else - false
     */
    private boolean handleBetweenNumberAndNumber(String current1, int counter) {
        // TODO add every number to the term dictionaries
        String current2;
        String current3;
        String current4 ;
        // checks if the 2nd word is a NUMBER
        if (counter + 1 < AfterSplit.length && isNumeric2(AfterSplit[counter + 1])) {
            current2 = AfterSplit[counter + 1];
            // checks if the 3rd word is "and"
            if (counter + 2 < AfterSplit.length && (AfterSplit[counter + 2].equals("and") || AfterSplit[counter + 2].equals("And") ||
                    AfterSplit[counter + 2].equals("AND"))) {
                current3 = AfterSplit[counter + 2];
                // checks if the 4th word is a NUMBER
                if (counter + 3 < AfterSplit.length && isNumeric2(AfterSplit[counter + 3])) {
                    current4 = AfterSplit[counter + 3];
                    // TODO add a number fix for current2 and current4
                    handleNormalLetters(current1 + current2 + current3 + current4);
                    return true;
                }
                else {
                    /* TODO add a fraction check and a number with a "thousand" / "million" / "billion" / "trillion" after check for current2 and current 3
                       TODO if so, add one more current (and check the length of the String list accordingly)
                       TODO else, return false;
                    */
                    return false;
                }
            }
            else {
                /* TODO add a fraction check and a number with a "thousand" / "million" / "billion" / "trillion" after check for current2 and current 3
                   TODO if so, add one more current (and check the length of the String list accordingly)
                   TODO else, return false;
                */
                return false;
            }
        }
        else {
            return false;
        }
    }

    /**
     * handles all letter cases for a given word
     * @param current - a given word
     */
    private void handleAllLetters(String current) {
        // --- case 1: all of the word is in lower letters ---
        if (current.toLowerCase().equals(current)) {
            handleLowerLetters(current);
            parsed.add(current);
        } else {
            // --- case 2: only first letter of word is a capital letter ---
            if (current.charAt(0) >= 65 && current.charAt(0) <= 90 &&
                    current.substring(1).toLowerCase().equals(current.substring(1))) {
                char lowerLetter = (char) (current.charAt(0) + 32);
                String lowerCurrent = lowerLetter + current.substring(1);
                handleCapitalLetters(lowerCurrent);
                parsed.add(lowerCurrent);
            } else {
                // --- case 3: all of the word is in capital letters ---
                if (current.toUpperCase().equals(current)) {
                    String lowerCurrent = current.toLowerCase();
                    handleCapitalLetters(lowerCurrent);
                    parsed.add(lowerCurrent);
                }
                // --- case 4: mixed capital, lower case word - > none of the above cases ---
                else {
                    handleNormalLetters(current);
                }
            }
        }
    }

    /**
     * removes any extra delimiters from a given word that we didn't remove when we split the words in the text prior
     * @param word - a given word
     * @return - the given word after the delimiter removal (if necessary)
     */
    private String removeExtraDelimiters(String word) {
        if(!word.equals("")) {
            if(word.equals("U.S."))
                return word;
            int length = word.length() - 1;
            if (word.charAt(length) == ',' || word.charAt(length) == '.' || word.charAt(length) == '/' || word.charAt(length) == '-') {
                word = word.substring(0, length);
                length = length - 1;
            }
            if (!word.equals("") && word.length()>1 && (word.charAt(0) == ',' || word.charAt(0) == '/' || word.charAt(0) == '.' || word.charAt(0) == '-'))
                word = word.substring(1);
        }
        return word;
    }

    /**
     * adds the given word to the corpus and document dictionaries
     * @param current - a given word
     */
    private void handleNormalLetters(String current) {
        String lowerCurrent = current.toLowerCase();
        updateDictionaries(lowerCurrent);
        parsed.add(lowerCurrent);
    }

    /**
     * checks whether a lower case word has been already put into the dictionary, and if so, in lower letters or capital letters,
     * and than adds the relevant form of the word (upper or lower case) to the corpus and document dictionaries
     * @param current - the lower case form of the word
     */
    private void handleLowerLetters(String current) {
        String currentUpper = current.toUpperCase();
        // word is not in corpus dictionary and is not in document dictionary
        if(!corpusDictionary.containsKey(current) && !corpusDictionary.containsKey(currentUpper)) {
            corpusDictionary.put(current,1);
            currentTermDictionary.put(current,1);
        }
        else {
            // word is in corpus dictionary in lower case
            if (corpusDictionary.containsKey(current)) {
                // word is in document dictionary in lower case
                if (currentTermDictionary.containsKey(current))
                    currentTermDictionary.put(current, currentTermDictionary.get(current) + 1);
                // word is not in document dictionary
                else {
                    currentTermDictionary.put(current, 1);
                    corpusDictionary.put(current,corpusDictionary.get(current) + 1);
                }
            } else {
                // word is in corpus dictionary in upper case
                if (corpusDictionary.containsKey(currentUpper)) {
                    int df = corpusDictionary.get(currentUpper);
                    corpusDictionary.remove(currentUpper);
                    // word is not in document dictionary
                    if (!currentTermDictionary.containsKey(currentUpper)) {
                        df++;
                        currentTermDictionary.put(current,1);
                    }
                    // word is in document dictionary in upper case
                    else {
                        int tf = currentTermDictionary.get(currentUpper);
                        currentTermDictionary.remove(currentUpper);
                        currentTermDictionary.put(current,tf + 1);
                    }
                    corpusDictionary.put(currentUpper, df);
                }
            }
        }
    }

    /**
     * checks whether a capital case word has been already put into the dictionary, and if so, in lower letters or capital letters,
     * and than adds the relevant form of the word (upper or lower case) to the corpus and document dictionaries
     * @param current - the lower case form of the word
     */
    private void handleCapitalLetters(String current) {
        String currentUpper = current.toUpperCase();
        // word is not in corpus dictionary and is not in document dictionary
        if(!corpusDictionary.containsKey(current) && !corpusDictionary.containsKey(currentUpper)) {
            corpusDictionary.put(currentUpper,1);
            currentTermDictionary.put(currentUpper,1);
        }
        else {
            // word is in corpus dictionary in lower case
            if (corpusDictionary.containsKey(current)) {
                // word is in document dictionary in lower case
                if (currentTermDictionary.containsKey(current))
                    currentTermDictionary.put(current, currentTermDictionary.get(current) + 1);
                    // word is not in document dictionary
                else {
                    currentTermDictionary.put(current, 1);
                    corpusDictionary.put(current,corpusDictionary.get(current) + 1);
                }
            } else {
                // word is in corpus dictionary in upper case
                if (corpusDictionary.containsKey(currentUpper)) {
                    // word is in document dictionary in upper case
                    if (currentTermDictionary.containsKey(currentUpper))
                        currentTermDictionary.put(currentUpper,currentTermDictionary.get(currentUpper) + 1);
                    // word is not in document dictionary
                    else {
                        currentTermDictionary.put(currentUpper, 1);
                        corpusDictionary.put(currentUpper,corpusDictionary.get(currentUpper) + 1);
                    }
                }
            }
        }
    }

    /**
     * adds a new (or existing) term to the corpus and document dictionaries
     * @param current - the current term to be added to the dictionaries
     */
    private void updateDictionaries(String current) {
        boolean newTerm = true;
        if (currentTermDictionary.containsKey(current)) {
            currentTermDictionary.put(current, currentTermDictionary.get(current) + 1);
            newTerm = false;
        }
        else
            currentTermDictionary.put(current,1);
        if (newTerm)
            corpusDictionary.put(current,corpusDictionary.get(current) + 1);
    }

    /**
     * checks if a given word is composed only of letters or not
     * taken from https://stackoverflow.com/questions/5238491/check-if-string-contains-only-letters
     * @param current - a given word
     * @return - true if its only letters, else false
     */
    private boolean isOnlyLetters(String current) {
        char[] chars = current.toCharArray();
        for (char c : chars) {
            if(!Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }


    private String change_to_price(String current, String current2, String current3,String current4) {
        String ans = "";

        /* !!!! need to consider PRICE FRACTION for each case !!!! */

        if (current.contains("$"))
            current = current2.substring(1);

        // --- Cases 1.1,2.1: PRICE Dollars ---
        if (current2.equals("Dollars") || current2.equals("dollars")) {
            /*
            if(current.contains("m")|| current.contains("bn")){
                if(current.contains("m")){
                    current = current.substring(0,current.length()-1);
                    return current + " M Dollars";
                }
                if(current.contains("bn")){
                    current = current2.substring(0,current.length()-1);
                    return current + "000 M Dollars";
                }
            */
            current = ChangeToPriceNum(current);
            return current + " Dollars";
        }

        // --- Cases 2.3,2.4,2.5,2.8,2.9.2.10: PRICE_WITHOUT_DOT billion/million/trillion U.S. dollars ---
        if (current2.equals("billion") || current2.equals("million") || current2.equals("trillion") && !current.contains(".")) {
            if (current2.equals("trillion")) {
                current = current + "000000 M Dollars";
                return current;
            }
            if (current2.equals("billion")) {
                current = current + "000 M Dollars";
                return current;
            }
            if (current2.equals("million")) {
                current = current + " M Dollars";
                return current;
            }
        }
        else {
            // --- Cases 2.3,2.4,2.5,2.8,2.9.2.10: PRICE_WITH_DOT billion/million/trillion U.S. dollars ---
            if (current2.equals("billion") || current2.equals("million") || current2.equals("trillion") && current.contains(".")) {
                int Count = 0;
                if (current2.equals("trillion")) {
                    String[] arr = current.split(".");
                    Count = arr[1].length();
                    if (Count == 1)
                        current = arr[0] + arr[1] + "00000";
                    if (Count == 2)
                        current2 = arr[0] + arr[1] + "0000";
                    if (Count == 3)
                        current = arr[0] + arr[1] + "000";
                    if (Count == 4)
                        current = arr[0] + arr[1] + "00";
                    if (Count == 5)
                        current = arr[0] + arr[1] + "0";
                    if (Count == 6)
                        current = arr[0] + arr[1];
                    current = current + " M Dollars";
                    return current;
                }
                if (current2.equals("billion")) {
                    String[] arr = current.split(".");
                    Count = arr[1].length();
                    if (Count == 1)
                        current = arr[0] + arr[1] + "00";
                    if (Count == 2)
                        current2 = arr[0] + arr[1] + "0";
                    if (Count == 3)
                        current = arr[0] + arr[1];
                    if (Count == 4)
                        current = current + " M Dollars";
                    return current;
                }
                if (current2.equals("million")) {
                    String[] arr = current.split(".");
                    current = current + " M Dollars";
                    return current;
                }
            }
        }
        // --- Cases 1.3,2.2: $PRICE ---
        if (!current2.equals("Dollars") && !current2.equals("dollars") && !current3.equals("Dollars") && !current3.equals("dollars") &&
                !current4.equals("Dollars") && !current4.equals("dollars")) {
            return ChangeToPriceNum(current) + " Dollars";
        }

       /* //1. "$450,000"
        if (!added) {
            if (Currant.contains("$")) {
                if (isLessThenMill(Currant)) {
                    Currant = Currant + " Dollars";
                    parsed.add(Currant);
                    added = true;
                } else {
                    if (!added) {
                        //2. $450,000,000
                        Currant = changeMillForPrice(Currant);
                        Currant = Currant + " Dollars";
                        parsed.add(Currant);
                    }
                }
            }
        }
        if (!added) {
            //3. 345,000 Dollars
            if (isLessThenMill(Currant))
                Currant = changeMillForPrice(Currant);
            if (counter + 1 < AfterSplit.length) {
                if (AfterSplit[counter + 1].equals("Dollars")) {
                    Currant = Currant + " Dollars";
                    counter++;
                    parsed.add(Currant);
                    added = true;
                }
            }
        }
        if (!added) {
            //4. 22 2/3 Dollars
            if (counter + 2 < AfterSplit.length) {
                if (AfterSplit[counter + 1].contains("/") && AfterSplit[counter + 2].equals("Dollars")) {
                    if (isLessThenMill(Currant))
                        Currant = changeMillForPrice(Currant);
                    Currant = Currant + " " + AfterSplit[counter + 1] + " Dollars";
                    counter = counter + 1;

                }
            }

            if (counter + 1 < AfterSplit.length) {
                if (AfterSplit[counter + 1].equals("$")) {
                    Currant = Currant + " Dollars";
                    counter++;
                    parsed.add(Currant);
                    added = true;
                }
            }
        }
    }
    */
        return ans;
    }

    /**this function changes the string from regular form to mill form if needed
     * @param current is the string need to change
     * @return the string after changed.
     */
    private String ChangeToPriceNum(String current) {
        String ans = "";
        String[] nums = current.split(",");
        if(nums.length >= 3){
            ans = changeNumToRegularNum(current);
            if(ans.contains("M")) {
                ans = ans.substring(0,ans.length()-1);
                ans = ans + " M";
            }
            if(ans.contains("B")){
                ans =ans.substring(0,ans.length()-1);
                if(ans.contains(".")){
                    String[] arr = ans.split(".");
                    String temp = arr[1].substring(2);
                    ans = arr[0]+arr[1].charAt(0)+arr[1].charAt(1)+arr[1].charAt(2)+"."+temp + " M";
                }
                else{
                    ans = ans + "000 M";
                }
            }
        }
        else {
            return current;
        }
        return ans;
    }

    /** this function checks if the current string should be a price number.
     * @param current
     * @param p1
     * @param current3
     * @return true if the current string should be consider as a price.
     */
    private boolean checkIfMoney(String current, String p1, String current3,String current4) {
        boolean ans = false;
        if (current.contains("$") || p1.equals("Dollars") || current3.equals("Dollars") || current4.equals("Dollars") ||
                p1.equals("dollars") || current3.equals("dollars")
                || current4.equals("dollars"))
            ans = true;
        return ans;
    }

    /**This function changes the number to the number as it should be in the parse rules.
     * @param current the number before the change
     * @return the number after the change.
     */
    private String changeNumToRegularNum(String current) {
        String[] nums = current.split(",");
        String signal = "";
        String ans = "";
        boolean to_change = true;
        int string_len = 0;
        // checks if the whole word is a number without any commas
        while (string_len < nums.length && to_change) {
            if (!isNumeric(nums[string_len]))
                to_change = false;
            string_len++;
        }
        // if there are elements which are not numbers
        if (to_change) {
            if (nums.length == 1)
                return current;
            if (nums.length == 2) {//Thousand
                if(nums[1].contains("$")) {
                    signal = "$";
                    nums[0] = nums[1].substring(1);
                }
                if(nums[1].contains("%")) {
                    signal = "%";
                    nums[1] = nums[1].substring(0, nums[1].length() - 1);
                }
                ans = nums[0];
                String tempString = nums[1];
                int x = 0;
                while (x <  tempString.length()) {
                    if (tempString.charAt(tempString.length() - 1) == '0')
                        tempString = tempString.substring(0, tempString.length() - 1);
                    x++;
                }
                if(tempString.equals("0")){
                    tempString = "";
                    ans = ans + tempString + 'K';
                }
                else {
                    ans = ans + '.' + tempString + 'K';
                }
            }
            if (nums.length == 3) { //million
                ans = nums[0];
                if(nums[1].contains("$")) {
                    signal = "$";
                    nums[0] = nums[1].substring(1);
                }
                if(nums[1].contains("%")) {
                    signal = "%";
                    nums[1] = nums[1].substring(0, nums[1].length() - 1);
                }
                String tempString = nums[2];
                int x = 0;
                while (x < tempString.length()) {
                    if (tempString.charAt(tempString.length() - 1) == '0')
                        tempString = tempString.substring(0, tempString.length() - 1);
                    x++;
                }
                if(tempString.equals("0"))
                    tempString = "";
                String tempString2 = nums[1];

                    int x1 = 0;
                    while (x1 < tempString2.length()) {
                        if (tempString2.charAt(tempString2.length() - 1) == '0')
                            tempString2 = tempString2.substring(0, tempString2.length() - 1);
                        x1++;
                    }
                    if(tempString2.equals("0"))
                        tempString2 = "";
                    if (tempString2.equals(""))
                        ans = nums[0] + 'M';
                    else {
                        ans = nums[0] + '.' + tempString2 + 'M';
                    }

                if(tempString.length() == 3 && tempString2.length() == 3 ) {
                    ans = nums[0] + '.' + nums[1] + nums[2] + 'M';
                }
                if(tempString.length() != 0 )
                    ans = nums[0] + '.' + nums[1] + tempString + 'M';
                else if(tempString.length() == 0) {
                    ans = nums[0] + '.' + tempString2 + tempString +'M';
                }


            }
            if (nums.length == 4) { //billion
                if(nums[1].contains("$")) {
                    signal = "$";
                    nums[0] = nums[0].substring(1);
                }
                if(nums[1].contains("%")) {
                    signal = "%";
                    nums[1] = nums[1].substring(0, nums[1].length() - 1);
                }
                String tempString = nums[1];
                int x = 0;
                while (x < tempString.length()) {
                    if (tempString.charAt(tempString.length() - 1) == '0')
                        tempString = tempString.substring(0, tempString.length() - 1);
                    x++;
                }
                String tempString2 = nums[2];
                if (tempString.equals("")) {
                    int x1 = 0;
                    while (x1 < tempString2.length()) {
                        if (tempString2.charAt(tempString2.length() - 1) == '0')
                            tempString2 = tempString2.substring(0, tempString2.length() - 1);
                        x1++;
                    }
                    if (tempString2.equals("")) {
                        String tempString3 = nums[3];
                        int x12 = 0;
                        while (x12 < tempString3.length()) {
                            if (tempString3.charAt(tempString3.length() - 1) == '0')
                                tempString3 = tempString3.substring(0, tempString3.length() - 1);
                            x12++;
                        }
                        if (tempString3.equals(""))
                            ans = nums[0] + 'B';
                        else {
                            ans = nums[0] + ".000000" + tempString3 + 'B';
                        }
                    } else {
                        ans = nums[0] + ".000" + nums[2];
                        String tempString3 = nums[3];
                        int x12 = 0;
                        while (x12 < tempString3.length()) {
                            if (tempString3.charAt(tempString3.length() - 1) == '0')
                                tempString3 = tempString3.substring(0, tempString3.length() - 1);
                            x12++;
                        }
                        if (tempString3.equals(""))
                            ans = ans + 'B';
                        else
                            ans = ans + tempString3 + 'B';
                    }
                } else {
                    int x1 = 0;
                    while (x1 < tempString2.length()) {
                        if (tempString2.charAt(tempString2.length() - 1) == '0')
                            tempString2 = tempString2.substring(0, tempString2.length() - 1);
                        x1++;
                    }
                    if(tempString2.equals("0"))
                        tempString2 = "";
                        if (tempString2.equals("")) {
                            String tempString3 = nums[3];
                            int x12 = 0;
                            while (x12 < tempString3.length()) {
                                if (tempString3.charAt(tempString3.length() - 1) == '0')
                                    tempString3 = tempString3.substring(0, tempString3.length() - 1);
                                x12++;
                            }
                            if(tempString3.equals("0"))
                                tempString3 = "";
                            if (tempString3.equals(""))
                                ans = nums[0] + '.' + tempString + 'B';
                            else {
                                ans = nums[0] + '.' + nums[1] + nums[2] + tempString3 + 'B';
                            }
                        } else {
                            String tempString3 = nums[3];
                            int x12 = 0;
                            while (x12 < tempString3.length()) {
                                if (tempString3.charAt(tempString3.length() - 1) == '0')
                                    tempString3 = tempString3.substring(0, tempString3.length() - 1);
                                x12++;
                            if(tempString3.equals("0"))
                                tempString3 = "";
                            }
                            if (tempString3.equals("")) {
                                ans = nums[0] + '.' + nums[1] + tempString2 + 'B';
                            } else {
                                ans = nums[0] + '.' + nums[1] + nums[2] + tempString3 + 'B';
                            }
                        }


                }
            }

        }
        ans = signal + ans;
        return ans;
    }

    /** This function used when the string is a price string and the price is higher then one million, and change the string price
     * according to the directions.
     * @param current is the string that we need to be changed.
     * @return the string changed according to the rules given.
     */
    private String changeMillForPrice(String current) {
        String Toreturn = "" ;
        if(current.contains("$")) {
            if(current.endsWith("$"))
                current = current.substring(0,current.length()-1);
            else{
                current = current.substring(1);
            }

        }
        return Toreturn;
    }

    /**This function returns True if the string given contains number that smaller then one million
     * @param current is the string given
     * @return true if the number is smaller then one million
     */
    private boolean isLessThenMill(String current) {
        boolean ans = false;
        return ans;
    }

    /** this function checks if the string need to change in cases of lower case/upper case and change him if needed.
     * @param current the string that we want to check his variations
     * @return the string after check the variation of thw string and change if needed.
     */
    private String ChangeStringOrNot(String current) {
        String Toreturn = "";
        return Toreturn;
    }

    /** this function returns if the string given is a number or not
     * @param str is the string that we want to check
     * @return true if the string given is a number.
     */
    public static boolean isNumeric(String str)
    {
        /* Might change to take less time */
        try
        {
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }

    /** this function returns if the string given is a number or not
     * @param str is the string that we want to check
     * @return true if the string given is a number.
     */
    public static boolean isNumeric2(String str){
        boolean ans = false;
        // TODO Might change to take less time
        if(str.contains("0") || str.contains("1")||str.contains("2") || str.contains("3") || str.contains("4")
                || str.contains("5") || str.contains("6") || str.contains("7") || str.contains("8") || str.contains("9")){
            ans = true;
        }
        return ans;
    }

    /**
     * stops creating the term lists
     */
    static void stop() {
        stop = true;
    }

    @Override
    public void run() {
        parseAll();
    }

    public static void main(String[] args) {
        String sTry = "of ]an [unidentified poll made in May 1993. The approval/disapproval \n" +
                "   ratings, in\\percent, \"for_ten ;Macedonian politicians were:";
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        String toDelete = "[?!:+{*}|<=>\"\\s;()_&\\\\\\[\\]]+";
        String[] AfterSplit = s.split(toDelete);
        System.out.println(Arrays.toString(AfterSplit));
    }
}

