package View;

import Model.MyModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = fxmlLoader.load(getClass().getResource("View.fxml").openStream());
        primaryStage.setTitle("IR Engine");
        primaryStage.setScene(new Scene(root, 500, 400));
        /*******************/
        Controller C = fxmlLoader.getController();
        C.stage=primaryStage;
        C.initView();
        /*******************/
        primaryStage.show();



    }


    public static void main(String[] args) {

        launch(args);
    }
}
