/**
 * This class represents a document parsing throught its <text> </text> fields with the ReadFile class
 */

package GeneralClasses;

public class Document {

    private String docId;
    private String[] docText;
    private int docTextSize;

    public Document(String docId) {
        this.docId = docId;
        docTextSize = 0;
    }

    /**
     * gets the document's id
     * @return
     */
    public String getDocId() {
        return docId;
    }

    /**
     * gets the <text></text> String array
     * @return
     */
    public String[] getDocText() {
        return docText;
    }

    /**
     * adds another <text></text> String to the Document
     * @param text
     */
    public void addDocText(String text) {
        if (docTextSize == 0) {
            docText = new String[1];
            docText[0] = text;
            docTextSize++;
        }
        else {
            String[] tempArr = new String[docTextSize];
            for (int i = 0; i < docTextSize; i++) {
                tempArr[i] = docText[i];
            }
            docText = new String[docTextSize + 1];
            for (int i = 0; i < docTextSize; i++) {
                docText[i] = tempArr[i];
            }
            docText[docTextSize] = text;
            docTextSize++;
        }
    }
}
