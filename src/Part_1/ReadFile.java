package Part_1;

import javafx.scene.control.Alert;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.nio.file.Files;

public class ReadFile implements Runnable {

    private String dirPath;
    public ReadFile(String dirPath) {
        this.dirPath = dirPath;
    }

    public void readThroughFiles() {
        File dir = new File(dirPath + "\\corpus");
        if (dir.exists()) {
            File[] subDirs = dir.listFiles();
            if (subDirs != null) {
                for (File f : subDirs) {
                    File[] tempFiles = f.listFiles();
                    if (tempFiles != null) {
                        try {
                            Document document = Jsoup.parse(new String(Files.readAllBytes(f.toPath())));
                            Elements elements = document.getElementsByTag("DOC");
                            GeneralClasses.Document doc = new GeneralClasses.Document(elements.text());
                            for (Element element : elements) {
                                String text = element.getElementsByTag("TEXT").text();
                                doc.addDocText(text);
                            }
                            // Parse.docQueue.add(doc);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            Parse.stop();
        }
        else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("You must choose an existing corpus folder path!");
            alert.show();
        }
    }

    @Override
    public void run() {
        readThroughFiles();
    }
}