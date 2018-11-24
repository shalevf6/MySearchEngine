package Part_1;

import GeneralClasses.Document;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * this class parses through the documents and creates a term list and a dictionary for every document
 */
public class Parse implements Runnable {

    private ArrayList<String> StopWords ;
    static public BlockingQueue<Document> docQueue = new ArrayBlockingQueue<>(1000);
    static private boolean stop = false;

    public Parse(String path){
        StopWords = getStopWords(path);
    }

    /** This function generates the stop words Dictionary to Array List.
     * @param path is the location of the Dictionary
     * @return the Array List with all the stop words.
     */
    private ArrayList<String> getStopWords(String path) {
        StopWords = new ArrayList<String>();
        return StopWords;
    }

    /**
     * This is the main Parse Function
     */
    public void parseAll(){
        while (true) {
            if (!docQueue.isEmpty()) {
                HashMap<String,String> termDictionary = new HashMap<>();
                boolean added = false;
                boolean dollar = false;
                ArrayList<String> parsed = new ArrayList<>();
                Document document = docQueue.remove();
                String[] documents = document.getDocText();
                for (String data:documents) {
                    String Currant = "";
                    int counter = 0;
                    //splits the string
                    String[] AfterSplit = data.split(" ");
                    // goes through every word in the document
                    while (counter <= AfterSplit.length - 1) {
                        Currant = AfterSplit[counter];
                        // Checks if there aren't any numbers in the word
                        if (!isNumeric2(Currant)) {
                            //checks if the current string is a stop word
                            if (!StopWords.contains(Currant)) {

                                /* need to implement Capital Letters fix */

                                /*
                                if (!Currant.equals("$") || !Currant.equals("%") || !Currant.equals(">")
                                        || !Currant.equals("<")) {
                                    Currant = ChangeStringOrNot(Currant);
                                    parsed.add(Currant);
                                    added = true;
                                }
                                */
                            }
                        }
                        // means it's a number:
                        else {
                            // checks if the number is the whole word
                            if (!isNumeric(Currant)) {
//                                if(!added) {
                                // ------- PERCENTAGE CHECK -------
                                // --- case 1: NUMBER% ---
                                if (Currant.contains("%")) {
                                    parsed.add(Currant);
                                    added = true;
                                }
                                    else {
                                    if (counter + 1 < AfterSplit.length) {
                                        // --- case 2, 3: NUMBER percent, NUMBER percentage ---
                                        if (AfterSplit[counter + 1].equals("percent") || AfterSplit[counter + 1].equals("percentage")) {
                                            Currant = Currant + "%";
                                            parsed.add(Currant);
                                            added = true;
                                            counter++;
                                        }
                                    } else {
//                                }
                                        String Currant2 = "";
                                        String Currant3 = "";
                                        String Currant4 = "";
                                        if (counter + 1 < AfterSplit.length)
                                            Currant2 = AfterSplit[counter + 1];
                                        if (counter + 2 < AfterSplit.length)
                                            Currant3 = AfterSplit[counter + 2];
                                        if (counter + 3 < AfterSplit.length)
                                            Currant4 = AfterSplit[counter + 3];
                                        // ------- PRICE CHECK -------
                                        dollar = checkIfMoney(Currant, Currant2, Currant3, Currant4);
                                        // --- all cases: Price Dollars, Price Fraction Dollars, $price,....
                                        if (dollar) {
                                            /* !!! need to update counter according to term !!! */
                                            Currant = change_to_price(Currant, Currant2, Currant3, Currant4);
                                            parsed.add(Currant);
                                            added = true;
                                        }
                                        else {
                                            // ------- NUMBER CHECK -------
                                            // NEED TO CHECK CURRENT2 = MILLION \ BILLION \ TRILLION \ THOUSAND
                                            if (Currant.contains(",") && !added) {
                                                Currant = changeNumToRegularNum(Currant);
                                                parsed.add(Currant);
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

    private String change_to_price(String Current, String Currant2, String Currant3,String Currant4) {
        String ans = "";

        /* !!!! need to consider PRICE FRACTION for each case !!!! */

        if (Current.contains("$"))
            Current = Currant2.substring(1);

        // --- Cases 1.1,2.1: PRICE Dollars ---
        if (Currant2.equals("Dollars") || Currant2.equals("dollars")) {
            /*
            if(Current.contains("m")|| Current.contains("bn")){
                if(Current.contains("m")){
                    Current = Current.substring(0,Current.length()-1);
                    return Current + " M Dollars";
                }
                if(Current.contains("bn")){
                    Current = Currant2.substring(0,Current.length()-1);
                    return Current + "000 M Dollars";
                }
            */
            Current = ChangeToPriceNum(Current);
            return Current + " Dollars";
        }

        // --- Cases 2.3,2.4,2.5,2.8,2.9.2.10: PRICE_WITHOUT_DOT billion/million/trillion U.S. dollars ---
        if (Currant2.equals("billion") || Currant2.equals("million") || Currant2.equals("trillion") && !Current.contains(".")) {
            if (Currant2.equals("trillion")) {
                Current = Current + "000000 M Dollars";
                return Current;
            }
            if (Currant2.equals("billion")) {
                Current = Current + "000 M Dollars";
                return Current;
            }
            if (Currant2.equals("million")) {
                Current = Current + " M Dollars";
                return Current;
            }
        }
        else {
            // --- Cases 2.3,2.4,2.5,2.8,2.9.2.10: PRICE_WITH_DOT billion/million/trillion U.S. dollars ---
            if (Currant2.equals("billion") || Currant2.equals("million") || Currant2.equals("trillion") && Current.contains(".")) {
                int Count = 0;
                if (Currant2.equals("trillion")) {
                    String[] arr = Current.split(".");
                    Count = arr[1].length();
                    if (Count == 1)
                        Current = arr[0] + arr[1] + "00000";
                    if (Count == 2)
                        Currant2 = arr[0] + arr[1] + "0000";
                    if (Count == 3)
                        Current = arr[0] + arr[1] + "000";
                    if (Count == 4)
                        Current = arr[0] + arr[1] + "00";
                    if (Count == 5)
                        Current = arr[0] + arr[1] + "0";
                    if (Count == 6)
                        Current = arr[0] + arr[1];
                    Current = Current + " M Dollars";
                    return Current;
                }
                if (Currant2.equals("billion")) {
                    String[] arr = Current.split(".");
                    Count = arr[1].length();
                    if (Count == 1)
                        Current = arr[0] + arr[1] + "00";
                    if (Count == 2)
                        Currant2 = arr[0] + arr[1] + "0";
                    if (Count == 3)
                        Current = arr[0] + arr[1];
                    if (Count == 4)
                        Current = Current + " M Dollars";
                    return Current;
                }
                if (Currant2.equals("million")) {
                    String[] arr = Current.split(".");
                    Current = Current + " M Dollars";
                    return Current;
                }
            }
        }
        // --- Cases 1.3,2.2: $PRICE ---
        if (!Currant2.equals("Dollars") && !Currant2.equals("dollars") && !Currant3.equals("Dollars") && !Currant3.equals("dollars") &&
                !Currant4.equals("Dollars") && !Currant4.equals("dollars")) {
            return ChangeToPriceNum(Current) + " Dollars";
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

    /** this function checks if the Currant string should be a price number.
     * @param currant
     * @param p1
     * @param currant3
     * @return true if the Currant string should be consider as a price.
     */
    private boolean checkIfMoney(String currant, String p1, String currant3,String currant4) {
        boolean ans = false;
        if (currant.contains("$") || p1.equals("Dollars") || currant3.equals("Dollars") || currant4.equals("Dollars") ||
                p1.equals("dollars") || currant3.equals("dollars")
                || currant4.equals("dollars"))
            ans = true;
        return ans;
    }

    /**This function change the number to the number as it should be in the parse rules.
     * @param currant the number before the change
     * @return the number after the change.
     */
    private String changeNumToRegularNum(String currant) {
        String[] nums = currant.split(",");
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
                return currant;
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
     * @param currant is the string that we need to be changed.
     * @return the string changed according to the rules given.
     */
    private String changeMillForPrice(String currant) {
        String Toreturn = "" ;
        if(currant.contains("$")) {
            if(currant.endsWith("$"))
                currant = currant.substring(0,currant.length()-1);
            else{
                currant = currant.substring(1);
            }

        }
        return Toreturn;
    }

    /**This function returns True if the string given contains number that smaller then one million
     * @param currant is the string given
     * @return true if the number is smaller then one million
     */
    private boolean isLessThenMill(String currant) {
        boolean ans = false;
        return ans;
    }

    /** this function checks if the string need to change in cases of lower case/upper case and change him if needed.
     * @param currant the string that we want to check his variations
     * @return the string after check the variation of thw string and change if needed.
     */
    private String ChangeStringOrNot(String currant) {
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
        /* Might change to take less time */
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
}

