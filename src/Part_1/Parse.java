package Part_1;

import java.util.ArrayList;
import java.util.Queue;

import static org.jsoup.helper.StringUtil.isNumeric;

public class Parse {

    private String data;
    private ArrayList<String> parsed ;
    private ArrayList<String> StopWords ;
    static public Queue<String> docQueue;
    static protected boolean stop;

    public Parse(String data,String path){
        this.data = data ;
        parsed = new ArrayList<String>();
        StopWords = getStopWords(path);

    }

    /** This function generates the stop words Dictionary to Array List.
     * @param path is the location of the Dictionary
     * @return the Array List with all the stop words.
     */
    private ArrayList<String> getStopWords(String path) {
        ArrayList<String> StopWords = new ArrayList<String>();
        return StopWords;
    }

    /** This is the main Parse Function
     * @return the Array List after parsing.
     */
    public void parseAll(){
        while (true) {
            if (!docQueue.isEmpty()) {
                String Currant = "";
                int counter = 0;
                String[] AfterSplit = data.split(" ");    //splits the string
                while (counter < AfterSplit.length - 1) {
                    Currant = AfterSplit[counter];
                    if(Currant.contains(","))
                        Currant=changeNumToRegularNum(Currant);
                    if (!isNumeric2(Currant)) {
                        if (!StopWords.contains(Currant)) { //checks if the currant string is a stop word
                            if (!Currant.equals("$") || !Currant.equals("%") || !Currant.equals(">")
                                    || !Currant.equals("<")) {
                                Currant = ChangeStringOrNot(Currant);
                                parsed.add(Currant);
                            }
                        }
                    } else {
                        if (!isNumeric(Currant)) {
                            /// case one:percentage
                            if (Currant.contains("%"))
                                parsed.add(Currant);
                            if (counter + 1 < AfterSplit.length) {
                                if (AfterSplit[counter + 1].equals("percent") || AfterSplit[counter + 1].equals("percentage")) {
                                    Currant = Currant + "%";
                                    parsed.add(Currant);
                                    counter++;
                                }
                            }
                            //case two:price
                            //1. "450,000$"
                            if (Currant.contains("$")) {
                                if (isLessThenMill(Currant)) {
                                    Currant = Currant + " Dollars";
                                    parsed.add(Currant);
                                } else {
                                    //2. 450,000,000$
                                    Currant = changeMillForPrice(Currant);
                                    Currant = Currant + " Dollars";
                                    parsed.add(Currant);
                                }
                            }
                            //3. 345,000 Dollars
                            if (isLessThenMill(Currant))
                                Currant = changeMillForPrice(Currant);
                            if (counter + 1 < AfterSplit.length) {
                                if (AfterSplit[counter + 1].equals("Dollars")) {
                                    Currant = Currant + " Dollars";
                                    counter++;
                                    parsed.add(Currant);
                                }
                            }
                            //4. 22 2/3 Dollars
                            if (counter + 2 < AfterSplit.length) {
                                if (AfterSplit[counter + 1].contains("/") && AfterSplit[counter + 2].equals("Dollars")) {
                                    Currant = Currant + " " + AfterSplit[counter + 1] + " Dollars";
                                    counter = counter + 2;
                                }
                            }
                            //case four:only number

                        }
                    }
                    counter++;
                }
            }
            if (stop) {
                break;
            }
        }
    }

    /**This function change the number to the number as it should be in the parse rules.
     * @param currant the number before the change
     * @return the number after the change.
     */
    private String changeNumToRegularNum(String currant) {
        String[] nums = currant.split(",");
        String ans = "";
        boolean to_change = true;
        int string_len = 0;
        while (string_len < nums.length && to_change) {
            if (!isNumeric(nums[string_len]))
                to_change = false;
            string_len++;
        }
        if (to_change) {
            if (nums.length == 1)
                return currant;
            if (nums.length == 2) {//Thousand
                ans = nums[0];
                String tempString = nums[1];
                int x = 0;
                while (x < tempString.length()) {
                    if (tempString.charAt(tempString.length() - 1) == '0')
                        tempString = tempString.substring(0, tempString.length() - 2);
                    x++;
                }
                ans = ans + tempString + 'K';
            }
            if (nums.length == 3) { //million
                ans = nums[0];
                String tempString = nums[2];
                int x = 0;
                while (x < tempString.length()) {
                    if (tempString.charAt(tempString.length() - 1) == '0')
                        tempString = tempString.substring(0, tempString.length() - 2);
                    x++;
                }
                String tempString2 = nums[1];
                if (tempString.equals("")) {
                    int x1 = 0;
                    while (x1 < tempString2.length()) {
                        if (tempString2.charAt(tempString2.length() - 1) == '0')
                            tempString2 = tempString2.substring(0, tempString2.length() - 2);
                        x1++;
                    }
                    if (tempString2.equals(""))
                        ans = nums[0] + 'M';
                    else {
                        ans = nums[0] + '.' + tempString2 + 'M';
                    }
                } else {
                    ans = nums[0] + '.' + nums[1] + nums[2] + 'M';
                }

            }
            if (nums.length == 4) { //billion
                String tempString = nums[1];
                int x = 0;
                while (x < tempString.length()) {
                    if (tempString.charAt(tempString.length() - 1) == '0')
                        tempString = tempString.substring(0, tempString.length() - 2);
                    x++;
                }
                String tempString2 = nums[2];
                if (tempString.equals("")) {
                    int x1 = 0;
                    while (x1 < tempString2.length()) {
                        if (tempString2.charAt(tempString2.length() - 1) == '0')
                            tempString2 = tempString2.substring(0, tempString2.length() - 2);
                        x1++;
                    }
                    if (tempString2.equals("")) {
                        String tempString3 = nums[3];
                        int x12 = 0;
                        while (x12 < tempString3.length()) {
                            if (tempString3.charAt(tempString3.length() - 1) == '0')
                                tempString3 = tempString3.substring(0, tempString3.length() - 2);
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
                                tempString3 = tempString3.substring(0, tempString3.length() - 2);
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
                            tempString2 = tempString2.substring(0, tempString2.length() - 2);
                        x1++;
                        if (tempString2.equals("")) {
                            String tempString3 = nums[3];
                            int x12 = 0;
                            while (x12 < tempString3.length()) {
                                if (tempString3.charAt(tempString3.length() - 1) == '0')
                                    tempString3 = tempString3.substring(0, tempString3.length() - 2);
                                x12++;
                            }
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
                                    tempString3 = tempString3.substring(0, tempString3.length() - 2);
                                x12++;
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

        }
        return ans;
    }

    /** This function used when the string is a price string and the price is higher then one million, and change the string price
     * according to the directions.
     * @param currant is the string that we need to be changed.
     * @return the string changed according to the rules given.
     */
    private String changeMillForPrice(String currant) {
        String Toreturn = "" ;
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

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }


    /** this function returns if the string given is a number or not
     * @param str is the string that we want to check
     * @return true if the string given is a number.
     */
    public static boolean isNumeric(String str)
    {
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
        if(str.contains("0") || str.contains("1")||str.contains("2") || str.contains("3") || str.contains("4")
                || str.contains("5") || str.contains("6") || str.contains("7") || str.contains("8") || str.contains("9")){
            ans = true;
        }
        return ans;
    }

    public static void stop() {
        stop = true;
    }
}

