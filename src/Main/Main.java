package Main;

import GeneralClasses.Document;
import Part_1.Parse;
import Part_1.ReadFile;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
        //Document doc = new Document("12");
        //doc.addDocText("120,000,000,001 $");
        //Parse parse = new Parse("");
        //Parse.docQueue.add(doc);
        //parse.parseAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
