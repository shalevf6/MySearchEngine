package Main;

import GeneralClasses.Document;
import Part_1.Parse;
import Part_1.ReadFile;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
//        Document doc = new Document("12");
//        doc.addDocText("120,000,000,001 $");
//        Parse parse = new Parse("");
//        Parse.docQueue.add(doc);
//        parse.parseAll();
    }

    private void readFileTest() {
        //        File f = new File("C:\\Users\\Shalev\\Desktop\\FB496130");
//        org.jsoup.nodes.Document file = Jsoup.parse(new String(Files.readAllBytes(f.toPath())));
//        Elements docs = file.getElementsByTag("DOC");
//        for (Element doc : docs) {
//            org.jsoup.nodes.Document document = Jsoup.parse(doc.text());
//            Elements docCity = doc.getElementsByTag("f p="+ "\"104\"");
//            String documentTitle = docCity.text();
//            System.out.println();
//            String allDocument = doc.toString();
//            String documentTitle = "";
//            Pattern p = Pattern.compile("<f p="+"\"104\">\n"+"(\\\\S+)</f>");
//            Matcher m = p.matcher(allDocument);
//            if (m.find())
//                documentTitle = m.group();
//            System.out.println();
//            int i = allDocument.indexOf("<f p="+ "\"104\">");
//            if (i != -1) {
//                i = i + 14;
//                documentTitle = documentTitle + allDocument.charAt(i);
//                i++;
//                while (allDocument.charAt(i) != '>') {
//                    StringBuilder stringBuilder = new StringBuilder(documentTitle);
//                    stringBuilder.append(allDocument.charAt(i));
//                    i++;
//                }
//            }
//        }
    }
    public static void main(String[] args) {
        launch(args);
    }
}
