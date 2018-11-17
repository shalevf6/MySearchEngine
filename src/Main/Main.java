package Main;

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
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();
//        ReadFile readFile = new ReadFile();
//        Parse parse = new Parse();
//        Thread readFileThread = new Thread(readFile.readThroughFiles());
//        Thread parseThread = new Thread(parse.parseAll();)
    }

    public static void main(String[] args) {
        launch(args);
    }
}
