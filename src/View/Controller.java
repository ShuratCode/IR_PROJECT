package View;

import Model.MyModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;


public class Controller implements Observer {
    private MyModel Model;
    private boolean isLoad;
    @FXML
    private TextField tIn,tOut;
    @FXML
    private Button bStartIndexing, bSaveDC,bLoadDC,bSelectRoot,bSelectDest,bShowCache,bShowDic,bReset;
    @FXML
    private CheckBox cStemer;
    @FXML
    private Label DBInfo;

    public Stage stage;

    private String sDiPath,sChPath;


    public Controller() {
        Model=null;
    }

    public void setModel(MyModel Model) {
        this.Model = Model;
    }
    /***********Functions*****************/
    public void initView(){
        bReset.setDisable(true);
        bSaveDC.setDisable(true);
        bShowCache.setDisable(true);
        bShowDic.setDisable(true);
    }
    public void startIndexing(){
        try {
            Model = new MyModel(tIn.getText(), cStemer.isSelected(), tOut.getText(), 10);
            Model.addObserver(this);
            bStartIndexing.setDisable(true);
            bReset.setDisable(true);
            bSaveDC.setDisable(true);
            bLoadDC.setDisable(true);
            bSelectRoot.setDisable(true);
            bSelectDest.setDisable(true);
            bShowCache.setDisable(true);
            bShowDic.setDisable(true);
            new Thread(()->{
                try {
                    Model.fnBuildDB();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }).start();

        }
        catch (Exception e){
            e.printStackTrace();
        }

    }
    public void reset(){
        if(Model!=null&&isLoad){
            Model.fnReset();
            bSaveDC.setDisable(true);
            bShowCache.setDisable(true);
            bShowDic.setDisable(true);
            isLoad=false;
        }

    }
    public void saveDC(){
        if(isLoad){
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Directory ");
            File selectedDirectory = chooser.showDialog(stage);
            if(selectedDirectory!=null){
                Model.fnSaveDicAndCache(selectedDirectory.getPath());
            }
        }
    }
    public void bLoadDC(){
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Directory ");
        File selectedDirectory = chooser.showDialog(stage);
        if(selectedDirectory!=null){
            Model=new MyModel("",false,"",10);
            bStartIndexing.setDisable(true);
            Model.fnLoadObjects(selectedDirectory.getPath());
        }
    }

    public void brwsTIN(){
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Root path");
        File selectedDirectory = chooser.showDialog(stage);
        tIn.setText(selectedDirectory.getPath());
    }
    public void brwsTOUT(){
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Destination path");
        File selectedDirectory = chooser.showDialog(stage);
        tOut.setText(selectedDirectory.getPath());
    }
    public void openDi()  {
        try{
            ProcessBuilder pb = new ProcessBuilder("Notepad.exe", sDiPath);//kinda works
            pb.start();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
    public void openCh(){
        if(isLoad){
            try{
                ProcessBuilder pb = new ProcessBuilder("Notepad.exe", sChPath);//kinda works
                pb.start();
            }
            catch (IOException e){
             e.printStackTrace();
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        String s=(String)arg;
        switch (s){
            case "index end" : {
                Platform.runLater(()->{AlertBox a = new AlertBox();
                            a.display("Indexing Finish", Model.buildBDInfo);
                            DBInfo.setText(Model.buildBDInfo);
                            bStartIndexing.setDisable(false);
                            bReset.setDisable(false);
                            bSaveDC.setDisable(false);
                            bLoadDC.setDisable(false);
                            bSelectRoot.setDisable(false);
                            bSelectDest.setDisable(false);
                            bShowCache.setDisable(false);
                            bShowDic.setDisable(false);
                            isLoad = true;});

                break;
            }
            case "input error":{

            }

        }
    }
}
