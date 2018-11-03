package GeneralClasses;

public class Document {

    private int docId;
    private String docTitle;
    private String docText;

    public Document(String docText) {
        this.docText = docText;
    }

    public Document(int docId, String docTitle, String docText) {
        this.docId = docId;
        this.docTitle = docTitle;
        this.docText = docText;
    }

    public int getDocId() {
        return docId;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    public String getDocTitle() {
        return docTitle;
    }

    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }

    public String getDocText() {
        return docText;
    }

    public void setDocText(String docText) {
        this.docText = docText;
    }
}
