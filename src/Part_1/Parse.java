package Part_1;

import GeneralClasses.Document;
import jdk.nashorn.internal.objects.Global;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * this class parses through the documents and creates a term list and a dictionary for every document
 */
public class Parse implements Runnable {

    private HashMap<String, Integer> StopWords;
    static public BlockingQueue<Document> docQueue = new ArrayBlockingQueue<>(1000);
    static private boolean stop = false;
    static public HashMap<String, Integer> corpusDictionary = new HashMap<>();
    static public HashMap<String, Integer> corpusCityDictionary = new HashMap<>();
    static public int docNumber = 0;
    public static boolean stemming;
    private HashMap<String, int[]> currentTermDictionary;
    private int max_tf;
    private String[] afterSplit;
    private int afterSplitLength;
    private Stemmer stemmer;
    boolean added;
    boolean dollar;

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
                if (stemming)
                    stemmer = new Stemmer();
                currentTermDictionary = new HashMap<>();
                max_tf = 0;
                added = false;
                dollar = false;
                boolean addedMore = false;
                Document document = docQueue.remove();
                String[] documents = document.getDocText();
                String documentTitle = document.getDocTitle();
                String documentDate = document.getDocDate();
                String documentCity = document.getCity();
                docNumber++;
                for (String data : documents) {
                    String current;
                    int counter = 0;
                    //splits the string
                    afterSplit = data.split("[?!:#@^&+{*}|<=>\"\\s;()_&\\\\\\[\\]]+");
                    afterSplitLength = afterSplit.length;
                    int wordCounter = 0;
                    while (wordCounter < afterSplitLength) {
                        afterSplit[wordCounter] = removeExtraDelimiters(afterSplit[wordCounter]);
                        wordCounter++;
                    }
                    // goes through every word in the document
                    while (counter <= afterSplitLength - 1) {
                        if (addedMore) {

                        }
                        current = afterSplit[counter];
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
                                if (counter + 1 < afterSplitLength) {
                                    if (handleMonthYearOrMonthDay(current, counter)) {
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
                                            if (!anotherWord.equals("") && !StopWords.containsKey(anotherWord))
                                                handleNormalLetters(current);
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

                                if (counter + 1 < afterSplit.length)
                                    current2 = afterSplit[counter + 1];
                                if (counter + 2 < afterSplit.length)
                                    current3 = afterSplit[counter + 2];
                                if (counter + 3 < afterSplit.length)
                                    current4 = afterSplit[counter + 3];
                                // checks if the number is the whole word
                                // ---case 0:inValid String -------
                                if (!CheckIfValidString(current)) {
                                    if (checkNumberEnding(current)) {
                                        String wordNum = current.substring(0, current.length() - 2);
                                        if (isNumeric(wordNum)) {
                                            handleNormalLetters(wordNum);
                                            continue;
                                        }
                                    }
                                    handleNormalLetters(current);
                                    counter++;
                                }
                                // ---case 0.5:contains dash ------//
                                boolean HoeMuchToChange = HandleDashwithNums(current) ;
                                if (current.contains("-") && HoeMuchToChange ||HandleDashwithNums(current2) && current2.contains("-")) {
                                    int ToAddToCounter = HandelDashNUms(current, current2, current3, current4, counter);
                                    if(ToAddToCounter!=0) {
                                        counter = counter + ToAddToCounter;
                                        continue;
                                    }
                                }
                                if ((!isNumeric(current) && !current.contains(",") || current.contains("$") || current2.equals("Dollars") || current2.equals("dollars") || current2.equals("percentage") ||
                                        current2.equals("percent") || current3.equals("Dollars") || current3.equals("dollars") || current4.equals("dollars") ||
                                        current4.equals("Dollars")) ) {

                                    // -------MONEY&&PERCENTAGE CHECK--------//

                                    int Add2Counter =HandleMoneyAndPercentage(current,current2,current3,current4,counter);
                                    if(Add2Counter != 0){
                                        counter = counter + Add2Counter;
                                        continue;
                                    }
                                } else  {
                                    // ------- NUMBER CHECK -------

                                    // ------- 'DD MONTH' and 'DD MONTH YEAR' CHECK
                                    if (isNumeric(current) || checkNumberEnding(current)) {
                                        int toAdd = handleDayMonthOrDayMonthYear(current, current2, current3);
                                        if (toAdd != 0) {
                                            counter = counter + toAdd;
                                            continue;
                                        }
                                    }
                                    int regularNumCheck = RegularNumCheck(current,current2,current3,current4,counter); //  CHECK CURRENT2 = MILLION \ BILLION \ TRILLION \ THOUSAND
                                    if(regularNumCheck !=0 ){
                                        counter = counter +regularNumCheck;
                                        continue;
                                    }
                                }
                                // ------- FRACTION CHECK -------
                                if ( isNumeric2(current)) {
                                    if (notFraction(current)) {
                                        handleNormalLetters(current);
                                        counter++;
                                    }
                                }

                                dollar = false;
                                handleNormalLetters(current);
                                counter++;
                            }
                        }
                        //Indexer.docQueue.add(document);
                    }
                }
                document.setTfAndTermDictionary(currentTermDictionary, max_tf);
            } else {
                if (stop) {
                    Indexer.stop();
                    break;
                }
            }
        }
    }

    private int RegularNumCheck(String current, String current2, String current3, String current4, int counter) {
        int toReturn = 0;
        if (current.contains(",") )
            current = changeNumToRegularNum(current);
        if (counter < afterSplit.length - 1)
            current2 = afterSplit[counter + 1];
        else
            current2 = "";
        if ( isNumeric2(current)) {
            if (notFraction(current2)) {
                current = current + " " + current2;
                handleNormalLetters(current);
                return 2;

            }
            //FIRST:IF CURRENT2= THOUSAND
            if (current2.equals("Thousand") || current2.equals("THOUSAND") || current2.equals("thousand")) {
                current = current + "K";
                toReturn++;
            }//SECOND:IF CURRENT2= MILLION
            if (current2.equals("Million") || current2.equals("MILLION") || current2.equals("million") || current2.equals("mill")) {
                current = current + "M";
                toReturn++;
            }//THIRD:IF CURRENT2= BILLION
            if (current2.equals("Billion") || current2.equals("BILLION") || current2.equals("billion")) {
                current = current + "B";
                toReturn++;
            }//FORTH:IF CURRENT2= TRILLION
            if (current2.equals("Trillion") || current2.equals("TRILLION") || current2.equals("trillion")) {
                Double temp = Double.parseDouble(current);
                temp = temp * 1000;
                int temp2 = temp.intValue();
                current = String.valueOf(temp2);
                current = current + "B";
                toReturn++;
            }

            if (current.contains("M")) {
                current = current.substring(0, current.length() - 1) + " M";
            }
            handleNormalLetters(current);
            toReturn++;
        }
        return toReturn;
    }

    private int HandleMoneyAndPercentage(String current, String current2, String current3, String current4, int counter) {
        int add2Counter = 0;
        // ------- PERCENTAGE CHECK -------
        // --- case 1: NUMBER% ---
        if (current.contains("%")) {
            handleNormalLetters(current);
            return 1;
        } else {
            if (counter + 1 < afterSplit.length) {
                // --- case 2, 3: NUMBER percent, NUMBER percentage ---
                if (afterSplit[counter + 1].equals("percent") || afterSplit[counter + 1].equals("percentage")) {
                    current = current + "%";
                    handleNormalLetters(current);
                     return 2;
                }
            }

                // ------- PRICE CHECK -------
                dollar = checkIfMoney(current, current2, current3, current4);
                // --- all cases: Price Dollars, Price Fraction Dollars, $price,....
                if (dollar) {
                    /* !!! need to update counter according to term !!! */
                    current = change_to_price(current, current2, current3, current4);
                    handleNormalLetters(current);
                    if (current2.equals("Dollars") || current2.equals("dollars"))
                        add2Counter = 2;
                    if (current3.equals("Dollars") || current3.equals("dollars"))
                        add2Counter = 3;
                    if (current4.equals("dollars") || current4.equals("Dollars"))
                        add2Counter = 4;
                    if (current.contains("$") && current2.equals("million") || current.contains("$") && current2.equals("billion") ||
                            current.contains("$") && current2.equals("trillion") || current.contains("$") && current2.equals("MILLION")
                            || current.contains("$") && current2.equals("BILLION") || current.contains("$") && current2.equals("TRILLION")
                            || current.contains("$") && current2.equals("Million") || current.contains("$") && current2.equals("Billion") ||
                            current.contains("$") && current2.equals("Trillion"))
                        add2Counter++;

                }

        }
        return add2Counter;
    }

    private int HandelDashNUms(String current, String current2, String current3, String current4, int counter) {
        int ToAdd2Counter = 0;
        //------current has Dash-------//
        if(current.contains("-")) {
            String[] DashSplit = current.split("-");
            //-----TWO NUMBERS------//
            if (isNumeric2(DashSplit[0]) && isNumeric2(DashSplit[1])) {
                String tempCurrent = DashSplit[0];
                current = DashSplit[1];
                if (current.contains(","))
                    current = changeNumToRegularNum(current);
                if (  isNumeric2(current)) {

                    //--------NUMBER-NUMBER FRACTION----//
                    if (notFraction(current2)) {
                        current = current + " " + current2;

                        handleNormalLetters(tempCurrent + "-" + current);
                        handleNormalLetters(current);
                        return 2;

                    }
                    if (counter < afterSplit.length - 1)
                        current2 = afterSplit[counter + 1];
                    else
                        current2 = "";
                    //--------NUMBER-NUMBER THOUSAND/MILLION/BILLION/TRILLION-------//
                    //FIRST:IF CURRENT2= THOUSAND
                        if (current2.equals("Thousand") || current2.equals("THOUSAND") || current2.equals("thousand")) {
                            current = current + "K";
                            ToAdd2Counter++;
                        }//SECOND:IF CURRENT2= MILLION
                        if (current2.equals("Million") || current2.equals("MILLION") || current2.equals("million") || current2.equals("mill")) {
                            current = current + "M";
                            ToAdd2Counter++;
                        }//THIRD:IF CURRENT2= BILLION
                        if (current2.equals("Billion") || current2.equals("BILLION") || current2.equals("billion")) {
                            current = current + "B";
                            ToAdd2Counter++;
                        }//FORTH:IF CURRENT2= TRILLION
                        if (current2.equals("Trillion") || current2.equals("TRILLION") || current2.equals("trillion")) {
                            Double temp = Double.parseDouble(current);
                            temp = temp * 1000;
                            int temp2 = temp.intValue();
                            current = String.valueOf(temp2);
                            current = current + "B";
                            counter--;
                            ToAdd2Counter = ToAdd2Counter + 1;
                        }
                        if (current.contains("M")) {
                            current = current.substring(0, current.length() - 1) + " M";
                        }

                }
                handleNormalLetters(tempCurrent);
                handleNormalLetters(current);
                handleNormalLetters(tempCurrent + "-" + current);
                return ToAdd2Counter + 1 ;
            }
            //------ONE NUMBER------//
            if ((isNumeric2(DashSplit[0]) || isNumeric2(DashSplit[1])) ) {
                String tempCurrent = DashSplit[0];
                current = DashSplit[1];
                if (isNumeric2(tempCurrent))
                    changeNumToRegularNum(tempCurrent);
                if (isNumeric2(current))
                    changeNumToRegularNum(current);

                //TODO add the word in shalev word's addition
                if (isOnlyLetters(current)) {
                    if (!StopWords.containsKey(current)) {
                        // if there is a delimiter in the word
                        if (!isOnlyLetters(current)) {
                            String remainingDelimiters = "['.,/\\s]+";
                            String[] moreWords = current.split(remainingDelimiters);
                            for (String moreWord : moreWords)
                                if (!moreWord.equals(""))
                                    handleAllLetters(moreWord);
                            // if there is no delimiter in the word
                        } else if (!current.equals(""))
                            handleAllLetters(current);

                    }
                }

                //-----NUMBER-WORD----------//
                if (isNumeric2(tempCurrent)) {
                    handleNormalLetters(tempCurrent);
                    handleNormalLetters(current);
                    handleNormalLetters(tempCurrent + "-" + current);
                    return 1;
                }
                //----WORD-NUMBER----------//
                //TODO add the word in shalev word's addition
                if (isNumeric2(current)) {
                    if ( isNumeric2(current)) {
                        //-------WORD-NUMBER FRACTION---------//
                        if (notFraction(current2)) {
                            current = current + " " + current2;
                            ToAdd2Counter++;
                            handleNormalLetters(current);
                            handleNormalLetters(tempCurrent);
                            handleNormalLetters(tempCurrent + "-" + current);
                            return 2;
                        }
                        if (counter < afterSplit.length - 1)
                            current2 = afterSplit[counter + 1];
                        else
                            current2 = "";
                        //------WORD-NUMBER THOUSAND/MILLION/TRILLION/BILLION
                        //TODO add the word in shalev word's addition
                         //FIRST:IF CURRENT2= THOUSAND
                            if (current2.equals("Thousand") || current2.equals("THOUSAND") || current2.equals("thousand")) {
                                current = current + "K";
                                ToAdd2Counter++;
                            }//SECOND:IF CURRENT2= MILLION
                            if (current2.equals("Million") || current2.equals("MILLION") || current2.equals("million") || current2.equals("mill")) {
                                current = current + "M";
                                ToAdd2Counter++;
                            }//THIRD:IF CURRENT2= BILLION
                            if (current2.equals("Billion") || current2.equals("BILLION") || current2.equals("billion")) {
                                current = current + "B";
                                ToAdd2Counter++;
                            }//FORTH:IF CURRENT2= TRILLION
                            if (current2.equals("Trillion") || current2.equals("TRILLION") || current2.equals("trillion")) {
                                Double temp = Double.parseDouble(current);
                                temp = temp * 1000;
                                int temp2 = temp.intValue();
                                int temp3;
                                current = String.valueOf(temp2);
                                current = current + "B";
                                ToAdd2Counter++;
                            }
                            if (current.contains("M")) {
                                current = current.substring(0, current.length() - 1) + " M";
                            }
                            handleNormalLetters(tempCurrent);
                            handleNormalLetters(current);
                            handleNormalLetters(tempCurrent + "-" + current);
                            return ToAdd2Counter +1;

                    }

                }
            }
        }

    //-------CURRENT2 HAS DASH-------//
    boolean HoeMuchToChange2 = HandleDashwithNums(current2);
        if (current2.contains("-") && HoeMuchToChange2) {
            String[] DashSplit = current2.split("-");
            String TempCurrent2 = DashSplit[0];
            String Temp2Current2 = DashSplit[1];
            if (isNumeric2(current)) {
                current = changeNumToRegularNum(current);
            }
            if (isNumeric2(Temp2Current2))
                Temp2Current2 = changeNumToRegularNum(Temp2Current2);
            if (isNumeric2(TempCurrent2))
                TempCurrent2 = changeNumToRegularNum(TempCurrent2);
                //TODO add the word in shalev word's addition
                //-------NUMBER MILLION/THOUSAND/TRILLION/BILLION-NUMBER-------//
                if (isNumeric2(current) && isNumeric2(Temp2Current2) && !notFraction(TempCurrent2)) {
                    if (TempCurrent2.equals("Thousand") || TempCurrent2.equals("THOUSAND") || TempCurrent2.equals("thousand")) {
                        current = current + "K";
                        ToAdd2Counter++;
                    }//SECOND:IF CURRENT2= MILLION
                    if (TempCurrent2.equals("Million") || TempCurrent2.equals("MILLION") || TempCurrent2.equals("million") || TempCurrent2.equals("mill")) {
                        current = current + "M";
                        ToAdd2Counter++;
                    }//THIRD:IF CURRENT2= BILLION
                    if (TempCurrent2.equals("Billion") || TempCurrent2.equals("BILLION") || TempCurrent2.equals("billion")) {
                        current = current + "B";
                        ToAdd2Counter++;
                    }//FORTH:IF CURRENT2= TRILLION
                    if (TempCurrent2.equals("Trillion") || TempCurrent2.equals("TRILLION") || TempCurrent2.equals("trillion")) {
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
                    if (current3.equals("Thousand") || current3.equals("THOUSAND") || current3.equals("thousand")) {
                        Temp2Current2 = Temp2Current2 + "K";
                        ToAdd2Counter++;
                    }//SECOND:IF CURRENT2= MILLION
                    if (current3.equals("Million") || current3.equals("MILLION") || current3.equals("million") || current3.equals("mill")) {
                        Temp2Current2 = Temp2Current2 + "M";
                        ToAdd2Counter++;
                    }//THIRD:IF CURRENT2= BILLION
                    if (current3.equals("Billion") || current3.equals("BILLION") || current3.equals("billion")) {
                        Temp2Current2 = Temp2Current2 + "B";
                        ToAdd2Counter++;
                    }//FORTH:IF CURRENT2= TRILLION
                    if (current3.equals("Trillion") || current3.equals("TRILLION") || current3.equals("trillion")) {
                        Double temp1 = Double.parseDouble(Temp2Current2);
                        temp1 = temp1 * 1000;
                        int temp2 = temp1.intValue();
                        Temp2Current2 = String.valueOf(temp2);
                        Temp2Current2 = Temp2Current2 + "B";
                        ToAdd2Counter = ToAdd2Counter + (2 - 1);
                    }
                    if (Temp2Current2.contains("M")) {
                        Temp2Current2 = Temp2Current2.substring(0, Temp2Current2.length() - 1) + " M";
                    }
                    handleNormalLetters(current);
                    handleNormalLetters(Temp2Current2);
                    handleNormalLetters(current + "-" + Temp2Current2);
                    return ToAdd2Counter + 1 ;

                }
                //--------NUMBER FRACTION-WORD-------//
                //TODO add the word in shalev word's addition
                if (isNumeric2(current) && notFraction(TempCurrent2) && isNumeric2(Temp2Current2)) {
                    if (current3.equals("Thousand") || current3.equals("THOUSAND") || current3.equals("thousand")) {
                        Temp2Current2 = Temp2Current2 + "K";
                        ToAdd2Counter++;
                    }//SECOND:IF CURRENT3= MILLION
                    if (current3.equals("Million") || current3.equals("MILLION") || current3.equals("million") || current3.equals("mill")) {
                        Temp2Current2 = Temp2Current2 + "M";
                        ToAdd2Counter++;
                    }//THIRD:IF CURRENT3= BILLION
                    if (current3.equals("Billion") || current3.equals("BILLION") || current3.equals("billion")) {
                        Temp2Current2 = Temp2Current2 + "B";
                        ToAdd2Counter++;
                    }//FORTH:IF CURRENT3= TRILLION
                    if (current3.equals("Trillion") || current3.equals("TRILLION") || current3.equals("trillion")) {
                        Double temp12 = Double.parseDouble(Temp2Current2);
                        temp12 = temp12 * 1000;
                        int temp22 = temp12.intValue();
                        counter++;
                        counter--;
                        Temp2Current2 = String.valueOf(temp22);
                        Temp2Current2 = Temp2Current2 + "B";
                        ToAdd2Counter = ToAdd2Counter + (2 - 1);
                    }
                    if (Temp2Current2.contains("M")) {
                        Temp2Current2 = Temp2Current2.substring(0, Temp2Current2.length() - 1) + " M";
                    }
                    handleNormalLetters(current);
                    handleNormalLetters(Temp2Current2);
                    handleNormalLetters(current + "-" + Temp2Current2);
                    return ToAdd2Counter + 1;
                }

        }
        return ToAdd2Counter;
    }

    private boolean HandleDashwithNums(String current) {
        String[] SplitDash = current.split("-");
        boolean toAddAll = false;
        if (SplitDash.length == 2) {
            return true;
        }
        return toAddAll;
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
        String monthNumber = getMonthNumber(current2);
        // check if the next word is a month
        if (!monthNumber.equals("00")) {
            int currentLength = current.length();
            // check if the number ends with an ordinal indicator (st, nd, rd, th) and remove it if so
            if (checkNumberEnding(current)) {
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
                    handleNormalLetters(current);
                    current = "0" + current;
                }
                handleNormalLetters(monthNumber + "-" + current);
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
                        handleNormalLetters("00" + current3 + "-" + monthNumber);
                    }
                    // need to add the YYYY-MM term separately
                    if (current3Length == 3) {
                        date = "0" + current3 + "-" + monthNumber + "-" + current;
                        handleNormalLetters("0" + current3 + "-" + monthNumber);
                    }
                    handleNormalLetters(current3);
                    handleNormalLetters(date);
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
                handleNormalLetters(date);
                handleNormalLetters(current2);
                handleAllLetters(getMonthNameForDictionary(monthNumber));
                return true;
            } else {
                if (!current2.equals("") && checkNumberEnding(current2)) {
                    current2 = current2.substring(0, current2Length - 2);
                    if (!current2.equals("")) {
                        if (current2Length - 2 == 1) {
                            date = monthNumber + "-0" + current2;
                            handleNormalLetters(date);
                            handleNormalLetters(current2);
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
                                handleNormalLetters(date);
                                handleNormalLetters(current2);
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
        String numberEnd = number.substring(numberLength - 2);
        return (numberEnd.equals("th") || numberEnd.equals("Th") || numberEnd.equals("TH") ||
                numberEnd.equals("st") || numberEnd.equals("St") || numberEnd.equals("ST") ||
                numberEnd.equals("nd") || numberEnd.equals("Nd") || numberEnd.equals("ND") ||
                numberEnd.equals("rd") || numberEnd.equals("Rd") || numberEnd.equals("RD"));
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
        if (monthName.equals("January") || monthName.equals("JANUARY") || monthName.equals("january") || monthName.equals("Jan") || monthName.equals("JAN")
                || monthName.equals("jan"))
            return "01";
        if (monthName.equals("February") || monthName.equals("FEBRUARY") || monthName.equals("february") || monthName.equals("Feb") || monthName.equals("FEB")
                || monthName.equals("feb"))
            return "02";
        if (monthName.equals("March") || monthName.equals("MARCH") || monthName.equals("march") || monthName.equals("Mar") || monthName.equals("MAR")
                || monthName.equals("mar"))
            return "03";
        if (monthName.equals("April") || monthName.equals("APRIL") || monthName.equals("april") || monthName.equals("Apr") || monthName.equals("APR")
                || monthName.equals("apr"))
            return "04";
        if (monthName.equals("May") || monthName.equals("MAY") || monthName.equals("may"))
            return "05";
        if (monthName.equals("June") || monthName.equals("JUNE") || monthName.equals("june") || monthName.equals("Jun") || monthName.equals("JUN")
                || monthName.equals("jun"))
            return "06";
        if (monthName.equals("July") || monthName.equals("JULY") || monthName.equals("july") || monthName.equals("Jul") || monthName.equals("JUL")
                || monthName.equals("jul"))
            return "07";
        if (monthName.equals("August") || monthName.equals("AUGUST") || monthName.equals("august") || monthName.equals("Aug") || monthName.equals("AUG")
                || monthName.equals("aug"))
            return "08";
        if (monthName.equals("September") || monthName.equals("SEPTEMBER") || monthName.equals("september") || monthName.equals("Sep") || monthName.equals("SEP")
                || monthName.equals("sep"))
            return "09";
        if (monthName.equals("October") || monthName.equals("OCTOBER") || monthName.equals("october") || monthName.equals("Oct") || monthName.equals("OCT")
                || monthName.equals("oct"))
            return "10";
        if (monthName.equals("November") || monthName.equals("NOVEMBER") || monthName.equals("november") || monthName.equals("Nov") || monthName.equals("NOV")
                || monthName.equals("nov"))
            return "11";
        if (monthName.equals("December") || monthName.equals("DECEMBER") || monthName.equals("december") || monthName.equals("Dec") || monthName.equals("DEC")
                || monthName.equals("dec"))
            return "12";
        return "00";
    }

    /**
     * this function checks if the string given is a fraction
     *
     * @param s
     * @return true if the string is a fraction
     */
    private boolean notFraction(String s) {
        boolean ans = false;
        if (s.contains("/")) {
            String[] ans1 = s.split("/");
            if (ans1.length > 2)
                return ans;
            if (isNumeric(ans1[0]) && isNumeric(ans1[1]))
                return true;
        }
        return ans;
    }

    /**
     * handles with words with dash between them
     * @param current - a given string with a '-' delimiter smoewhere in the middle of it
     */
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
     *
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
     *
     * @param current1 - the first word of the term
     * @param counter  - the counter for the words in the text
     * @return - true if the term was found to be true, else - false
     */
    private boolean handleBetweenNumberAndNumber(String current1, int counter) {
        // TODO add every number to the term dictionaries
        String current2;
        String current3;
        String current4;
        // checks if the 2nd word is a NUMBER
        if (counter + 1 < afterSplitLength && isNumeric2(afterSplit[counter + 1])) {
            current2 = afterSplit[counter + 1];
            // checks if the 3rd word is "and"
            if (counter + 2 < afterSplitLength && (afterSplit[counter + 2].equals("and") || afterSplit[counter + 2].equals("And") ||
                    afterSplit[counter + 2].equals("AND"))) {
                current3 = afterSplit[counter + 2];
                // checks if the 4th word is a NUMBER
                if (counter + 3 < afterSplitLength && isNumeric2(afterSplit[counter + 3])) {
                    current4 = afterSplit[counter + 3];
                    // TODO add a number fix for current2 and current4
                    handleNormalLetters(current1 + current2 + current3 + current4);
                    return true;
                } else {
                    /* TODO add a fraction check and a number with a "thousand" / "million" / "billion" / "trillion" after check for current2 and current 3
                       TODO if so, add one more current (and check the length of the String list accordingly)
                       TODO else, return false;
                    */
                    return false;
                }
            } else {
                /* TODO add a fraction check and a number with a "thousand" / "million" / "billion" / "trillion" after check for current2 and current 3
                   TODO if so, add one more current (and check the length of the String list accordingly)
                   TODO else, return false;
                */
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * handles all letter cases for a given word
     *
     * @param current - a given word
     */
    private void handleAllLetters(String current) {
        String stemmed = "";
        if (stemming)
            stemmed = stemmer.stemTerm(current);
        // --- case 1: all of the word is in lower letters ---
        if (current.toLowerCase().equals(current)) {
            if (stemming)
                handleLowerLetters(stemmed);
             else
                handleLowerLetters(current);
        } else {
            // --- cases 2,3: only first letter of word is a capital letter || all of the word is in capital letters ---
            if ((current.charAt(0) >= 65 && current.charAt(0) <= 90 &&
                    current.substring(1).toLowerCase().equals(current.substring(1))) || (current.toUpperCase().equals(current))) {
                char lowerLetter = (char) (current.charAt(0) + 32);
                String lowerCurrent = lowerLetter + current.substring(1);
                if (stemming)
                    handleCapitalLetters(stemmed); // TODO: maybe toLowerCase
                else
                    handleCapitalLetters(lowerCurrent);
            }
            // --- case 4: mixed capital, lower case word - > none of the above cases ---
            else {
                if (stemming)
                    handleNormalLetters(stemmed);
                else
                    handleNormalLetters(current);

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
    }

    /**
     * checks whether a lower case word has been already put into the dictionary, and if so, in lower letters or capital letters,
     * and than adds the relevant form of the word (upper or lower case) to the corpus and document dictionaries
     * @param current - the lower case form of the word
     */
    private void handleLowerLetters(String current) {
        String currentUpper = current.toUpperCase();
        int[] termData;
        // word is not in corpus dictionary and is not in document dictionary
        if (!corpusDictionary.containsKey(current) && !corpusDictionary.containsKey(currentUpper)) {
            termData = new int[4];
            termData[0] = 1;
            if (max_tf < 1)
                max_tf = 1;
            currentTermDictionary.put(current, termData);
            corpusDictionary.put(current, 1);
        } else {
            // word is in corpus dictionary in lower case
            if (corpusDictionary.containsKey(current)) {
                // word is in document dictionary in lower case
                if (currentTermDictionary.containsKey(current)) {
                    termData = currentTermDictionary.get(current);
                    int tf = termData[0];
                    termData[0] = tf + 1;
                    if (max_tf < tf + 1)
                        max_tf = tf + 1;
                }
                    // word is not in document dictionary
                else {
                    termData = new int[4];
                    termData[0] = 1;
                    if (max_tf < 1)
                        max_tf = 1;
                    currentTermDictionary.put(current, termData);
                    corpusDictionary.put(current, corpusDictionary.get(current) + 1);
                }
            } else {
                // word is in corpus dictionary in upper case
                if (corpusDictionary.containsKey(currentUpper)) {
                    int df = corpusDictionary.get(currentUpper);
                    corpusDictionary.remove(currentUpper);
                    // word is not in document dictionary
                    if (!currentTermDictionary.containsKey(currentUpper)) {
                        df++;
                        termData = new int[4];
                        termData[0] = 1;
                        if (max_tf < 1)
                            max_tf = 1;
                        currentTermDictionary.put(current, termData);
                    }
                    // word is in document dictionary in upper case
                    else {
                        termData = currentTermDictionary.get(currentUpper);
                        int tf = termData[0];
                        termData[0] = tf + 1;
                        if (max_tf < tf + 1)
                            max_tf = tf + 1;
                        currentTermDictionary.remove(currentUpper);
                        currentTermDictionary.put(current,termData);
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
        int[] termData;
        // word is not in corpus dictionary and is not in document dictionary
        if(!corpusDictionary.containsKey(current) && !corpusDictionary.containsKey(currentUpper)) {
            termData = new int[4];
            termData[0] = 1;
            if (max_tf < 1)
                max_tf = 1;
            currentTermDictionary.put(currentUpper, termData);
            corpusDictionary.put(currentUpper,1);
        }
        else {
            // word is in corpus dictionary in lower case
            if (corpusDictionary.containsKey(current)) {
                // word is in document dictionary in lower case
                if (currentTermDictionary.containsKey(current)) {
                    termData = currentTermDictionary.get(current);
                    int tf = termData[0];
                    termData[0] = tf + 1;
                    if (max_tf < tf + 1)
                        max_tf = tf + 1;
                }
                    // word is not in document dictionary
                else {
                    termData = new int[4];
                    termData[0] = 1;
                    if (max_tf < 1)
                        max_tf = 1;
                    currentTermDictionary.put(current, termData);
                    corpusDictionary.put(current,corpusDictionary.get(current) + 1);
                }
            } else {
                // word is in corpus dictionary in upper case
                if (corpusDictionary.containsKey(currentUpper)) {
                    // word is in document dictionary in upper case
                    if (currentTermDictionary.containsKey(currentUpper)) {
                        termData = currentTermDictionary.get(currentUpper);
                        int tf = termData[0];
                        termData[0] = tf + 1;
                        if (max_tf < tf + 1)
                            max_tf = tf + 1;
                    }
                    // word is not in document dictionary
                    else {
                        termData = new int[4];
                        termData[0] = 1;
                        if (max_tf < 1)
                            max_tf = 1;
                        currentTermDictionary.put(currentUpper, termData);
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
        int[] termData;
        if (currentTermDictionary.containsKey(current)) {
            termData = currentTermDictionary.get(current);
            int tf = termData[0];
            termData[0] = tf + 1;
            if (max_tf < tf + 1)
                max_tf = tf + 1;
            newTerm = false;
        }
        else {
            termData = new int[4];
            termData[0] = 1;
            if (max_tf < 1)
                max_tf = 1;
            currentTermDictionary.put(current, termData);
        }
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