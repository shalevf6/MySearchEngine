package Part_1;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.nio.file.Files;

public class ReadFile {

    public void readThroughFiles(String dirPath) throws FileNotFoundException {
        File dir = new File(dirPath);
        File[] subDirs = dir.listFiles();
        if (subDirs != null) {
            for (File f : subDirs) {
                File[] tempFiles = f.listFiles();
                if (tempFiles != null) {
                    try {
                        Document document = Jsoup.parse(new String(Files.readAllBytes(f.toPath())));
                        Elements elements = document.getElementsByTag("DOC");
                        for (Element element : elements) {
                            String text = element.getElementsByTag("TEXT").text();
                            Parse.docQueue.add(text);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        Parse.stop();
    }
}
