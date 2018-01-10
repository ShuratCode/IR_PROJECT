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
    private Button bStartIndexing, bSaveDC,bLoadDC,bSelectRoot,bSelectDest,bShowCache,bShowDic,bReset,bResetQH,bSaveQ,bRun1Q,bRunFileQ;
    @FXML
    private CheckBox cStemer,cbExtendQ, cbTop5S ;
    @FXML
    private Label DBInfo;

    private String sLastQSPath;

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
        bResetQH.setDisable(true);
        bRun1Q.setDisable(true);
        bSaveQ.setDisable(true);
        bRunFileQ.setDisable(true);
        cbTop5S.setDisable(true);
        cbExtendQ.setDisable(true);
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
            fnFreezWindow();
            DBInfo.setText("Status: processing\n please be patient\n");
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
        if(tQuery.getText().equals("")){
            AlertBox a = new AlertBox();
            a.display("Parameters Error", "Please enter a valid query");
            return;
        }
        if(cbTop5S.isSelected()){
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Corpus path");
            File selectedDirectory = chooser.showDialog(stage);
            if(selectedDirectory.getPath().equals("")){
                AlertBox a = new AlertBox();
                a.display("Parameters Error", "Please enter a valid corpus path");
                return;
            }
            Model.setCorpusP(selectedDirectory.getPath());
            fnFreezWindow();
            DBInfo.setText("Status: processing\n please be patient\n");
            new Thread(()->{
                Model.fnMostImportant(tQuery.getText());

            }).start();
        }
        else {
            if(tQuery.getText().equals("")){
                AlertBox a = new AlertBox();
                a.display("Parameters Error", "Please enter a valid query");
                return;
            }
            new Thread(()->Model.fnRunSimpleQuery(tQuery.getText(),cbExtendQ.isSelected())).start();
            fnFreezWindow();
            DBInfo.setText("Status: processing\n please be patient\n");
        }
    }


    public void saveQ(){
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Root path");
        File selectedDirectory = chooser.showDialog(stage);
        if(selectedDirectory.getPath().equals("")){
            AlertBox a = new AlertBox();
            a.display("Parameters Error", "Please enter a valid directory to save" );
            return;
        }
        Model.fnSaveQ(selectedDirectory.getPath()+"\\"+tQueryName.getText());
        sLastQSPath=selectedDirectory.getPath()+"\\"+tQueryName.getText();
        DBInfo.setText("Status: Save success\n ");
    }

    public void fnRunFileQ(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select query file");
        File selectedFile = fileChooser.showOpenDialog(stage);
        if(selectedFile.getPath().equals("")){
            AlertBox a = new AlertBox();
            a.display("Parameters Error", "Please enter a valid query file");
            return;
        }
        fnFreezWindow();
        DBInfo.setText("Status: processing\n please be patient\n");
        new Thread(()->{
            Model.fnRunFileQuery(selectedFile.getPath());
        }).start();
    }

    public void turnOExtend(){
        cbExtendQ.setSelected(false);
    }
    public void turnOffTop5(){
        cbTop5S.setSelected(false);
    }

    public void fnResetQH(){
        Model.fnResetQHistory(sLastQSPath);
    }

    private void fnFreezWindow(){
    bStartIndexing.setDisable(true);
    bSaveDC.setDisable(true);
    bLoadDC.setDisable(true);
    bSelectRoot.setDisable(true);
    bSelectDest.setDisable(true);
    bShowCache.setDisable(true);
    bShowDic.setDisable(true);
    bReset.setDisable(true);
    bResetQH.setDisable(true);
    bSaveQ.setDisable(true);
    bRun1Q.setDisable(true);
    bRunFileQ.setDisable(true);
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
                break;
            }
            case "SimpleQ returned" :{
                Platform.runLater(()->{
                    DisplayQ d=new DisplayQ();
                    d.display("Query results", Model.DisplayQ.toString());
                    bSaveQ.setDisable(false);
                    bRun1Q.setDisable(false);
                    bRunFileQ.setDisable(false);
                    bResetQH.setDisable(false);
                    bLoadDC.setDisable(false);
                    bSaveDC.setDisable(false);
                    bShowCache.setDisable(false);
                    bShowDic.setDisable(false);
                    DBInfo.setText("Status: Query returned successfully\n ");

                });
                break;
            }
            case "Load objects end":{
                Platform.runLater(()->{
                    bSaveDC.setDisable(false);
                    bShowCache.setDisable(false);
                    bShowDic.setDisable(false);
                    bLoadDC.setDisable(false);
                    bResetQH.setDisable(false);
                    bRun1Q.setDisable(false);
                    bRunFileQ.setDisable(false);
                    cbExtendQ.setDisable(false);
                    cbTop5S.setDisable(false);
                    bReset.setDisable(false);
                    bStartIndexing.setDisable(false);
                    bSelectRoot.setDisable(false);
                    bSelectDest.setDisable(false);
                    isLoad=true;
                    DBInfo.setText("Status: Load successfully \n ");
                });
                break;
            }
            case "FileQ end" : {
                Platform.runLater(()->{
                    DisplayQ d=new DisplayQ();
                    d.display("Query File results", Model.DisplayQ.toString());
                    bSaveQ.setDisable(false);
                    bRun1Q.setDisable(false);
                    bRunFileQ.setDisable(false);
                    bResetQH.setDisable(false);
                    bLoadDC.setDisable(false);
                    bSaveDC.setDisable(false);
                    bShowCache.setDisable(false);
                    bShowDic.setDisable(false);
                    DBInfo.setText("Status: Query returned successfully \n ");
                });
                break;
            }
            case "Top 5 end":{
                Platform.runLater(()->{
                    DisplayQ d=new DisplayQ();
                    d.display("Top 5 Lines", Model.DisplayQ.toString());
                    bSaveQ.setDisable(false);
                    bRun1Q.setDisable(false);
                    bRunFileQ.setDisable(false);
                    bResetQH.setDisable(false);
                    bLoadDC.setDisable(false);
                    bSaveDC.setDisable(false);
                    bShowCache.setDisable(false);
                    bShowDic.setDisable(false);
                    DBInfo.setText("Status: Query returned successfully \n ");
                });
                break;
            }
            case "Top 5 fail : doc or corpus not found":{
                Platform.runLater(()->{
                    AlertBox a = new AlertBox();
                    a.display("Input Error", "You have entered wrong parameters.\n Please check again document number & corpus path.\n");
                    bRun1Q.setDisable(false);
                    bRunFileQ.setDisable(false);
                    bResetQH.setDisable(false);
                    bLoadDC.setDisable(false);
                    bSaveDC.setDisable(false);
                    bShowCache.setDisable(false);
                    bShowDic.setDisable(false);
                });
                break;
            }

        }
    }

}
