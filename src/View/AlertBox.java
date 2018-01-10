package View;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * this class used to display Error messages to the user
 */
public class AlertBox
{
    public void display(String WinName, String text)
    {
        Stage window = new Stage();

        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(WinName);
        window.setMinWidth(250);
        window.setMinHeight(100);

        Label  label     = new Label(text);
        Button btn_close = new Button("OK");
        btn_close.setOnAction(event -> window.close());

        VBox contain = new VBox(15);
        contain.getChildren().addAll(label, btn_close);
        contain.setAlignment(Pos.CENTER);

        Scene scene = new Scene(contain);
        window.setScene(scene);
        window.showAndWait();

    }
}
