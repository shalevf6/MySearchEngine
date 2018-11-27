package Part_1;

import GeneralClasses.Document;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
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
    public static boolean stemming;
    private HashMap<String,Integer> currentTermDictionary;
    private ArrayList<String> parsed;
    private String[] AfterSplit;
    private int AfterSplitLength;
    private Stemmer stemmer;

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
    public void parseAll() {
        while (true) {
            if (!docQueue.isEmpty()) {
                if(stemming)
                    stemmer = new Stemmer();
                currentTermDictionary = new HashMap<>();
                boolean added = false;
                boolean dollar = false;
                boolean addedMore = false;
                parsed = new ArrayList<>();
                Document document = docQueue.remove();
                String[] documents = document.getDocText();
                docNumber++;
                for (String data : documents) {
                    String current;
                    int counter = 0;
                    //splits the string
                    AfterSplit = data.split("[?!:#@^&+{*}|<=>\"\\s;()_&\\\\\\[\\]]+");
                    AfterSplitLength = AfterSplit.length;
                    int wordCounter = 0;
                    while (wordCounter < AfterSplitLength) {
                        AfterSplit[wordCounter] = removeExtraDelimiters(AfterSplit[wordCounter]);
                        wordCounter++;
                    }
                    // goes through every word in the document
                    while (counter <= AfterSplitLength - 1) {
                        if(addedMore) {

                        }
                        current = AfterSplit[counter];
                        // checks if the current string is a stop word (and not the word between)
                        if (!(current.equals("between") || current.equals("Between") || current.equals("BETWEEN")) && StopWords.containsKey(current)) {
                            counter++;
                        } else {
                            // checks if there aren't any numbers in the word
                            if (!isNumeric2(current)) {

                                // ------- 'BETWEEN NUMBER AND NUMBER' CHECK -------
                                // checks if the 1st word is "between"
                                if (current.equals("between") || current.equals("Between") || current.equals("BETWEEN")) {
                                    if (handleBetweenNumberAndNumber(current, counter)) {
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
                                    counter++;
                                    continue;
                                }

                                // ------- 'WORD/WORD' CHECK -------
                                if (current.contains("/")) {
                                    String[] orSplit = current.split("/");
                                    checkFurtherSplits(orSplit);
                                    counter++;
                                    continue;
                                }

                                // ------- 'MONTH YEAR' and 'MONTH DD' CHECK -------
                                if (counter + 1 < AfterSplitLength) {
                                    if (handleMonthYearOrMonthDay(current,counter)) {
                                        counter = counter + 2;
                                        continue;
                                    }
                                }

                                // ------- CAPITAL LETTERS CHECK -------
                                if (isOnlyLetters(current)) {
                                    handleAllLetters(current);
                                    counter++;
                                    // means its a different/empty letter case
                                } else {
                                    if (!current.equals("")) {
                                        String[] moreWords = current.split("[-'%$]+");
                                        for (String anotherWord : moreWords) {
                                            if(!anotherWord.equals("") && !StopWords.containsKey(anotherWord))
                                                updateDictionaries(current);
                                        }
                                    }
                                    counter++;
                                }
                            }
                            // means it's a number:
                            else {
                                String current2 = "";
                                String current3 = "";
                                String current4 = "";

                                if (counter + 1 < AfterSplit.length)
                                    current2 = AfterSplit[counter + 1];
                                if (counter + 2 < AfterSplit.length)
                                    current3 = AfterSplit[counter + 2];
                                if (counter + 3 < AfterSplit.length)
                                    current4 = AfterSplit[counter + 3];
                                // checks if the number is the whole word
                                if (!isNumeric(current) && !current.contains(",") ||current.contains("$") || current2.equals("Dollars") || current2.equals("dollars") || current2.equals("percentage") ||
                                        current2.equals("percent") || current3.equals("Dollars") || current3.equals("dollars") || current4.equals("dollars") ||
                                        current4.equals("Dollars")) {
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
                                        }
                                        if (!added) {

                                            // ------- PRICE CHECK -------
                                            dollar = checkIfMoney(current, current2, current3, current4);
                                            // --- all cases: Price Dollars, Price Fraction Dollars, $price,....
                                            if (dollar) {
                                                /* !!! need to update counter according to term !!! */
                                                current = change_to_price(current, current2, current3, current4);
                                                parsed.add(current);
                                                if (current2.equals("Dollars") || current2.equals("dollars"))
                                                    counter++;
                                                if (current3.equals("Dollars") || current3.equals("dollars"))
                                                    counter = counter + 2;
                                                if (current4.equals("dollars") || current4.equals("Dollars"))
                                                    counter = counter + 3;
                                                if (current.contains("$") && current2.equals("million") || current.contains("$") && current2.equals("billion") ||
                                                        current.contains("$") && current2.equals("trillion") || current.contains("$") && current2.equals("MILLION")
                                                        || current.contains("$") && current2.equals("BILLION") || current.contains("$") && current2.equals("TRILLION")
                                                        || current.contains("$") && current2.equals("Million") || current.contains("$") && current2.equals("Billion") ||
                                                        current.contains("$") && current2.equals("Trillion"))
                                                    counter = counter + 1;

                                                added = true;
                                            }

                                        }
                                    }
                                } else {
                                    // ------- NUMBER CHECK -------

                                    // ------- 'DD MONTH' and 'DD MONTH YEAR' CHECK
                                    if(isNumeric(current) || checkNumberEnding(current)) {
                                        int toAdd = handleDayMonthOrDayMonthYear(current, current2, current3);
                                        if (toAdd != 0) {
                                            counter = counter + toAdd;
                                            continue;
                                        }
                                    }

                                    //  CHECK CURRENT2 = MILLION \ BILLION \ TRILLION \ THOUSAND
                                    if (current.contains(",") && !added)
                                        current = changeNumToRegularNum(current);
                                    if (!added && isNumeric2(current)) {
                                        if (notFraction(current2)) {
                                            current = current + " " + current2;
                                            counter++;
                                            parsed.add(current);
                                            added = true;
                                            if (counter < AfterSplit.length - 1)
                                                current2 = AfterSplit[counter + 1];
                                            else
                                                current2 = "";
                                        }
                                        if (!added) { //FIRST:IF CURRENT2= THOUSAND
                                            if (current2.equals("Thousand") || current2.equals("THOUSAND") || current2.equals("thousand")) {
                                                //Double temp = Double.parseDouble(current);
                                                //temp = temp*1000;
                                                //current = String.valueOf(temp);
                                                current = current + "K";
                                                counter++;
                                            }//SECOND:IF CURRENT2= MILLION
                                            if (current2.equals("Million") || current2.equals("MILLION") || current2.equals("million") || current2.equals("mill")) {
                                                //Double temp = Double.parseDouble(current);
                                                //temp = temp*1000000;
                                                //current = String.valueOf(temp);
                                                current = current + "M";
                                                counter++;
                                            }//THIRD:IF CURRENT2= BILLION
                                            if (current2.equals("Billion") || current2.equals("BILLION") || current2.equals("billion")) {
                                                //Double temp = Double.parseDouble(current);
                                                //temp = temp*1000000000;
                                                //current = String.format ("%f", temp);
                                                //current = changeNumToRegularNum(current);
                                                current = current + "B";
                                                counter++;
                                            }//FORTH:IF CURRENT2= TRILLION
                                            if (current2.equals("Trillion") || current2.equals("TRILLION") || current2.equals("trillion")) {
                                                Double temp = Double.parseDouble(current);
                                                temp = temp * 1000;
                                                int temp2 = temp.intValue();
                                                ;
                                                current = String.valueOf(temp2);
                                                current = current + "B";

                                                counter++;
                                            }

                                            if (current.contains("M")) {
                                                current = current.substring(0, current.length() - 1) + " M";
                                            }

                                            parsed.add(current);
                                            added = true;

                                        }
                                    }

                                }
                                // ------- FRACTION CHECK -------
                                if (!added && isNumeric2(current)) {
                                    if (notFraction(current)) {
                                        parsed.add(current);
                                        added = true;
                                    }
                                }
                                added = false;
                                dollar = false;
                                counter++;
                            }

                        }
                        //document.setTermList(parsed);
                        //Indexer.docQueue.add(document);
                    }
                }

            }
            else{
                if (stop) {
                    Indexer.stop();
                    break;
                }
            }
        }
    }

    /**
     * handles the 'DD MONTH' / 'DD MONTH YYYY' date cases
     * @param current - an optional DD word
     * @param current2 - an optional MONTH word
     * @param current3 - an optional YEAR word
     * @return - 0 if the case was found not to be true, 2 if the 'DD MONTH' case was true, and 3 if the 'DD MONTH YYYY' case was true
     */
    private int handleDayMonthOrDayMonthYear(String current, String current2, String current3) {
        String monthNumber = getMonthNumber(current2);
        // check if the next word is a month
        if(!monthNumber.equals("00")) {
            int currentLength = current.length();
            // check if the number ends with an ordinal indicator (st, nd, rd, th) and remove it if so
            if (checkNumberEnding(current)) {
                current = current.substring(0,currentLength - 2);
                currentLength = currentLength - 2;
            }
            // check if the number has 2 or less digits
            if (currentLength <= 2){
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
     * @param current - a given word that might be a month
     * @param counter - the counter for the words in the split array
     * @return - true if it was found that the case was verified, else - false
     */
    private boolean handleMonthYearOrMonthDay(String current, int counter) {
        String monthNumber = getMonthNumber(current);
        // checks if the first word is a month
        if(!monthNumber.equals("00")) {
            String current2 = AfterSplit[counter + 1];
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
            }
            else {
                if (!current2.equals("") && checkNumberEnding(current2)){
                    current2 = current2.substring(0,current2Length - 2);
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
     * @param number - a given number
     * @return - true if the case we check is true. else - false
     */
    private boolean checkNumberEnding(String number) {
        int numberLength = number.length();
        String numberEnd = number.substring(numberLength - 2);
        return (numberEnd.equals("th") || numberEnd.equals("Th") || numberEnd.equals("TH") ||
                numberEnd.equals("st") || numberEnd.equals("St") || numberEnd.equals("ST") ||
                numberEnd.equals("nd") || numberEnd.equals("Nd") || numberEnd.equals("ND") ||
                numberEnd.equals("rd") || numberEnd.equals("Rd") || numberEnd.equals("RD"));
    }

    /**
     * gets a month's name in capital letters given its number
     * @param monthNumber - a given month's number
     * @return - a month's name in capital letters
     */
    private String getMonthNameForDictionary(String monthNumber) {
        if(monthNumber.equals("01"))
            return "JANUARY";
        if(monthNumber.equals("02"))
            return "FEBRUARY";
        if(monthNumber.equals("03"))
            return "MARCH";
        if(monthNumber.equals("04"))
            return "APRIL";
        if(monthNumber.equals("05"))
            return "MAY";
        if(monthNumber.equals("06"))
            return "JUNE";
        if(monthNumber.equals("07"))
            return "JULY";
        if(monthNumber.equals("08"))
            return "AUGUST";
        if(monthNumber.equals("09"))
            return "SEPTEMBER";
        if(monthNumber.equals("10"))
            return "OCTOBER";
        if(monthNumber.equals("11"))
            return "NOVEMBER";
        if(monthNumber.equals("12"))
            return "DECEMBER";
        return "";
    }

    /**
     * checks and returns the number that represents the month name in the given string
     * @param monthName - the given month name
     * @return - the number that represents the month name in the given string. If it's not a month name, than returns "00"
     */
    private String getMonthNumber(String monthName) {
        if(monthName.equals("January") || monthName.equals("JANUARY") || monthName.equals("january") || monthName.equals("Jan") || monthName.equals("JAN")
                || monthName.equals("jan"))
            return "01";
        if(monthName.equals("February") || monthName.equals("FEBRUARY") || monthName.equals("february") || monthName.equals("Feb") || monthName.equals("FEB")
                || monthName.equals("feb"))
            return "02";
        if(monthName.equals("March") || monthName.equals("MARCH") || monthName.equals("march") || monthName.equals("Mar") || monthName.equals("MAR")
                || monthName.equals("mar"))
            return "03";
        if(monthName.equals("April") || monthName.equals("APRIL") || monthName.equals("april") || monthName.equals("Apr") || monthName.equals("APR")
                || monthName.equals("apr"))
            return "04";
        if(monthName.equals("May") || monthName.equals("MAY") || monthName.equals("may"))
            return "05";
        if(monthName.equals("June") || monthName.equals("JUNE") || monthName.equals("june") || monthName.equals("Jun") || monthName.equals("JUN")
                || monthName.equals("jun"))
            return "06";
        if(monthName.equals("July") || monthName.equals("JULY") || monthName.equals("july") || monthName.equals("Jul") || monthName.equals("JUL")
                || monthName.equals("jul"))
            return "07";
        if(monthName.equals("August") || monthName.equals("AUGUST") || monthName.equals("august") || monthName.equals("Aug") || monthName.equals("AUG")
                || monthName.equals("aug"))
            return "08";
        if(monthName.equals("September") || monthName.equals("SEPTEMBER") || monthName.equals("september") || monthName.equals("Sep") || monthName.equals("SEP")
                || monthName.equals("sep"))
            return "09";
        if(monthName.equals("October") || monthName.equals("OCTOBER") || monthName.equals("october") || monthName.equals("Oct") || monthName.equals("OCT")
                || monthName.equals("oct"))
            return "10";
        if(monthName.equals("November") || monthName.equals("NOVEMBER") || monthName.equals("november") || monthName.equals("Nov") || monthName.equals("NOV")
                || monthName.equals("nov"))
            return "11";
        if(monthName.equals("December") || monthName.equals("DECEMBER") || monthName.equals("december") || monthName.equals("Dec") || monthName.equals("DEC")
                || monthName.equals("dec"))
            return "12";
        return "00";
    }

    /**this function checks if the string given is a fraction
     * @param s
     * @return true if the string is a fraction
     */
    private boolean notFraction(String s) {
        boolean ans = false;
        if(s.contains("/")) {
            String[] ans1 = s.split("/");
            if(ans1.length>2)
                return ans;
            if(isNumeric(ans1[0])&& isNumeric(ans1[1]))
                return true;
        }
        return ans;
    }
                      
    private void handleWordsWithDash(String current) {
        String[] dashSplit = current.split("-");
        boolean allWords = true;
        int dashSplitLength = dashSplit.length;
        String remainingDelimiters = "['.,/\\s]+";
        // if there are 2 or 3 words between the '-' delimiter
        if (dashSplitLength == 2 || dashSplitLength == 3) {
            for (String aDashSplit : dashSplit)
                if (!StopWords.containsKey(aDashSplit)) {
                    // if there is a delimiter in the word
                    if (!isOnlyLetters(aDashSplit)) {
                        allWords = false;
                        String[] moreWords = aDashSplit.split(remainingDelimiters);
                        for (String moreWord : moreWords)
                            if (!moreWord.equals(""))
                                handleAllLetters(moreWord);
                        // if there is no delimiter in the word
                    } else if (!aDashSplit.equals(""))
                        handleAllLetters(aDashSplit);
                }
            // if all the words between the '-' delimiter are letters
            if (allWords) {
                handleNormalLetters(current);
            }
        }
        // if there are more than 3 words between the '-' delimiter
        else {
            checkFurtherSplits(dashSplit);

        }
    }

    /**
     * checks if there are more delimiters which we didn't check before in the given string array
     * @param delimiterSplit - a given string array
     */
    private void checkFurtherSplits(String[] delimiterSplit) {
        String remainingDelimiters = "[.,'/\\s]+";
        for (String delimiterWord : delimiterSplit) {
            if (!StopWords.containsKey(delimiterWord)) {
                // if there is no delimiter in the word
                if (isOnlyLetters(delimiterWord)) {
                    if (!delimiterWord.equals(""))
                        handleAllLetters(delimiterWord);
                }
                // if there is a delimiter in the word
                else {
                    String[] moreWords = delimiterWord.split(remainingDelimiters);
                    for (String moreWord : moreWords)
                        if (!moreWord.equals(""))
                            handleAllLetters(moreWord);
                }
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
        if (counter + 1 < AfterSplitLength && isNumeric2(AfterSplit[counter + 1])) {
            current2 = AfterSplit[counter + 1];
            // checks if the 3rd word is "and"
            if (counter + 2 < AfterSplitLength && (AfterSplit[counter + 2].equals("and") || AfterSplit[counter + 2].equals("And") ||
                    AfterSplit[counter + 2].equals("AND"))) {
                current3 = AfterSplit[counter + 2];
                // checks if the 4th word is a NUMBER
                if (counter + 3 < AfterSplitLength && isNumeric2(AfterSplit[counter + 3])) {
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
            if (!word.equals("") && length > 1 && (word.charAt(0) == ',' || word.charAt(0) == '/' || word.charAt(0) == '.' || word.charAt(0) == '-'))
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

    /**this function checks if the string given is a month
     * @param s
     * @return true if the string given is a Month.
     */
    private boolean notMonth(String s) {
        boolean ans = false;

        return ans;
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

        /* !!!! need to consider PRICE FRACTION for each case !!!! */

        if (current.contains("$"))
            current = current.substring(1);

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
        //---Case not mentioned: PRICE FRACTION Dollars -----
        if(isNumeric(current)&& notFraction(current2)&& (current3.equals("Dollars")||current3.equals("dollars")||current3.equals("DOLLARS"))){
            return current+" "+ current2 +" "+ "Dollars";
        }

        // --- Cases 2.3,2.4,2.5,2.8,2.9.2.10: PRICE_WITHOUT_DOT billion/million/trillion U.S. dollars ---
        if (current2.equals("billion") &&!current.contains(".") || current2.equals("million")&&!current.contains(".") || current2.equals("trillion") && !current.contains(".")
                ||current2.equals("Trillion") && !current.contains(".")||current2.equals("TRILLION") && !current.contains(".")
                ||current2.equals("Billion") && !current.contains(".")||current2.equals("BILLION") && !current.contains(".")
                ||current2.equals("MILLION") && !current.contains(".")||current2.equals("Million") && !current.contains(".") ||
                current2.equals("bn") && !current.contains(".")||current2.equals("m") && !current.contains(".") ) {
            if (current2.equals("trillion") ||current2.equals("Trillion")||current2.equals("TRILLION")  ) {
                current = current + "000000 M Dollars";
                return current;
            }
            if (current2.equals("billion")||current2.equals("BILLION")||current2.equals("Billion") ||current2.equals("bn") ) {
                current = current + "000 M Dollars";
                return current;
            }
            if (current2.equals("million") ||current2.equals("MILLION")||current2.equals("Million")||current2.equals("m")  ) {
                current = current + " M Dollars";
                return current;
            }
        }
        else {
            // --- Cases 2.3,2.4,2.5,2.8,2.9.2.10: PRICE_WITH_DOT billion/million/trillion U.S. dollars ---
            if (current2.equals("billion")&& current.contains(".") || current.contains(".")&& current2.equals("million") || current2.equals("trillion") && current.contains(".")
                    ||current2.equals("Trillion") && current.contains(".")||current2.equals("TRILLION") && current.contains(".")
                    ||current2.equals("Billion") && current.contains(".")||current2.equals("BILLION") && current.contains(".")
                    ||current2.equals("MILLION") && current.contains(".")||current2.equals("Million") && current.contains(".") ||
                    current2.equals("bn") && current.contains(".")||current2.equals("m") && current.contains(".") ) {
                int Count = 0;
                if (current2.equals("trillion")||current2.equals("Trillion") || current2.equals("TRILLION")) {
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
                    current = current + " M Dollars";
                    return current;
                }
                if (current2.equals("billion")|| current2.equals("BILLION")||current2.equals("bn")|| current2.equals("Biliion")) {
                    String[] arr = current.split("\\.");
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
                if (current2.equals("million")|| current2.equals("m")|| current2.equals("Million")|| current2.equals("MILLION")||current2.equals("M")) {
                    String[] arr = current.split("\\.");
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
                    String[] arr = ans.split("\\.");
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
        if(nums[0].contains("$")) {
            signal =""+nums[0].charAt(0);
            nums[0].substring(0,nums[0].length()-1);
        }
        String ans = "";
        boolean to_change = true;
        int string_len = 0;
        // checks if the whole word is a number without any commas
        while (string_len < nums.length && to_change) {
            if (!isNumeric(nums[string_len])&& !nums[string_len].contains("$"))
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
                    if(tempString2.equals("") && !ans.contains("M"))
                        ans = nums[0]  + tempString2 + tempString +'M';


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

        if(!ans.equals("") && !ans.contains("$"))
            ans = signal + ans;
        if(!ans.equals("") && ans.contains("$"))
            ans= ans;
        else if(ans.equals(""))
            return current;
        String[] finalS = ans.split("\\.");
        if(finalS.length == 1)
            return ans;
        else
        {
            if(finalS[1].contains("B")) {
                if(finalS[1].charAt(1)=='B')
                    return finalS[0] + "." + finalS[1].charAt(0) + finalS[1].charAt(1);
                return finalS[0] + "." + finalS[1].charAt(0) + finalS[1].charAt(1) + "B";
            }
            if(finalS[1].contains("M")) {
                if(finalS[1].charAt(1)=='M')
                    return finalS[0] + "." + finalS[1].charAt(0) + finalS[1].charAt(1);
                return finalS[0] + "." + finalS[1].charAt(0) + finalS[1].charAt(1) + "M";
            }
            if(finalS[1].contains("K")) {
                if(finalS[1].charAt(1)=='K')
                    return finalS[0] + "." + finalS[1].charAt(0) + finalS[1].charAt(1);
                return finalS[0] + "." + finalS[1].charAt(0) + finalS[1].charAt(1) + "K";
            }
            ans=finalS[0]+"."+finalS[1].charAt(0)+finalS[1].charAt(1);
        }

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
        // TODO: Might change to take less time
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

    /** this function checks if the string given is a valid string(number or word)
     * @param str
     * @return true if the string is valid
     */
    public boolean CheckIfValidString(String str){
        boolean ans = true;
        if(isNumeric2(str) &&!str.contains("-") &&!str.contains(",") &&!str.contains(".")){
            for (int i = 0; i < str.length(); i++) {
                char charAt2 = str.charAt(i);
                if (Character.isLetter(charAt2)) {
                   ans = false;
                   return ans;
                }

            }
        }

        return ans;
    }
    public static void main(String[] args) {
        //String sTry = "of ]an [unidentified poll made in May 1993. The approval/disapproval \n" +
          //      "   ratings, in\\percent, \"for_ten ;Macedonian politicians were:";
        //Scanner sc = new Scanner(System.in);
        //String s = sc.nextLine();
        //String toDelete = "[?!:+{*}|<=>\"\\s;()_&\\\\\\[\\]]+";
        //String[] AfterSplit = s.split(toDelete);
        //System.out.println(Arrays.toString(AfterSplit));
        System.out.println("Start Parsing");
        Parse p = new Parse("320 million U.S. dollars 1 trillion U.S. dollars");

        p.parseAll();
//         String sTry = "of, an,unidentified poll. made.in May .1993 ,The /approval disapproval/ for/the things";
//        Scanner sc = new Scanner(System.in);
//        String s = sc.nextLine();
//         String remainingDelimiters = "[.,/\\s]+";
//         String toDelete = "[?!:+{*}|<=>\"\\s;()_&\\\\\\[\\]]+";
//         String[] AfterSplit = sTry.split(remainingDelimiters);
//         System.out.println(Arrays.toString(AfterSplit));
    }
}

