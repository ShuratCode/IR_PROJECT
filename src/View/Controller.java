package View;

import Model.MyModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;


public class Controller implements Observer {
    private MyModel Model;
    private boolean isLoad;
    @FXML
    private TextField tIn,tOut, tQuery,tQueryName;
    @FXML
    private Button bStartIndexing, bSaveDC,bLoadDC,bSelectRoot,bSelectDest,bShowCache,bShowDic,bReset,bResetQH,bSaveQ;
    @FXML
    private CheckBox cStemer,cbExtendQ, cbTop5S ;
    @FXML
    private Label DBInfo;
    @FXML
    private TextArea tAreaDisplay;


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
            Model.addObserver(this);
            bStartIndexing.setDisable(true);
            bLoadDC.setDisable(true);
            new Thread(()->Model.fnLoadObjects(selectedDirectory.getPath())).start();

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
        if(isLoad){
        try{
            Model.fnShowDictionary();
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("Notepad.exe resources\\Dictionary.txt");

        }
        catch (IOException e){
            e.printStackTrace();
        }
    } }
    public void openCh(){
        if(isLoad){
            try{
                Model.fnShowCache();
                Runtime runtime = Runtime.getRuntime();
                Process process = runtime.exec("Notepad.exe resources\\Cache.txt");
            }
            catch (IOException e){
             e.printStackTrace();
            }
        }
    }

/****************************************************funcs for part B**************************************/

    public void fnRunSimpleQ(){
        if(cbTop5S.isSelected()){
            Model.fnMostImportant(tQuery.getText());
        }
        else new Thread(()->Model.fnRunSimpleQuery(tQuery.getText(),cbExtendQ.isSelected())).start();


    }

    public void saveQ(){
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Root path");
        File selectedDirectory = chooser.showDialog(stage);
        Model.fnSaveQ(selectedDirectory.getPath()+"\\"+tQueryName.getText());
    }

    public void fnRunFileQ(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select query file");
        File selectedFile = fileChooser.showOpenDialog(stage);
        Model.fnRunFileQuery(selectedFile.getPath());
    }
/*
    public void fnResetQH(){
        Model.fnResetQHistory();
    }*/
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
            case "SimpleQ returned" :{
                Platform.runLater(()->{
                    DisplayQ d=new DisplayQ();
                    d.display("Query results", Model.DisplayQ.toString());
                });
                break;
            }
            case "Load objects end":{
                Platform.runLater(()->{
                    bSaveDC.setDisable(false);
                    bShowCache.setDisable(false);
                    bShowDic.setDisable(false);
                    bLoadDC.setDisable(false);
                    isLoad=true;
                });
                break;
            }
            case "FileQ end" : {
                Platform.runLater(()->{
                    DisplayQ d=new DisplayQ();
                    d.display("Query File results", Model.DisplayQ.toString());
                });
                break;
            }

        }
    }

}
