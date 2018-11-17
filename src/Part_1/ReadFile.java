package Part_1;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;

public class ReadFile {

    public void readThroughFiles(String dirPath) throws FileNotFoundException {
        File dir = new File(dirPath + "\\corpus");
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
}