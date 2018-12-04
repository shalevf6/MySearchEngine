package Part_1;

import GeneralClasses.Document;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * this class parses through the documents and creates a term list and a dictionary for every document
 */
public class Parse implements Runnable {

    static public boolean stemming;
    static private boolean stop = false;
    static BlockingQueue<Document> docQueue = new ArrayBlockingQueue<>(3000);
    private HashMap<String, short[]> currentTermDictionary;
    private HashMap<String, Integer> StopWords;
    private String[] afterSplit;
    private int afterSplitLength;
    private Stemmer stemmer;
    private short max_tf;
    private boolean dollar;
    private int docPart;
    private int firstPartCounter;

    /**
     * a constructor for the Parse class
     *
     * @param path - the path to the stop words file
     */
    public Parse(String path) {
        getStopWords(path);
    }

    /**
     * This function generates the stop words Dictionary to Array List.
     *
     * @param path is the location of the Dictionary
     */
    private void getStopWords(String path) {
        StopWords = new HashMap<>();
        File stopWordsFile = new File(path);
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(stopWordsFile));
            String stopWord = bufferedReader.readLine();
            while (stopWord != null) {
                stopWord = stopWord.trim();
                StopWords.put(stopWord, 1);
                stopWord = bufferedReader.readLine();
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * This is the main Parse Function
     */
    private void parseAll() {
        while (true) {
            if (!docQueue.isEmpty()) {
                if (stemming)
                    stemmer = new Stemmer();
                currentTermDictionary = new HashMap<>();
                max_tf = 0;
                dollar = false;
                docPart = 1;
                boolean turnToDocument = true;
                boolean turnToTitle = true;
                boolean turnToDate = true;
                Document document = null;
                try {
                    document = docQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(document.getDocId());
                String[] documents = document.getDocText();
                if (documents == null)
                    turnToDocument = false;
                String documentTitle = document.getDocTitle();
                if (documentTitle == null)
                    turnToTitle = false;
                String documentDate = document.getDocDate();
                if (documentDate == null)
                    turnToDate = false;
                String documentCity = document.getCity();
                if (documentCity != null) {
                    short[] cityData = new short[4];
                    cityData[0] = 1;
                    currentTermDictionary.put(documentCity, cityData);
                }
                // in order to go through both the document's text and the document title
                while (turnToDocument || turnToTitle) {
                    // means now we parse through the document's title
                    if (!turnToDocument) {
                        documents = new String[1];
                        documents[0] = documentTitle;
                        docPart = 2;
                    }
                    for (String data : documents) {
                        String current;
                        int counter = 0;
                        // splits the text before parsing
                        afterSplit = data.split("(?!,[0-9])[?!:,#`@^~&+{*}|<=>\"\\s;()_\\\\\\[\\]]+");
                        afterSplitLength = afterSplit.length;
                        if (turnToDocument) {
                            firstPartCounter = afterSplitLength / 10;
                            firstPartCounter++;
                        } else
                            firstPartCounter = 0;
                        int countWord = 0;

                        // removes any extra delimiters in the start / end of every word
                        while (countWord < afterSplitLength - 1) {
                            afterSplit[countWord] = removeExtraDelimiters(afterSplit[countWord]);
                            countWord++;
                        }

                        // goes through every word in the document
                        while (counter <= afterSplitLength - 1) {
                            firstPartCounter--;
                            current = afterSplit[counter];
                            String currentLower = current.toLowerCase();
                            // checks if the current string is a stop word (and not the word between) or the word may
                            if (current.equals("") || (!(currentLower.equals("may")) && !(currentLower.equals("between")) &&
                                    StopWords.containsKey(currentLower))) {
                                counter++;
                            } else {

                                // checks if there aren't any numbers in the word
                                if (!isNumeric2(current)) {

                                    // checks if its not worth checking
                                    if (current.length() == 1) {
                                        counter++;
                                        continue;
                                    }

                                    // ------- ENDS WITH 'S CHECK -------
                                    if (current.endsWith("'s"))
                                        current = current.substring(0, current.length() - 2);

                                    // ------- 'BETWEEN NUMBER AND NUMBER' CHECK -------
                                    // checks if the 1st word is "between"
                                    if (currentLower.equals("between")) {
                                        int toAdd = handleBetweenNumberAndNumber(counter);
                                        if (toAdd > 0) {
                                            counter = counter + toAdd;
                                            continue;
                                        } else {
                                            counter++;
                                            continue;
                                        }
                                    }

                                    // ------- 'WORD-WORD' | 'WORD-WORD-WORD' CHECK -------
                                    if (current.contains("-")) {
                                        handleWordsWithDash(current);
                                        counter++;
                                        continue;
                                    }

                                    // ------- 'WORD/WORD' CHECK -------
                                    if (current.contains("/")) {
                                        String[] orSplit = current.split("/");
                                        for (int i = 0; i < orSplit.length; i++) {
                                            orSplit[i] = removeExtraDelimiters(orSplit[i]);
                                            if (orSplit[i].endsWith("'s") && !orSplit[i].equals(""))
                                                orSplit[i] = orSplit[i].substring(0, orSplit[i].length() - 2);
                                            if (!StopWords.containsKey(orSplit[i].toLowerCase()))
                                                checkFurtherSplits(orSplit[i]);
                                        }
                                        counter++;
                                        continue;
                                    }

                                    // ------- 'MONTH YEAR' and 'MONTH DD' CHECK -------
                                    if (counter + 1 < afterSplitLength) {
                                        if (handleMonthYearOrMonthDay(currentLower, counter)) {
                                            counter = counter + 2;
                                            continue;
                                        }
                                    }

                                    // ------- 'may' STOP WORD MISS -------
                                    if (currentLower.equals("may")) {
                                        counter++;
                                        continue;
                                    }

                                    // ------- CAPITAL LETTERS CHECK -------
                                    if (isOnlyLetters(current)) {
                                        handleAllLetters(current);
                                        counter++;
                                        continue;
                                    }

                                    // ------- INITIALS WITH DOT (.) CHECK -------
                                    if (current.contains(".")) {
                                        if (handleInitials(current)) {
                                            counter++;
                                            continue;
                                        }
                                    }

                                    // means its a different/empty letter case
                                    String[] moreWords = current.split("[.'%$]+");
                                    for (int i = 0; i < moreWords.length; i++) {
                                        if (moreWords[i].endsWith("'s") && !moreWords[i].equals(""))
                                            moreWords[i] = moreWords[i].substring(0, moreWords[i].length() - 2);
                                        if (!moreWords[i].equals("") && !StopWords.containsKey(moreWords[i]))
                                            updateDictionaries(current.toLowerCase());
                                    }
                                    counter++;
                                }
                                // means it's a number:
                                else {
                                    String current2 = "";
                                    String current3 = "";
                                    String current4 = "";

                                    if (counter + 1 < afterSplit.length)
                                        current2 = afterSplit[counter + 1];
                                    if (counter + 2 < afterSplit.length)
                                        current3 = afterSplit[counter + 2];
                                    if (counter + 3 < afterSplit.length)
                                        current4 = afterSplit[counter + 3];
                                    // checks if the number is the whole word
                                    // ---case 0:inValid String -------
                                    if (!CheckIfValidString(current)) {
                                        if (checkNumberEnding(current.toLowerCase())) {
                                            String wordNum = current.substring(0, current.length() - 2);
                                            if (isNumeric(wordNum)) {
                                                updateDictionaries(wordNum);
                                                counter++;
                                                continue;
                                            }
                                        }
                                        updateDictionaries(current.toLowerCase());
                                        counter++;
                                        continue;
                                    }
                                    // ---case 0.5:contains dash ------//
                                    //-------ONE DASH ONLY-------------//
                                    boolean HowMuchToChange = HandleDashwithNums(current);
                                    if (current.contains("-") && HowMuchToChange || HandleDashwithNums(current2) && current2.contains("-")) {
                                        int ToAddToCounter = HandelDashNUms(current, current2, current3, current4, counter);
                                        if (ToAddToCounter != 0) {
                                            counter = counter + ToAddToCounter;
                                            continue;
                                        }
                                    }
                                    //--------MORE THEN ONE DASH---------//
                                    if (!HowMuchToChange && current.contains("-")) {
                                        String[] splitedByDash = current.split("-");
                                        handleSplitedCase(splitedByDash);
                                        counter++;
                                        continue;
                                    }
                                    if (!isNumeric(current) && !current.contains(",") || current.contains("$") || current2.toLowerCase().equals("dollars") ||
                                            current2.toLowerCase().equals("percentage") || current2.toLowerCase().equals("percent") ||
                                            current3.toLowerCase().equals("dollars") || current4.toLowerCase().equals("dollars")) {

                                        // -------MONEY&&PERCENTAGE CHECK--------//

                                        int Add2Counter = HandleMoneyAndPercentage(current, current2, current3, current4, counter);
                                        if (Add2Counter != 0) {
                                            counter = counter + Add2Counter;
                                            continue;
                                        }
                                    } else {
                                        // ------- NUMBER CHECK -------

                                        // ------- 'DD MONTH' and 'DD MONTH YEAR' CHECK
                                        if (isNumeric(current) || checkNumberEnding(current.toLowerCase())) {
                                            int toAdd = handleDayMonthOrDayMonthYear(current, current2, current3);
                                            if (toAdd != 0) {
                                                counter = counter + toAdd;
                                                continue;
                                            }
                                        }
                                        if (current2.toLowerCase().equals("billion")) {
                                            current = current + "B";
                                            counter++;
                                        }
                                        if (current2.toLowerCase().equals("Trillion")) {
                                            current = current + "T";
                                            counter++;
                                        }
                                        if (current2.toLowerCase().equals("million")) {
                                            current = current + "M";
                                            counter++;
                                        }
                                        if (current2.toLowerCase().equals("thousand")) {
                                            current = current + "K";
                                            counter++;
                                        }
                                        int regularNumCheck = RegularNumCheck(current, counter); //  CHECK CURRENT2 = MILLION \ BILLION \ TRILLION \ THOUSAND
                                        if (regularNumCheck != 0) {
                                            counter = counter + regularNumCheck;
                                            continue;
                                        }
                                    }

                                    // ------- FRACTION CHECK -------
                                    if (isNumeric2(current)) {
                                        if (notFraction(current)) {
                                            updateDictionaries(current.toLowerCase());
                                            counter++;
                                        }
                                    }

                                    //-------CONTAINS SLASH BUT MORE THEN ONE------//
                                    if (current.contains("/") && notFraction(current)) {
                                        String[] splitedBySlash = current.split("/");
                                        handleSplitedCase(splitedBySlash);
                                        counter++;
                                        continue;
                                    }

                                    dollar = false;

                                    // means its a different/empty case
                                    String[] moreWords = current.split("[.'\\-/%$]+");
                                    for (int i = 0; i < moreWords.length; i++) {
                                        moreWords[i] = removeExtraDelimiters(moreWords[i]);
                                        if (!moreWords[i].equals("") && moreWords[i].endsWith("'s"))
                                            moreWords[i] = moreWords[i].substring(0, moreWords.length - 2);
                                        if (!moreWords[i].equals("") && !StopWords.containsKey(moreWords[i]))
                                            updateDictionaries(current.toLowerCase());
                                    }
                                    counter++;
                                }
                            }
                        }
                    }
                    if (turnToDocument)
                        turnToDocument = false;
                    else
                        turnToTitle = false;
                }
                // adds the date of the document to the dictionary
                if (turnToDate) {
                    String[] dateSplit = documentDate.split("(?!,[0-9])[?!:,#@^&+{*}|<=>\"\\s;()_\\\\\\[\\]]+");
                    int count = 0;
                    int dateSplitLength = dateSplit.length;
                    // removes any extra delimiters in the start / end of every word
                    while (count < dateSplitLength - 1) {
                        dateSplit[count] = removeExtraDelimiters(dateSplit[count]);
                        count++;
                    }
                    boolean dateExists = false;
                    if (dateSplitLength == 3) {
                        String dateSplit1 = dateSplit[0];
                        String dateSplit2 = dateSplit[1];
                        String dateSplit3 = dateSplit[2];
                        if (isNumeric(dateSplit1) && Integer.valueOf(dateSplit1) <= 31) {
                            String monthNumber = getMonthNumber(dateSplit2.toLowerCase());
                            if (!monthNumber.equals("00") && isNumeric(dateSplit3)) {
                                String date = dateSplit3 + "-" + monthNumber + "-" + dateSplit1;
                                updateDictionariesOutsideText(date);
                                String monthName = getMonthNameForDictionary(monthNumber);
                                if (Indexer.termDictionary.containsKey(monthName)) {
                                    int[] corpusTermData = Indexer.termDictionary.get(monthName);
                                    if (currentTermDictionary.containsKey(monthName.toLowerCase())) {
                                        short[] termData = currentTermDictionary.get(monthName.toLowerCase());
                                        termData[0] = (short) (termData[0] + 1);
                                        if (max_tf < termData[0])
                                            max_tf = termData[0];
                                    }
                                    else {
                                        short[] termData = new short[4];
                                        termData[0] = 1;
                                        if (max_tf < 1)
                                            max_tf = 1;
                                        currentTermDictionary.put(monthName.toLowerCase(), termData);
                                        corpusTermData[0] = corpusTermData[0] + 1;
                                    }
                                    corpusTermData[1] = corpusTermData[1] + 1;
                                }
                                else {
                                    int[] corpusTermData = new int[3];
                                    corpusTermData[0] = 1;
                                    corpusTermData[1] = 1;
                                    short[] termData = new short[4];
                                    termData[0] = 1;
                                    if (max_tf < 1)
                                        max_tf = 1;
                                    currentTermDictionary.put(monthName.toLowerCase(), termData);
                                    Indexer.termDictionary.put(monthName, corpusTermData);
                                }
                                updateDictionariesOutsideText(monthNumber + "-" + dateSplit1);
                                document.setDocDate(date);
                                dateExists = true;
                            }
                        }
                    }
                    if (!dateExists) {
                        document.setDocDate(null);
                    }
                }
                // adds the document's dictionary to the document and adds the document into the Indexer queue
                document.setTfAndTermDictionary(currentTermDictionary, max_tf);
                // frees more memory by deleting the actual text of the document
                document.deleteDocumentText();
                try {
                    Indexer.docQueue.put(document);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                if (stop) {
                    Indexer.stop();
                    break;
                }
            }
        }
    }

    /**
     * adds a new (or existing) term that were found outside the text tage to the corpus and document dictionaries
     * @param current - the current term to be added to the dictionaries
     */
    private void updateDictionariesOutsideText(String current) {
        if (!current.equals("")) {
            short[] termData;
            int[] corpusTermData;
            if (Indexer.termDictionary.containsKey(current)) {
                corpusTermData = Indexer.termDictionary.get(current);
                if (currentTermDictionary.containsKey(current)) {
                    termData = currentTermDictionary.get(current);
                    short tf = termData[0];
                    termData[0] = (short) (tf + 1);
                    if (max_tf < tf + 1)
                        max_tf = (short) (tf + 1);
                } else {
                    termData = new short[4];
                    termData[0] = 1;
                    if (max_tf < 1)
                        max_tf = 1;
                    currentTermDictionary.put(current, termData);
                    corpusTermData[0] = corpusTermData[0] + 1;
                }
                corpusTermData[1] = corpusTermData[1] + 1;
            } else {
                termData = new short[4];
                termData[0] = 1;
                if (max_tf < 1)
                    max_tf = 1;
                currentTermDictionary.put(current, termData);
                corpusTermData = new int[3];
                corpusTermData[0] = 1;
                corpusTermData[1] = 1;
                Indexer.termDictionary.put(current, corpusTermData);
            }
        }
    }

    /**
     * handles the case after we split a string that had '-' or '/' in it
     * @param splitedArray - a given splited string array
     */
    private void handleSplitedCase(String[] splitedArray) {
        for (int i = 0; i < splitedArray.length; i++) {
            splitedArray[i] = removeExtraDelimiters(splitedArray[i]);
            if (splitedArray[i].endsWith("'s") && !splitedArray[i].equals(""))
                splitedArray[i] = splitedArray[i].substring(0, splitedArray[i].length() - 2);
            if (!splitedArray[i].equals("")) {
                if (isNumeric2(splitedArray[i]) && !CheckIfValidString(splitedArray[i])) {
                    splitedArray[i] = changeNumToRegularNum(splitedArray[i]);
                    updateDictionaries(splitedArray[i]);
                } else {
                    //------MEANS IT'S A WORD---------//
                    // checks if the right part is a word
                    if (isOnlyLetters(splitedArray[i])) {
                        // checks if it's not a stop word to add it alone to the dictionary
                        if (!StopWords.containsKey(splitedArray[i].toLowerCase())) {
                            handleAllLetters(splitedArray[i]);
                        }
                    } else {
                        // if it's not a letter, maybe there is a delimiter in it
                        checkFurtherSplits(splitedArray[i]);
                    }
                }
            }
        }
    }

    /**
     * checks if a given string is an initials string (like U.S.)
     * @param current - a given string
     * @return - true if there only capital letters between the dots. else - false
     */
    private boolean handleInitials(String current) {
        String[] initialsSplit = current.split("\\.");
        if (initialsSplit.length >= 2) {
            for (String initialChar : initialsSplit) {
                if (!isOnlyLetters(initialChar) || initialChar.length() != 1 || !initialChar.equals(initialChar.toUpperCase()))
                    return false;
            }
            short[] termData;
            // if the term exists in the document's dictionary, update the tf count
            if (currentTermDictionary.containsKey(current)) {
                existsInDocumentDictionaryCase(current);
                int[] corpusTermData = Indexer.termDictionary.get(current);
                corpusTermData[1] = corpusTermData[1] + 1;
            }
            // enter the new term entry to the document's dictionary
            else {
                termData = new short[4];
                termData[0] = 1;
                if (max_tf < 1)
                    max_tf = 1;
                termData[docPart] = 1;
                if (firstPartCounter > 0)
                    termData[3] = 1;
                currentTermDictionary.put(current, termData);
                // enter the new term to the corpus's dictionary
                if (!Indexer.termDictionary.containsKey(current))
                    Indexer.termDictionary.put(current, new int[]{1,1,0});
                // update the df of the term in the corpus's dictionary
                else {
                    int[] corpusTermData = Indexer.termDictionary.get(current);
                    corpusTermData[0] = corpusTermData[0] + 1;
                    corpusTermData[1] = corpusTermData[1] + 1;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * handles a string that exists in documentDictionary and updates the dictionary accordingly
     * @param current - a given string
     */
    private void existsInDocumentDictionaryCase(String current) {
        short[] termData = currentTermDictionary.get(current);
        short tf = termData[0];
        termData[0] = (short) (tf + 1);
        termData[docPart] = 1;
        if (max_tf < tf + 1)
            max_tf = (short) (tf + 1);
        if (firstPartCounter > 0)
            termData[3] = 1;
    }

    private int RegularNumCheck(String current, int counter) {
        int toReturn = 0;
        String current2;
        String current3;
        String current4;

        if(current.contains("B")&& !current.contains("\\.")){
            current =current.substring(0,current.length()-1);
            current = current + ",000,000,000";
        }
        if(current.contains("M")&& !current.contains("\\.")){
            current =current.substring(0,current.length()-1);
            current = current + ",000,000";
        }
        if(current.contains("B")&& !current.contains("\\.")){
            current =current.substring(0,current.length()-1);
            current = current + ",000,000,000";
        }
        if(current.contains("K")&& !current.contains("\\.")){
            current =current.substring(0,current.length()-1);
            current = current + ",000";
        }
        if(current.contains("T")&& !current.contains("\\.")){
            current =current.substring(0,current.length()-1);
            current = current + ",000,000,000,000";
        }
        if(current.contains("\\.") && (current.contains("B")||current.contains("M")||current.contains("T")||current.contains("K"))){
            Double temp = 0.0;
            if(current.contains("K")) {
                temp = Double.parseDouble(current);
                temp = temp * 1000;
            }
            if(current.contains("M")) {
                temp = Double.parseDouble(current);
                temp = temp * 1000000;
            }
            if(current.contains("B")) {
                temp = Double.parseDouble(current);
                temp = temp * 1000000000;
            }
            if(current.contains("T")) {
                temp = Double.parseDouble(current);
                temp = temp * 1000000000*1000;
            }
                int temp2 = temp.intValue();
                current = String.valueOf(temp2);
                if(temp2 < 1000000) {
                    temp = temp/1000;
                    current = String.valueOf(temp);
                    current = current+"K";
                }
                 else if(temp >=1000000000){
                    temp = temp/1000000000;
                    current =String.valueOf(temp);
                    current = current +"B";
                }
                else {
                    temp = temp/1000000;
                    current= String.valueOf(temp);
                    current = current + "M";
                }
                toReturn++;
            }
        if (current.contains(",") )
            current = changeNumToRegularNum(current);
        if (counter < afterSplit.length - 1)
            current2 = afterSplit[counter + 1];
        else
            current2 = "";
        if (isNumeric2(current)) {
            if (notFraction(current2)) {
                current = current + " " + current2;
                updateDictionaries(current.toLowerCase());
                return 2;
            }
            updateDictionaries(current);
            toReturn++;
        }
        return toReturn;
    }

    private String handleMoreThenOneNumber(String current, int counter) {
        int toReturn = 0;
        String current2;
        String current3;
        String current4;
        if(current.contains("B")&& !current.contains("\\.")){
            current =current.substring(0,current.length()-1);
            current = current + ",000,000,000";
        }
        if(current.contains("M")&& !current.contains("\\.")){
            current =current.substring(0,current.length()-1);
            current = current + ",000,000";
        }
        if(current.contains("B")&& !current.contains("\\.")){
            current =current.substring(0,current.length()-1);
            current = current + ",000,000,000";
        }
        if(current.contains("K")&& !current.contains("\\.")){
            current =current.substring(0,current.length()-1);
            current = current + ",000";
        }
        if(current.contains("T")&& !current.contains("\\.")){
            current =current.substring(0,current.length()-1);
            current = current + ",000,000,000,000";
        }
        if(current.contains("\\.") && (current.contains("B")||current.contains("M")||current.contains("T")||current.contains("K"))){
            Double temp = 0.0;
            if(current.contains("K")) {
                temp = Double.parseDouble(current);
                temp = temp * 1000;
            }
            if(current.contains("M")) {
                temp = Double.parseDouble(current);
                temp = temp * 1000000;
            }
            if(current.contains("B")) {
                temp = Double.parseDouble(current);
                temp = temp * 1000000000;
            }
            if(current.contains("T")) {
                temp = Double.parseDouble(current);
                temp = temp * 1000000000*1000;
            }
            int temp2 = temp.intValue();
            current = String.valueOf(temp2);
            if(temp2 < 1000000)
            {
                temp = temp/1000;
                current = String.valueOf(temp);
                current = current+"K";
            }
            else if(temp >=1000000000)
            {
                temp = temp/1000000000;
                current =String.valueOf(temp);
                current = current +"B";
            }
            else
                {
                temp = temp/1000000;
                current= String.valueOf(temp);
                current = current + "M";
            }
        }
        if (current.contains(",") )
            current = changeNumToRegularNum(current);
        return current;
        }


    private int handleFixedNumbers(String current, String current2) {
        int toReturn = 0;
        double ans;
        if(current.contains("K")){
            current =current.substring(0,current.length()-1);
            double dow =Double.parseDouble(current);
            dow = dow*1000;
            current =String.valueOf(dow);
        }
        if(current.contains("M")){
            current =current.substring(0,current.length()-1);
            double dow =Double.parseDouble(current);
            dow = dow*1000000;
            current =String.valueOf(dow);
        }
        if(current.contains("B")){
            current =current.substring(0,current.length()-1);
            double dow =Double.parseDouble(current);
            dow = dow*1000000000;
            current =String.valueOf(dow);
        }

        if (current2.toLowerCase().equals("thousand")) {
            current = current + "K";
            current = changeNumToRegularNum(current);
            toReturn++;
        }//SECOND:IF CURRENT2= MILLION
        if (current2.toLowerCase().equals("million") || current2.toLowerCase().equals("mill")) {
            current = current + "M";
            current = changeNumToRegularNum(current);
            toReturn++;
        }//THIRD:IF CURRENT2= BILLION
        if (current2.toLowerCase().equals("billion")) {
            current = current + "B";
            current = changeNumToRegularNum(current);
            toReturn++;
        }//FORTH:IF CURRENT2= TRILLION
        if (current2.toLowerCase().equals("trillion")) {

            Double temp = Double.parseDouble(current);
            temp = temp * 1000;
            int temp2 = temp.intValue();
            current = String.valueOf(temp2);
            current = current + "B";
            current = changeNumToRegularNum(current);

            toReturn++;
        }
        return toReturn;
    }

    private int HandleMoneyAndPercentage(String current, String current2, String current3, String current4, int counter) {
        int add2Counter = 0;
        // ------- PERCENTAGE CHECK -------
        // --- case 1: NUMBER% ---
        if (current.contains("%")) {
            updateDictionaries(current.toLowerCase());
            return 1;
        } else {
            if (counter + 1 < afterSplit.length) {
                // --- case 2, 3: NUMBER percent, NUMBER percentage ---
                if (afterSplit[counter + 1].toLowerCase().equals("percent") || afterSplit[counter + 1].toLowerCase().equals("percentage")) {
                    current = current + "%";
                    updateDictionaries(current.toLowerCase());
                     return 2;
                }
            }
                // ------- PRICE CHECK -------
                dollar = checkIfMoney(current, current2, current3, current4);
                // --- all cases: Price Dollars, Price Fraction Dollars, $price,....
                if (dollar) {
                    current = change_to_price(current, current2, current3, current4);
                    //current =handleDot(current);
                    updateDictionaries(current);
                    String current2Lower = current2.toLowerCase();
                    if (current2Lower.equals("dollars"))
                        add2Counter = 2;
                    if (current3.toLowerCase().equals("dollars"))
                        add2Counter = 3;
                    if (current4.toLowerCase().equals("dollars"))
                        add2Counter = 4;
                    if (current.contains("$") && current2Lower.equals("million") || current.contains("$") && current2Lower.equals("billion") ||
                            current.contains("$") && current2Lower.equals("trillion") || current.contains("$") && current2Lower.equals("thousand"))
                        add2Counter++;
                }
        }
        if(current.contains("$") || current.toLowerCase().contains("dollars"))
            add2Counter++;
        return add2Counter;
    }

    private String handleDot(String current) {
        String ToReturn = "";
        String[] SplitDot = current.split("\\.");
        if(SplitDot.length ==1)
            return current;
        else{
            ToReturn =SplitDot[0]+".";
            int i =0;
            if(SplitDot[1].length() >= 2 && SplitDot[1].charAt(1) != '0' ) {
                ToReturn = ToReturn + SplitDot[1].charAt(0) + SplitDot[1].charAt(1);
                if(ToReturn.charAt(ToReturn.length()-1) == '.')
                    ToReturn = ToReturn.substring(0,ToReturn.length() - 1);
                return ToReturn;
            }
            if(SplitDot[1].length() == 1 && SplitDot[1].charAt(0) != '0') {
                ToReturn = ToReturn + SplitDot[1].charAt(0);
                if(ToReturn.charAt(ToReturn.length()-1) == '.')
                    ToReturn = ToReturn.substring(0,ToReturn.length()-1);
                return ToReturn;
            }
            if(SplitDot[1].length() >= 2 && SplitDot[1].charAt(1) == '0' ){
                if(SplitDot[1].charAt(0)!='0'){
                    ToReturn = ToReturn+ SplitDot[1].charAt(0);
                    if(ToReturn.charAt(ToReturn.length()-1)=='.')
                        ToReturn = ToReturn.substring(0,ToReturn.length() - 1);
                    return ToReturn;
                }
                else {
                    if(ToReturn.charAt(ToReturn.length()-1) == '.')
                        ToReturn = ToReturn.substring(0,ToReturn.length() - 1);
                    return ToReturn;
                }
            }
        }
        return ToReturn;
    }

    private int HandelDashNUms(String current, String current2, String current3, String current4, int counter) {
        int ToAdd2Counter = 0;
        //------current has Dash-------//
        if (current.contains("-")) {
            String[] DashSplit = current.split("-");
            for (int i = 0; i < DashSplit.length; i++)
                DashSplit[i] = removeExtraDelimiters(DashSplit[i]);
            //-----TWO NUMBERS------//
            if (isNumeric2(DashSplit[0]) && isNumeric2(DashSplit[1])) {
                String tempCurrent = DashSplit[0];
                current = DashSplit[1];
                if (current.contains(","))
                    current = changeNumToRegularNum(current);
                else{
                    //current =handleZero(current);
                    //tempCurrent =handleZero(current);
                }
                if (isNumeric2(current)) {

                    //--------NUMBER-NUMBER FRACTION----//
                    if (notFraction(current2)) {
                        current = current + " " + current2;
                        updateDictionaries(tempCurrent + "-" + current);
                        updateDictionaries(current);
                        updateDictionaries(tempCurrent);
                        return 2;
                    }
                    if (counter < afterSplit.length - 1)
                        current2 = afterSplit[counter + 1];
                    else
                        current2 = "";
                    //--------NUMBER-NUMBER THOUSAND/MILLION/BILLION/TRILLION-------//
                    //FIRST:IF CURRENT2= THOUSAND
                    String current2Lower = current2.toLowerCase();
                    if (current2Lower.equals("thousand")) {
                        current = current + "K";
                        ToAdd2Counter++;
                    }//SECOND:IF CURRENT2= MILLION
                    if (current2Lower.equals("million") || current2Lower.equals("mill")) {
                        current = current + "M";
                        ToAdd2Counter++;
                    }//THIRD:IF CURRENT2= BILLION
                    if (current2Lower.equals("billion")) {
                        current = current + "B";
                        ToAdd2Counter++;
                    }//FORTH:IF CURRENT2= TRILLION
                    if (current2Lower.equals("trillion")) {
                        Double temp = Double.parseDouble(current);
                        temp = temp * 1000;
                        int temp2 = temp.intValue();
                        current = String.valueOf(temp2);
                        current = current + "B";
                        ToAdd2Counter = ToAdd2Counter + 1;
                    }
                    if (current.contains("M")) {
                        current = current.substring(0, current.length() - 1) + " M";
                    }
                }
                if (isNumeric2(tempCurrent) && tempCurrent.contains(","))
                    tempCurrent = changeNumToRegularNum(tempCurrent);
                updateDictionaries(tempCurrent);
                updateDictionaries(current);
                updateDictionaries(tempCurrent + "-" + current);
                return ToAdd2Counter + 1;
            }
            //------ONE NUMBER------//
            if ((isNumeric2(DashSplit[0]) || isNumeric2(DashSplit[1]))) {
                String tempCurrent = DashSplit[0];
                current = DashSplit[1];
                if (isNumeric2(tempCurrent)) {
                    changeNumToRegularNum(tempCurrent);
                    //tempCurrent = handleZero(tempCurrent);
                }
                if (isNumeric2(current)) {
                    changeNumToRegularNum(current);
                    //current = handleZero(current);
                }
                //1-----NUMBER-WORD----------1//
                if (isNumeric2(tempCurrent)) {
                    tempCurrent = changeNumToRegularNum(tempCurrent); // TODO : check it doesn't enter this if and adds tf for nothing
                    updateDictionaries(tempCurrent);
                    // checks if the right part is a word
                    if (isOnlyLetters(current)) {
                        // checks if it's not a stop word to add it alone to the dictionary
                        if (!StopWords.containsKey(current.toLowerCase())) {
                            handleAllLetters(current);
                        }
                        updateDictionaries(tempCurrent + "-" + current.toLowerCase());
                        return 1;
                    }
                    // if it's not a letter, maybe there is a delimiter in it
                    else {
                        checkFurtherSplits(current);
                    }
                }
                //2----WORD-NUMBER----------2//
                if (isNumeric2(current)) {
                    if (isNumeric2(current)) {
                        //2.1-------WORD-NUMBER FRACTION---------2.1//
                        if (notFraction(current2)) {
                            current = current + " " + current2;
                            updateDictionaries(current);
                            // checks if the left part is a word
                            if (isOnlyLetters(tempCurrent)) {
                                // checks if it's not a stop word to add it alone to the dictionary
                                if (!StopWords.containsKey(tempCurrent.toLowerCase())) {
                                    handleAllLetters(tempCurrent);
                                }
                                updateDictionaries(tempCurrent.toLowerCase() + "-" + current);
                                return 2;
                            }
                            // if it's not a letter, maybe there is a delimiter in it
                            else {
                                checkFurtherSplits(tempCurrent);
                            }
                        }
                        if (counter < afterSplit.length - 1)
                            current2 = afterSplit[counter + 1];
                        else
                            current2 = "";
                        //------WORD-NUMBER THOUSAND/MILLION/TRILLION/BILLION
                        //FIRST:IF CURRENT2= THOUSAND
                        String current2Lower = current2.toLowerCase();
                        if (current2Lower.equals("thousand")) {
                            current = current + "K";
                            ToAdd2Counter++;
                        }//SECOND:IF CURRENT2= MILLION
                        if (current2Lower.equals("million") || current2Lower.equals("mill")) {
                            current = current + "M";
                            ToAdd2Counter++;
                        }//THIRD:IF CURRENT2= BILLION
                        if (current2Lower.equals("billion")) {
                            current = current + "B";
                            ToAdd2Counter++;
                        }//FORTH:IF CURRENT2= TRILLION
                        if (current2Lower.equals("trillion")) {
                            Double temp = Double.parseDouble(current);
                            temp = temp * 1000;
                            int temp2 = temp.intValue();
                            current = String.valueOf(temp2);
                            current = current + "B";
                            ToAdd2Counter++;
                        }
                        if (current.contains("M")) {
                            current = current.substring(0, current.length() - 1) + " M";
                        }
                        // checks if the left part is a word
                        if (isOnlyLetters(tempCurrent)) {
                            // checks if it's not a stop word to add it alone to the dictionary
                            if (!StopWords.containsKey(tempCurrent.toLowerCase())) {
                                handleAllLetters(tempCurrent);
                            }
                        }
                        // if it's not a letter, maybe there is a delimiter in it
                        else {
                            checkFurtherSplits(tempCurrent);
                        }
                        updateDictionaries(tempCurrent.toLowerCase() + "-" + current);
                        updateDictionaries(current);
                        return ToAdd2Counter + 1;
                    }

                }
            }
        }

        //-------CURRENT2 HAS DASH-------//
        boolean HoeMuchToChange2 = HandleDashwithNums(current2);
        if (current2.contains("-") && HoeMuchToChange2) {
            String[] DashSplit = current2.split("-");
            for (int i = 0; i < DashSplit.length; i++)
                DashSplit[i] = removeExtraDelimiters(DashSplit[i]);
            String TempCurrent2 = DashSplit[0];
            String Temp2Current2 = DashSplit[1];
            if (isNumeric2(current)) {
                current = changeNumToRegularNum(current);
                //current = handleZero(current);
            }
            if (isNumeric2(Temp2Current2)) {
                Temp2Current2 = changeNumToRegularNum(Temp2Current2);
                //Temp2Current2 = handleZero(Temp2Current2);
            }
            if (isNumeric2(TempCurrent2)) {
                TempCurrent2 = changeNumToRegularNum(TempCurrent2);
                //TempCurrent2 = handleZero(TempCurrent2);
            }

            //-------NUMBER MILLION/THOUSAND/TRILLION/BILLION-NUMBER-------//

            if (isNumeric2(current) && isNumeric2(Temp2Current2) && !notFraction(TempCurrent2)) {
                String tempCurrent2Lower = TempCurrent2.toLowerCase();
                if (tempCurrent2Lower.equals("thousand")) {
                    current = current + " K";
                    ToAdd2Counter++;
                }//SECOND:IF CURRENT2= MILLION
                if (tempCurrent2Lower.equals("million") || tempCurrent2Lower.equals("mill")) {
                    current = current + " M";
                    ToAdd2Counter++;
                }//THIRD:IF CURRENT2= BILLION
                if (tempCurrent2Lower.equals("billion")) {
                    current = current + " B";
                    ToAdd2Counter++;
                }//FORTH:IF CURRENT2= TRILLION
                if (tempCurrent2Lower.equals("trillion")) {
                    Double temp = Double.parseDouble(current);
                    temp = temp * 1000;
                    int temp2 = temp.intValue();
                    current = String.valueOf(temp2);
                    current = current + " B";
                    ToAdd2Counter = ToAdd2Counter + 1;
                }
                updateDictionaries(current);
                updateDictionaries(Temp2Current2);
                updateDictionaries(current + "-" + Temp2Current2);
                return ToAdd2Counter + 1;
            }
            //--------NUMBER FRACTION-WORD-------//
            if (isNumeric2(current) && notFraction(TempCurrent2) && isNumeric2(Temp2Current2)) {
                String current3Lower = current3.toLowerCase();
                if (current3Lower.equals("thousand")) {
                    Temp2Current2 = Temp2Current2 + " K";
                    ToAdd2Counter++;
                }//SECOND:IF CURRENT3= MILLION
                if (current3Lower.equals("million") || current3Lower.equals("mill")) {
                    Temp2Current2 = Temp2Current2 + " M";
                    ToAdd2Counter++;
                }//THIRD:IF CURRENT3= BILLION
                if (current3Lower.equals("billion")) {
                    Temp2Current2 = Temp2Current2 + " B";
                    ToAdd2Counter++;
                }//FORTH:IF CURRENT3= TRILLION
                if (current3Lower.equals("trillion")) {
                    Double temp12 = Double.parseDouble(Temp2Current2);
                    temp12 = temp12 * 1000;
                    int temp22 = temp12.intValue();
                    counter++;
                    counter--;
                    Temp2Current2 = String.valueOf(temp22);
                    Temp2Current2 = Temp2Current2 + " B";
                    ToAdd2Counter = ToAdd2Counter + (2 - 1);
                }
                updateDictionaries(Temp2Current2);
                // checks if the right part is a word
                if (isOnlyLetters(Temp2Current2)) {
                    // checks if it's not a stop word to add it alone to the dictionary
                    if (!StopWords.containsKey(Temp2Current2.toLowerCase())) {
                        handleAllLetters(Temp2Current2);
                    }
                    updateDictionaries(current + "-" + Temp2Current2.toLowerCase());
                    return ToAdd2Counter + 1;
                }
                // if it's not a letter, maybe there is a delimiter in it
                else {
                    checkFurtherSplits(Temp2Current2);
                }
            }
        }
        return ToAdd2Counter;
    }

    /**this function changes a string given uf it's contains zero at the beginning and ther's no dots or "," for example 0023---->23
     * @param current
     * @return the number after changing without zeros in the beginning
     */
    private String handleZero(String current) {
        if(current.contains("\\.")|| current.contains(","))
            return current;
        else {
            for(int i = 0 ; i < current.length(); i++){
                if(current.charAt(i)=='0')
                    current =current.substring(1);
                else
                    return current;
            }
        }
        return current;
    }

    private boolean HandleDashwithNums(String current) {
        String[] SplitDash = current.split("-");
        if (SplitDash.length == 2) {
            return true;
        }
        return false;
    }

    /**
     * handles the 'DD MONTH' / 'DD MONTH YYYY' date cases
     *
     * @param current  - an optional DD word
     * @param current2 - an optional MONTH word
     * @param current3 - an optional YEAR word
     * @return - 0 if the case was found not to be true, 2 if the 'DD MONTH' case was true, and 3 if the 'DD MONTH YYYY' case was true
     */
    private int handleDayMonthOrDayMonthYear(String current, String current2, String current3) {
        String monthNumber = getMonthNumber(current2.toLowerCase());
        // check if the next word is a month
        if (!monthNumber.equals("00")) {
            int currentLength = current.length();
            // check if the number ends with an ordinal indicator (st, nd, rd, th) and remove it if so
            if (checkNumberEnding(current.toLowerCase())) {
                current = current.substring(0, currentLength - 2);
                currentLength = currentLength - 2;
            }
            // check if the number has 2 or less digits
            if (currentLength <= 2) {
                // check if the number's value is of a day
                int dayValue = Integer.valueOf(current);
                // adds a new day date
                if (dayValue > 31)
                    return 0;
                if (currentLength == 1) {
                    updateDictionaries(current);
                    current = "0" + current;
                }
                updateDictionaries(monthNumber + "-" + current);
                handleAllLetters(getMonthNameForDictionary(monthNumber));
                // check if there is also a year in the date term
                int current3Length = current3.length();
                if (isNumeric(current3) && current3Length <= 4 && current3Length > 1) {
                    int yearValue = Integer.valueOf(current3);
                    if (yearValue <= 31)
                        return 2;
                    String date = "";
                    // need to add the YYYY-MM term separately
                    if (current3Length == 2) {
                        date = "00" + current3 + "-" + monthNumber + "-" + current;
                        updateDictionaries("00" + current3 + "-" + monthNumber);
                    }
                    // need to add the YYYY-MM term separately
                    if (current3Length == 3) {
                        date = "0" + current3 + "-" + monthNumber + "-" + current;
                        updateDictionaries("0" + current3 + "-" + monthNumber);
                    }
                    updateDictionaries(current3);
                    updateDictionaries(date);
                    return 3;
                }
                return 2;
            }
        }
        return 0;
    }


    /**
     * handles the 'MONTH YEAR' / 'MONTH DD' date case
     *
     * @param current - a given word that might be a month
     * @param counter - the counter for the words in the split array
     * @return - true if it was found that the case was verified, else - false
     */
    private boolean handleMonthYearOrMonthDay(String current, int counter) {
        String monthNumber = getMonthNumber(current);
        // checks if the first word is a month
        if (!monthNumber.equals("00")) {
            String current2 = afterSplit[counter + 1];
            int current2Length = current2.length();
            String date;
            // checks if the second word is a number
            if (isNumeric(current2) && !current2.equals("")) {
                // check the number of characters in current2
                if (current2Length <= 2) {
                    if (current2Length == 1)
                        date = monthNumber + "-0" + current2;
                        // checks if the number in the second word is between 0 to 31 (a day's number)
                    else {
                        int dayValue = Integer.valueOf(current2);
                        // adds a new day date
                        if (dayValue <= 31 && dayValue >= 0)
                            date = monthNumber + "-" + current2;
                        else
                            // adds a new year between 32 to 99
                            date = "00" + current2 + "-" + monthNumber;
                    }
                }
                // than its a year
                else {
                    // the year is between 1000 to 9999
                    if (current2Length == 4)
                        date = current2 + "-" + monthNumber;
                        // the year is between 100 to 999
                    else {
                        date = "0" + current2 + "-" + monthNumber;
                    }
                }
                updateDictionaries(date);
                updateDictionaries(current2);
                handleAllLetters(getMonthNameForDictionary(monthNumber));
                return true;
            } else {
                if (!current2.equals("") && isNumeric(current2) && checkNumberEnding(current2.toLowerCase())) {
                    current2 = current2.substring(0, current2Length - 2);
                    if (!current2.equals("")) {
                        if (current2Length - 2 == 1) {
                            date = monthNumber + "-0" + current2;
                            updateDictionaries(date);
                            updateDictionaries(current2);
                            handleAllLetters(getMonthNameForDictionary(monthNumber));
                            return true;
                        } else {
                            if (current2Length - 2 == 2) {
                                int dayValue = Integer.valueOf(current2);
                                // adds a new day date
                                if (dayValue <= 31 && dayValue >= 0)
                                    date = monthNumber + "-" + current2;
                                else
                                    // adds a new year between 32 to 99
                                    date = "00" + current2 + "-" + monthNumber;
                                updateDictionaries(date);
                                updateDictionaries(current2);
                                handleAllLetters(getMonthNameForDictionary(monthNumber));
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * checks if the given word is a number which ends with st\nd\rd\th in capital or lower letters
     *
     * @param number - a given number
     * @return - true if the case we check is true. else - false
     */
    private boolean checkNumberEnding(String number) {
        int numberLength = number.length();
        if (numberLength > 2) {
            String numberEnd = number.substring(numberLength - 2);
            return (numberEnd.equals("th") || numberEnd.equals("st") || numberEnd.equals("nd") || numberEnd.equals("rd"));
        }
        return false;
    }

    /**
     * gets a month's name in capital letters given its number
     *
     * @param monthNumber - a given month's number
     * @return - a month's name in capital letters
     */
    private String getMonthNameForDictionary(String monthNumber) {
        if (monthNumber.equals("01"))
            return "JANUARY";
        if (monthNumber.equals("02"))
            return "FEBRUARY";
        if (monthNumber.equals("03"))
            return "MARCH";
        if (monthNumber.equals("04"))
            return "APRIL";
        if (monthNumber.equals("05"))
            return "MAY";
        if (monthNumber.equals("06"))
            return "JUNE";
        if (monthNumber.equals("07"))
            return "JULY";
        if (monthNumber.equals("08"))
            return "AUGUST";
        if (monthNumber.equals("09"))
            return "SEPTEMBER";
        if (monthNumber.equals("10"))
            return "OCTOBER";
        if (monthNumber.equals("11"))
            return "NOVEMBER";
        if (monthNumber.equals("12"))
            return "DECEMBER";
        return "";
    }

    /**
     * checks and returns the number that represents the month name in the given string
     *
     * @param monthName - the given month name
     * @return - the number that represents the month name in the given string. If it's not a month name, than returns "00"
     */
    private String getMonthNumber(String monthName) {
        if (monthName.equals("january") || monthName.equals("jan"))
            return "01";
        if (monthName.equals("february") || monthName.equals("feb"))
            return "02";
        if (monthName.equals("march") || monthName.equals("mar"))
            return "03";
        if (monthName.equals("april") || monthName.equals("apr"))
            return "04";
        if (monthName.equals("may"))
            return "05";
        if (monthName.equals("june") || monthName.equals("jun"))
            return "06";
        if (monthName.equals("july") || monthName.equals("jul"))
            return "07";
        if (monthName.equals("august") || monthName.equals("aug"))
            return "08";
        if (monthName.equals("september") || monthName.equals("sep"))
            return "09";
        if (monthName.equals("october") || monthName.equals("oct"))
            return "10";
        if (monthName.equals("november") || monthName.equals("nov"))
            return "11";
        if (monthName.equals("december") || monthName.equals("dec"))
            return "12";
        return "00";
    }

    /**
     * this function checks if the string given is a fraction
     *
     * @param s - a given string
     * @return true if the string is a fraction. else - false
     */
    private boolean notFraction(String s) {
        if (s.contains("/")) {
            String[] ans1 = s.split("/");
            if (ans1.length > 2)
                return false;
            if (ans1.length == 2) {
                if (isNumeric(ans1[0]) && isNumeric(ans1[1]))
                    return true;
            }
        }
        return false;
    }

    /**
     * handles with words with dash between them
     * @param current - a given string with a '-' delimiter smoewhere in the middle of it
     */
    private void handleWordsWithDash(String current) {
        String[] dashSplit = current.split("-");
        boolean allWords = true;
        int dashSplitLength = dashSplit.length;
        String remainingDelimiters = "['.$%/\\s]+";
        // if there are 2 or 3 words between the '-' delimiter
        if (dashSplitLength == 2 || dashSplitLength == 3) {
            for (int i = 0; i < dashSplitLength; i++) {
                dashSplit[i] = removeExtraDelimiters(dashSplit[i]);
                if (dashSplit[i].endsWith("'s") && !dashSplit[i].equals(""))
                    dashSplit[i] = dashSplit[i].substring(0, dashSplit[i].length() - 2);
                if (!StopWords.containsKey(dashSplit[i])) {
                    // if there is a delimiter in the word
                    if (!isOnlyLetters(dashSplit[i])) {
                        allWords = false;
                        String[] moreWords = dashSplit[i].split(remainingDelimiters);
                        for (int j = 0; j < moreWords.length; j++) {
                            moreWords[j] = removeExtraDelimiters(moreWords[j]);
                            if (!moreWords[j].equals(""))
                                handleAllLetters(moreWords[j]);
                        }
                        // if there is no delimiter in the word
                    } else if (!dashSplit[i].equals(""))
                        handleAllLetters(dashSplit[i]);
                }
            }
            // if all the words between the '-' delimiter are letters
            if (allWords) {
                updateDictionaries(current.toLowerCase());
            }
        }
        // if there are more than 3 words between the '-' delimiter
        else {
            for (int i = 0; i < dashSplitLength; i++) {
                if (dashSplit[i].endsWith("'s") && !dashSplit[i].equals(""))
                    dashSplit[i] = dashSplit[i].substring(0, dashSplit[i].length() - 2);
                checkFurtherSplits(dashSplit[i]);
            }
        }
    }

    /**
     * checks if there are more delimiters which we didn't check before in the given string
     *
     * @param current - a given string
     */
    private void checkFurtherSplits(String current) {
        String remainingDelimiters = "[.,'/$%\\s]+";
        String[] delimiterSplit = current.split(remainingDelimiters);
        for (String delimiterWord : delimiterSplit) {
            // check if the word is a stop word
            if (!StopWords.containsKey(delimiterWord.toLowerCase())) {
                // if the word is only letters
                if (isOnlyLetters(delimiterWord)) {
                    if (!delimiterWord.equals(""))
                        handleAllLetters(delimiterWord);
                }
                // if there are more than only letters in the word
                else {
                    if (!delimiterWord.equals(""))
                        updateDictionaries(delimiterWord.toLowerCase());
                }
            }
        }
    }

    /**
     * checks and handles the 'BETWEEN NUMBER AND NUMBER' term
     *
     * @param counter  - the counter for the words in the text
     * @return - the amount of words it checked were part of the term. If not, 0.
     */
    private int handleBetweenNumberAndNumber(int counter) {
        String current1 = "between";
        String current2;
        String current3;
        String current4;
        // checks if the 2nd word is a NUMBER
        if (counter + 1 < afterSplitLength && isNumeric2(afterSplit[counter + 1]) && haveNoletters(afterSplit[counter +1])) {
            current2 = afterSplit[counter + 1];
            // checks if the 3rd word is "and"
            if (counter + 2 < afterSplitLength && afterSplit[counter + 2].toLowerCase().equals("and")) {
                current3 = afterSplit[counter + 2];
                // checks if the 4th word is a NUMBER
                if (counter + 3 < afterSplitLength && isNumeric2(afterSplit[counter + 3])) {
                    current4 = afterSplit[counter + 3];
                    //Done!
                    if(!notFraction(current2)) {
                        current2 = changeNumToRegularNum(current2);
                    }
                    if(!notFraction(current4)) {
                        current4 = changeNumToRegularNum(current4);
                    }
                    String current3Lower = current3.toLowerCase();
                    String current5 = "";
                    String current6 = "";
                    if(counter+4 < afterSplitLength)
                        current5 = afterSplit[counter + 4];
                    if(counter+5 < afterSplitLength)
                        current6 = afterSplit[counter + 5];
                    if(notFraction(current5)) {
                        updateDictionaries(current1 + " " + current2 + " " + current3Lower + " " + current4 + " " + current5);
                        return 5;
                    }
                    String current5Lower = current5.toLowerCase();

                    if (current5Lower.equals("thousand")) {
                        current4 = current4 + " K";
                        updateDictionaries(current1 + " " + current2 + " " + current3Lower + " " + current4 );
                        return 5;
                    }//SECOND:IF CURRENT2 = MILLION
                    if (current5Lower.equals("million") || current5Lower.equals("mill")) {
                        current4 = current4 + " M";
                        updateDictionaries(current1 + " " + current2 + " " + current3Lower + " " + current4 );
                        return 5;
                    }//THIRD:IF CURRENT2 = BILLION
                    if (current5Lower.equals("billion")) {
                        current4 = current4 + " B";
                        updateDictionaries(current1 + " " + current2 + " " + current3Lower + " " + current4 );
                        return 5;
                    }//FORTH:IF CURRENT2 = TRILLION
                    if(current4.equals("L4000000000000")){
                        counter++;
                        counter--;
                    }
                    if (current5Lower.equals("trillion")) {
                        current4 =current4 + "T";
                        current4 =handleMoreThenOneNumber(current4,counter);
                        Double temp = Double.parseDouble(current4);
                        temp = temp /1000;
                        int temp2 = temp.intValue();
                        current4 = String.format ("%.0f", temp);
                        current4 = current4 + "B";
                        updateDictionaries(current1 + " " + current2 + " " + current3Lower + " " + current4 );
                        return 5;
                    }
                    updateDictionaries(current1 + " " + current2 + " " + current3Lower + " " + current4);
                    return 4;
                } else {
                    return 0;
                }
            } else {
                if(counter + 3 < afterSplitLength)
                    current4 = afterSplit[counter + 3];
                else
                    current4= "";
                current3 = "";
                if(counter+2 < afterSplitLength)
                    current3 = afterSplit[counter + 2];
                if(!isNumeric2(current3))
                    return 0;
                if(!notFraction(current3) && isNumeric2(current3))
                    current3 = changeNumToRegularNum(current3);
                if(current4.toLowerCase().equals("and")) {
                    String current4Lower = current4.toLowerCase();
                    String current5 = "";
                    current3 = "";
                    if (counter + 4 < afterSplitLength)
                        current3 = afterSplit[counter + 2];
                    if (counter + 4 < afterSplitLength)
                        current5 = afterSplit[counter + 4];
                    if (!isNumeric2(current5))
                        return 0;
                    boolean toAdd = false;
                    String current3Lower = current3.toLowerCase();
                    if (current3Lower.equals("thousand")) {
                        current2 = current2 + " K";
                        toAdd = true;
                    }//SECOND:IF CURRENT2= MILLION
                    if (current3Lower.equals("million") || current3Lower.equals("mill")) {
                        current2 = current2 + " M";
                        toAdd = true;
                    }//THIRD:IF CURRENT2= BILLION
                    if (current3Lower.equals("billion")) {
                        current2 = current2 + " B";
                        toAdd = true;
                    }//FORTH:IF CURRENT2= TRILLION
                    if (current3Lower.equals("trillion")) {
                        Double temp = Double.parseDouble(current5);
                        temp = temp * 1000;
                        int temp2 = temp.intValue();
                        current2 = String.valueOf(temp2);
                        current2 = current2 + " B";
                        toAdd = true;
                    }
                    if (notFraction(current5) && toAdd) {
                        updateDictionaries(current1 + " " + current2 + " " + current4Lower + " " + current5);
                        return 5;
                    }
                    //--------BETWEEN NUMBER FRACTION AND FRACTION--------//
                    if (notFraction(current3) && notFraction(current5)) {
                        updateDictionaries(current1 + " "  + current2 + " " + current3 + " " + current4Lower + " " + current5);
                        return 5;
                    }

                    current5 = changeNumToRegularNum(current5);
                    String current6 = "";
                    if (counter + 5 < afterSplitLength)
                        current6 = afterSplit[counter + 5];
                    String current6Lower = current6.toLowerCase();
                    if (current6Lower.equals("thousand")) {
                        current5 = current5 + " K";
                        if (toAdd) {
                            updateDictionaries(current1 + " " + current2 + " " + current3 + " " + current4Lower + " " + current5);
                            return 6;
                        }
                        if (notFraction(current3)) {
                            updateDictionaries(current1 + " " + current2 + " " + current3 + " " + current4Lower + " " + current5);
                            return 6;
                        }
                    }//SECOND:IF CURRENT2= MILLION
                    if (current6Lower.equals("million") || current6Lower.equals("mill")) {
                        current5 = current5 + " M";
                        if (toAdd) {
                            updateDictionaries(current1 + " " + current2 + " " + current3 + " " + current4Lower + " " + current5);
                            return 6;
                        }
                        if (notFraction(current3)) {
                            updateDictionaries(current1 + " " + current2 + " " + current3 + " " + current4Lower + " " + current5);
                            return 6;
                        }
                    }//THIRD:IF CURRENT2= BILLION
                    if (current6Lower.equals("billion")) {
                        current5 = current5 + " B";
                        if (toAdd) {
                            updateDictionaries(current1 + " " + current2 + " " + current3 + " " + current4Lower + " " + current5);
                            return 6;
                        }
                        if (notFraction(current3)) {
                            updateDictionaries(current1 + " " + current2 + " " + current3 + " " + current4Lower + " " + current5);
                            return 6;
                        }
                    }//FORTH:IF CURRENT2= TRILLION
                    if (current6Lower.equals("trillion")) {
                        Double temp = Double.parseDouble(current5);
                        temp = temp * 1000;
                        int temp2 = temp.intValue();
                        current5 = String.valueOf(temp2);
                        current5 = current4 + " B";
                        if (toAdd) {
                            updateDictionaries(current1 + " " + current2 + " " + current3 + " " + current4Lower + " " + current5);
                            return 6;
                        }
                        if (notFraction(current3)) {
                            updateDictionaries(current1 + " " + current2 + " " + current3 + " " + current4Lower + " " + current5);
                            return 6;
                        }
                    }
                }
                else
                    return 0;
            }
        }
        else {
            return 0;
        }
        return 0;
    }

    private boolean haveNoletters(String s) {
        boolean ans = true;
        for(int i = 0 ;i< s.length();i++){
            if(Character.isLetter(s.charAt(i)) && i== s.length()-1)
                if(s.charAt(i)!='K' ||s.charAt(i)!='B'||s.charAt(i)!='T'||s.charAt(i)!='M')
                return false;
            if(Character.isLetter(s.charAt(i)) && i!= s.length()-1)
                return false;
        }
        return ans;
    }

    /**
     * handles all letter cases for a given word
     *
     * @param current - a given word
     */
    private void handleAllLetters(String current) {
        // checks if the given string is a city
        if (Indexer.corpusCityDictionary.containsKey(current.toUpperCase())) {
            handleCityTerms(current.toUpperCase());
        }
        else {
            // all cases which the given string is not a city
            String stemmed = "";
            if (stemming) {
                stemmed = stemmer.stemTerm(current);
            }
            // --- case 1: all of the word is in lower letters ---
            if (current.toLowerCase().equals(current)) {
                if (stemming)
                    handleLowerLetters(stemmed.toLowerCase());
                else
                    handleLowerLetters(current);
            } else {
                // --- cases 2,3: only first letter of word is a capital letter || all of the word is in capital letters ---
                if ((current.toUpperCase().equals(current)) || (current.charAt(0) >= 65 && current.charAt(0) <= 90 &&
                        current.substring(1).toLowerCase().equals(current.substring(1)))) {
                    String lowerCurrent = current.toLowerCase();
                    if (stemming)
                        handleCapitalLetters(stemmed.toLowerCase());
                    else
                        handleCapitalLetters(lowerCurrent);
                }
                // --- case 4: mixed capital, lower case word - > none of the above cases ---
                else {
                    if (stemming)
                        updateDictionaries(stemmed.toLowerCase());
                    else
                        updateDictionaries(current.toLowerCase());
                }
            }
        }
    }

    /**
     * handles the city terms that needs to be added to the city dictionary
     * @param city - a given city term
     */
    private void handleCityTerms(String city) {
        short [] cityData;
        // the city is in this document's dictionary
        if (currentTermDictionary.containsKey(city)) {
            cityData = currentTermDictionary.get(city);
            short tf = cityData[0];
            cityData[0] = (short) (tf + 1);
            cityData[docPart] = 1;
            if (max_tf < tf + 1)
                max_tf = (short) (tf + 1);
            if (firstPartCounter > 0)
                cityData[3] = 1;
            int[] corpusTermData = Indexer.termDictionary.get(city);
            corpusTermData[1] = corpusTermData[1] + 1;
        }
        // the city is not in this document's dictionary
        else {
            cityData = new short[4];
            cityData[0] = 1;
            cityData[docPart] = 1;
            if (max_tf < 1)
                max_tf = 1;
            if (firstPartCounter > 0)
                cityData[3] = 1;
            currentTermDictionary.put(city, cityData);
            int[] corpusTermData = Indexer.termDictionary.get(city);
            corpusTermData[1] = corpusTermData[1] + 1;
            corpusTermData[0] = corpusTermData[0] + 1;
        }
    }

    /**
     * removes any extra delimiters from a given word's start or and twice (that we didn't remove when we split the words in the text prior)
     * except U.S.
     * @param word - a given word
     * @return - the given word after the delimiter removal (if necessary)
     */
    private String removeExtraDelimiters(String word) {
        if(!word.equals("")) {

            if(word.equals("U.S."))
                return word;

            int length = word.length();

            // checks if there is a delimiter at the start of the word
            if (word.charAt(0) == '/' || word.charAt(0) == '.' || word.charAt(0) == '-' || word.charAt(0) == '\'' || word.charAt(0) == '%') {
                return removeExtraDelimiters(word.substring(1));
            }

            // checks if there is a delimiter at the end of the word
            if (word.charAt(length - 1) == '.' || word.charAt(length - 1) == '/' || word.charAt(length - 1) == '-' || word.charAt(length - 1) == '\'' ||
                    word.charAt(length - 1) == '$')
                return removeExtraDelimiters(word.substring(0, length - 1));

            return word;
        }
        return word;
    }

    /**
     * checks whether a lower case word has been already put into the dictionary, and if so, in lower letters or capital letters,
     * and than adds the relevant form of the word (upper or lower case) to the corpus and document dictionaries
     * @param current - the lower case form of the word
     */
    private void handleLowerLetters(String current) {
        String currentUpper = current.toUpperCase();
        short[] termData;
        // word is not in corpus dictionary and is not in document dictionary
        if (!Indexer.termDictionary.containsKey(current) && !Indexer.termDictionary.containsKey(currentUpper))
            notInDictionaries(current, false);
        else {
            // word is in corpus dictionary in lower case
            if (Indexer.termDictionary.containsKey(current)) {
                // word is in document dictionary
                if (currentTermDictionary.containsKey(current)) {
                    termData = currentTermDictionary.get(current);
                    termData[docPart] = 1;
                    int tf = termData[0];
                    termData[0] = (short) (tf + 1);
                    if (max_tf < tf + 1)
                        max_tf = (short) (tf + 1);
                    if (firstPartCounter > 0)
                        termData[3] = 1;
                    int[] corpusTermData = Indexer.termDictionary.get(current);
                    corpusTermData[1] = corpusTermData[1] + 1;
                }
                // word is not in document dictionary
                else {
                    termData = new short[4];
                    termData[0] = 1;
                    termData[docPart] = 1;
                    if (max_tf < 1)
                        max_tf = 1;
                    if (firstPartCounter > 0)
                        termData[3] = 1;
                    currentTermDictionary.put(current, termData);
                    int[] corpusTermData = Indexer.termDictionary.get(current);
                    corpusTermData[0] = corpusTermData[0] + 1;
                    corpusTermData[1] = corpusTermData[1] + 1;
                }
            } else {
                // word is in corpus dictionary in upper case
                if (Indexer.termDictionary.containsKey(currentUpper)) {
                    int[] corpusTermData = Indexer.termDictionary.get(currentUpper);
                    Indexer.termDictionary.remove(currentUpper);
                    // word is not in document dictionary
                    if (!currentTermDictionary.containsKey(current)) {
                        corpusTermData[0] = corpusTermData[0] + 1;
                        termData = new short[4];
                        termData[0] = 1;
                        termData[docPart] = 1;
                        if (max_tf < 1)
                            max_tf = 1;
                        if (firstPartCounter > 0)
                            termData[3] = 1;
                        currentTermDictionary.put(current, termData);
                    }
                    // word is in document dictionary
                    else {
                        existsInDocumentDictionaryCase(current);
                    }
                    corpusTermData[1] = corpusTermData[1] + 1;
                    Indexer.termDictionary.put(current, corpusTermData);
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
        if (!Indexer.termDictionary.containsKey(current) && !Indexer.termDictionary.containsKey(currentUpper))
            notInDictionaries(current, true);
        else {
            short[] termData;
            // word is in document dictionary
            if (currentTermDictionary.containsKey(current)) {
                existsInDocumentDictionaryCase(current);
                existsInCorpusDictionary(current, currentUpper, false);
            }
            // word is not in document dictionary
            else {
                termData = new short[4];
                termData[0] = 1;
                termData[docPart] = 1;
                if (max_tf < 1)
                    max_tf = 1;
                if (firstPartCounter > 0)
                    termData[3] = 1;
                currentTermDictionary.put(current, termData);
                existsInCorpusDictionary(current, currentUpper, true);
            }
        }
    }

    /**
     * adds the given string to the corpus dictionary, depends on how it was saved before (capital or lower letters)
     * @param current - the string in lower case letters
     * @param currentUpper - the string in capital letters
     * @param upDf - to know if the df of the term in the term dictionary needs to change or not
     */
    private void existsInCorpusDictionary(String current, String currentUpper, boolean upDf) {
        int[] corpusTermData;
        // word is in corpus dictionary in capital letters
        if (Indexer.termDictionary.containsKey(currentUpper)) {
            corpusTermData = Indexer.termDictionary.get(currentUpper);
            if (upDf)
                corpusTermData[0] = corpusTermData[0] + 1;
            corpusTermData[1] = corpusTermData[1] + 1;
            Indexer.termDictionary.put(currentUpper, corpusTermData);
        }
        // word is in corpus dictionary in lower case
        else {
            corpusTermData = Indexer.termDictionary.get(current);
            if (upDf)
                corpusTermData[0] = corpusTermData[0] + 1;
            corpusTermData[1] = corpusTermData[1] + 1;
            Indexer.termDictionary.put(current, corpusTermData);
        }
    }

    /**
     * handles the case which the string isn't in any dictionary and adds it accordingly
     * @param current - a given string
     * @param isUpperCase - is the string came from a capital letter function or a lower letter function
     */
    private void notInDictionaries(String current, boolean isUpperCase) {
        short[] termData = new short[4];
        int[] corpusTermData = new int[3];
        corpusTermData[0] = 1;
        corpusTermData[1] = 1;
        termData[0] = 1;
        termData[docPart] = 1;
        if (max_tf < 1)
            max_tf = 1;
        if (firstPartCounter > 0)
            termData[3] = 1;
        currentTermDictionary.put(current, termData);
        if (isUpperCase) {
            Indexer.termDictionary.put(current.toUpperCase(), corpusTermData);

        } else {
            Indexer.termDictionary.put(current, corpusTermData);
        }
    }

    /**
     * adds a new (or existing) term to the corpus and document dictionaries
     * @param current - the current term to be added to the dictionaries
     */
    private void updateDictionaries(String current) {
        if (!current.equals("")) {
            boolean newTerm = true;
            short[] termData;
            int[] corpusTermData;
            String currentUpper = current.toUpperCase();
            if (currentTermDictionary.containsKey(current)) {
                // checks if exists in corpus dictionary in upper case letters
                if (Indexer.termDictionary.containsKey(currentUpper))
                    handleCapitalLetters(current.toLowerCase());
                else {
                    termData = currentTermDictionary.get(current);
                    short tf = termData[0];
                    termData[0] = (short) (tf + 1);
                    termData[docPart] = 1;
                    if (max_tf < tf + 1)
                        max_tf = (short) (tf + 1);
                    if (firstPartCounter > 0)
                        termData[3] = 1;
                    newTerm = false;
                    corpusTermData = Indexer.termDictionary.get(current);
                    corpusTermData[1] = corpusTermData[1] + 1;
                }
            } else {
                termData = new short[4];
                termData[0] = 1;
                termData[docPart] = 1;
                if (max_tf < 1)
                    max_tf = 1;
                if (firstPartCounter > 0)
                    termData[3] = 1;
                currentTermDictionary.put(current, termData);
            }
            if (newTerm) {
                // checks if exists in corpus dictionary in upper case letters
                if (Indexer.termDictionary.containsKey(currentUpper))
                    handleCapitalLetters(current.toLowerCase());
                else {
                    if (Indexer.termDictionary.containsKey(current)) {
                        corpusTermData = Indexer.termDictionary.get(current);
                        corpusTermData[0] = corpusTermData[0] + 1;
                        corpusTermData[1] = corpusTermData[1] + 1;
                    } else {
                        corpusTermData = new int[3];
                        corpusTermData[0] = 1;
                        corpusTermData[1] = 1;
                        Indexer.termDictionary.put(current, corpusTermData);
                    }
                }
            }
        }
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

    /**this function changes the string current to the price according to the directions
     * @param current
     * @param current2
     * @param current3
     * @param current4
     * @return the string with the real price.
     */
    private String change_to_price(String current, String current2, String current3,String current4) {
        String ans = "";
        String current2Lower = current2.toLowerCase();
        /* !!!! need to consider PRICE FRACTION for each case !!!! */

        if (current.contains("$"))
            current = current.substring(1);

        // --- Cases 1.1,2.1: PRICE Dollars ---
        if (current2Lower.equals("dollars")) {
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
            current =handleDot(current);
             current = handleZero(current);
            return current + " Dollars";
        }
        //---Case not mentioned: PRICE FRACTION Dollars -----
        if (isNumeric(current) && notFraction(current2) && (current3.toLowerCase().equals("dollars"))) {
            return current + " " + current2 + " " + "Dollars";
        }

        // --- Cases 2.3,2.4,2.5,2.8,2.9.2.10: PRICE_WITHOUT_DOT billion/million/trillion U.S. dollars ---
        if (current2Lower.equals("billion") && !current.contains(".") || current2Lower.equals("million") && !current.contains(".") ||
                current2Lower.equals("trillion") && !current.contains(".") || current2Lower.equals("bn") && !current.contains(".") ||
                current2Lower.equals("m") && !current.contains(".")) {
            if (current2Lower.equals("trillion")) {
                current = current + "000000 M Dollars";
                return current;
            }
            if (current2Lower.equals("billion") || current2Lower.equals("bn")) {
                current = current + "000 M Dollars";
                return current;
            }
            if (current2Lower.equals("million") || current2.equals("m")) {
                current = current + " M Dollars";
                return current;
            }
        } else {
            // --- Cases 2.3,2.4,2.5,2.8,2.9.2.10: PRICE_WITH_DOT billion/million/trillion U.S. dollars ---
            if (current2Lower.equals("billion") && current.contains(".") || current.contains(".") && current2Lower.equals("million") ||
                    current2Lower.equals("trillion") && current.contains(".") || current2Lower.equals("bn") && current.contains(".") ||
                    current2Lower.equals("m") && current.contains(".")) {
                int Count = 0;
                if (current2Lower.equals("trillion")) {
                    Double temp = Double.parseDouble(current);
                    temp = temp * 1000000;
                    current = String.format ("%.0f", temp);
                    current = handleDot(current);

                    /*
                    String[] arr = current.split("\\.");
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
                        */
                    current = current + " M Dollars";
                    return current;
                }
                if (current2Lower.equals("billion") || current2Lower.equals("bn")) {
                    String[] arr = current.split("\\.");
                    Count = arr[1].length();
                    if (Count == 1)
                        current = arr[0] + arr[1] + "00";
                    if (Count == 2)
                        current = arr[0] + arr[1] + "0";
                    if (Count == 3)
                        current = arr[0] + arr[1];
                    if (Count == 4)
                        current = current + " M Dollars";
                    return current + " M Dollars";
                }
                if (current2Lower.equals("million") || current2Lower.equals("m")) {
                    String[] arr = current.split("\\.");
                    current =handleDot(current);
                    current = current + " M Dollars";
                    return current;
                }
            }
        }
        // --- Cases 1.3,2.2: $PRICE ---
        if (!current2Lower.equals("dollars") && !current3.toLowerCase().equals("dollars") && !current4.toLowerCase().equals("dollars")) {
            current =handleDot(current);
            return ChangeToPriceNum(current) + " Dollars";
        }
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
                if(ans.contains("."))
                    ans= handleDot(ans);
                ans = ans + " M";
            }
            if(ans.contains("B")){
                ans =ans.substring(0,ans.length()-1);
                if(ans.contains(".")){
                    ans = handleDot(ans);
                    //String[] arr = ans.split("\\.");
                    //String temp = arr[1].substring(2);
                    ans = ans+ " M";
                }
                else{
                    ans = ans + "000";
                    if(ans.contains("."))
                        ans =handleDot(ans);
                    ans = ans+  " M";
                }
            }
        }
        else {
            if(current.contains("."))
                current = handleDot(current);
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
        if (current.contains("$") || p1.toLowerCase().equals("dollars") || current3.toLowerCase().equals("dollars") ||
                current4.toLowerCase().equals("dollars"))
            ans = true;
        return ans;
    }

    /**This function changes the number to the number as it should be in the parse rules.
     * @param current the number before the change
     * @return the number after the change.
     */
    private String changeNumToRegularNum(String current) {
        if(current.contains("B")||current.contains("K")||current.contains("M")||current.contains("K")){

        }
        String[] nums = current.split(",");
        String signal = "";
        if (nums[0].contains("$")) {
            signal = "" + nums[0].charAt(0);
            nums[0].substring(0, nums[0].length() - 1);
        }
        String ans = "";
        boolean to_change = true;
        int string_len = 0;
        // checks if the whole word is a number without any commas
        while (string_len < nums.length && to_change) {
            if (!isNumeric(nums[string_len]) && !nums[string_len].contains("$"))
                to_change = false;
            string_len++;
        }
        // if there are elements which are not numbers
        if (to_change) {
            if (nums.length == 1)
                return current;
            if (nums.length == 2) {//Thousand
                if (nums[1].contains("$")) {
                    signal = "$";
                    nums[0] = nums[1].substring(1);
                }
                if (nums[1].contains("%")) {
                    signal = "%";
                    nums[1] = nums[1].substring(0, nums[1].length() - 1);
                }
                ans = nums[0];
                String tempString = nums[1];
                int x = 0;
                while (x < tempString.length()) {
                    if (tempString.charAt(tempString.length() - 1) == '0')
                        tempString = tempString.substring(0, tempString.length() - 1);
                    x++;
                }
                if (tempString.equals("0")) {
                    tempString = "";
                    ans = ans + tempString + 'K';
                } else {
                    ans = ans + '.' + tempString + 'K';

                }
            }
            if (nums.length == 3) { //million
                ans = nums[0];
                if (nums[1].contains("$")) {
                    signal = "$";
                    nums[0] = nums[1].substring(1);
                }
                if (nums[1].contains("%")) {
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
                if (tempString.equals("0"))
                    tempString = "";
                String tempString2 = nums[1];

                int x1 = 0;
                while (x1 < tempString2.length()) {
                    if (tempString2.charAt(tempString2.length() - 1) == '0')
                        tempString2 = tempString2.substring(0, tempString2.length() - 1);
                    x1++;
                }
                if (tempString2.equals("0"))
                    tempString2 = "";
                if (tempString2.equals(""))
                    ans = nums[0] + 'M';
                else {
                    ans = nums[0] + '.' + tempString2 + 'M';
                }

                if (tempString.length() == 3 && tempString2.length() == 3) {
                    ans = nums[0] + '.' + nums[1] + nums[2] + 'M';
                }
                if (tempString.length() != 0)
                    ans = nums[0] + '.' + nums[1] + tempString + 'M';
                else if (tempString.length() == 0) {
                    if (tempString2.equals("") && !ans.contains("M"))
                        ans = nums[0] + tempString2 + tempString + 'M';
                }
            }
            if (nums.length == 4) { //billion
                if (nums[1].contains("$")) {
                    signal = "$";
                    nums[0] = nums[0].substring(1);
                }
                if (nums[1].contains("%")) {
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
                    if (tempString2.equals("0"))
                        tempString2 = "";
                    if (tempString2.equals("")) {
                        String tempString3 = nums[3];
                        int x12 = 0;
                        while (x12 < tempString3.length()) {
                            if (tempString3.charAt(tempString3.length() - 1) == '0')
                                tempString3 = tempString3.substring(0, tempString3.length() - 1);
                            x12++;
                        }
                        if (tempString3.equals("0"))
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
                            if (tempString3.equals("0"))
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
        if (ans.contains("M")) {
            String tempi = ans.substring(ans.length() - 1);
            ans = ans.substring(0, ans.length() - 1);
            ans = handleDot(ans);
            ans = ans + "M";
        }
        if (!ans.equals("") && !ans.contains("$"))
            ans = signal + ans;
        if (!ans.equals("") && ans.contains("$"))
            ans = ans;
        if(nums.length > 4){
            ans ="";
            for(int i = 0; i<nums.length;i++){
                for(int j =0; j<nums[i].length();j++){
                    if(nums[i].charAt(j)!=',')
                        ans = ans + nums[i].charAt(j);
                }
            }
        }
        else if (ans.equals(""))
            return current;
        String[] finalS = ans.split("\\.");
        if (finalS.length == 1)
            return ans;
        else {
            if (finalS[1].contains("B")&&finalS[1].length()>=2) {
                if (finalS[1].charAt(1) == 'B' &&finalS[1].charAt(0)=='0')
                    return finalS[0]  + finalS[1].charAt(1);
                if (finalS[1].charAt(1) == 'B')
                    return finalS[0] + "." + finalS[1].charAt(0) + finalS[1].charAt(1);
                return finalS[0] + "." + finalS[1].charAt(0) + finalS[1].charAt(1) + "B";
            }
            if (finalS[1].contains("M")&&finalS[1].length()>=2) {
                if (finalS[1].charAt(1) == 'M')
                    return finalS[0] + "." + finalS[1].charAt(0) + finalS[1].charAt(1);
                return finalS[0] + "." + finalS[1].charAt(0) + finalS[1].charAt(1) + "M";
            }
            if (finalS[1].contains("K") &&finalS[1].length()>=2) {
                if (finalS[1].charAt(1) == 'K')
                    return finalS[0] + "." + finalS[1].charAt(0) + finalS[1].charAt(1);
                return finalS[0] + "." + finalS[1].charAt(0) + finalS[1].charAt(1) + "K";
            }
            if(finalS[1].length()>=2)
            ans = finalS[0] + "." + finalS[1].charAt(0) + finalS[1].charAt(1);
            else
                ans = finalS[0] + "." + finalS[1].charAt(0);
        }
        return ans;
    }

    /** this function returns if the string given is a number or not
     * @param str is the string that we want to check
     * @return true if the string given is a number.
     */
    private static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    /** this function returns if the string given is a number or not
     * @param str is the string that we want to check
     * @return true if the string given is a number.
     */
    private static boolean isNumeric2(String str){
        boolean ans = false;
        // TODO Might change to take less time
        if(str.contains("0") || str.contains("1")||str.contains("2") || str.contains("3") || str.contains("4")
                || str.contains("5") || str.contains("6") || str.contains("7") || str.contains("8") || str.contains("9")){
            ans = true;
        }
        return ans;
    }

    /** this function checks if the string given is a valid string(number or word)
     * @param str - a given string
     * @return true if the string is valid. else - false
     */
    private boolean CheckIfValidString(String str) {
        boolean ans = false;
        if (isNumeric2(str) && !str.contains("-") && !str.contains(",") && !str.contains("\\.")) {
            for (int i = 0; i < str.length(); i++) {
                char charAt2 = str.charAt(i);
                if (Character.isLetter(charAt2)) {
                    return false;
                }
            }
            for (int i = 0; i < str.length(); i++) {
                if (str.charAt(i) == '$' && i != 0 || str.charAt(i) == '%' && i != str.length() - 1)
                    return false;
            }
            ans = true;
        }
        if (isNumeric2(str) && (str.contains("-") || str.contains("\\."))) {
            for (int i = 0; i < str.length(); i++) {
                char charAt2 = str.charAt(i);
                if (Character.isLetter(charAt2)) {
                    if (i > 0) {
                        if (str.charAt(i - 1) != '-')
                            return false;
                    }
                    if (i < str.length() - 2)
                        if (str.charAt(i + 1) != '-')
                            return false;
                }
            }
            return true;
        }
        return ans;
    }

    /**
     * stops parsing through documents
     */
    static void stop() {
        stop = true;
    }

    /**
     * resets all the static variables
     */
    public static void resetAll() {
        stop = false;
        stemming = false;
    }

    /**
     * resets parts of the static variables
     */
    static void resetPartially() {
        stop = false;
    }

    @Override
    public void run() {
        parseAll();
    }
}