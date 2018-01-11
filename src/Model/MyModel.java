package Model;


import Documnet.Document;
import Indexer.MyIndexer;
import Parse.Parse;
import Parse.Term;
import ReadFile.ReadFile;
import Searcher.Searcher;
import Stemmer.PorterStemmer;
import Tuple.MutablePair;
import Tuple.MutableTriple;
import org.jsoup.Jsoup;

import java.io.*;
import java.util.*;


/**
 * @author Shaked
 * @since 23-Nov-17
 */
public class MyModel extends Observable
{
    /***********************************************************************************************
     *                                      Fields                                                 *
     ***********************************************************************************************/
    public String                                         buildBDInfo;
    public HashMap<String, MutablePair<double[], String>> mDocInfo;
    public StringBuilder                                  DisplayQ, QtoSave;
    private ReadFile  readFile;
    private Parse     parse;
    private MyIndexer indexer;
    private String    sRootPath, sStopWordsPath, sPathForPosting;
    private ArrayList<String> arrayStringFilesPath;
    private PorterStemmer     stemmer;
    private boolean           bToStem;
    private int               iNumOfDocs, iNumOfParts;
    private Searcher searcher;


    /***********************************************************************************************
     *                                      Constructors                                           *
     ***********************************************************************************************/
    /**
     * Constructor. will create a new folder name Posting in the path to keep all the files
     *
     * @param sRootPath       path for the db
     * @param bToStem         do we use stemmer or not
     * @param sPathForPosting path to where we save the posting files
     * @param iNumOfParts     how many parts we divide the db for reading.
     */
    public MyModel(String sRootPath, boolean bToStem, String sPathForPosting, int iNumOfParts)
    {
        this.sRootPath = sRootPath;
        this.readFile = new ReadFile();
        this.parse = new Parse();
        this.bToStem = bToStem;
        this.sPathForPosting = sPathForPosting + "\\Posting";
        String sPathForIndexer = fnCreateFolder();
        this.indexer = new MyIndexer(sPathForIndexer, bToStem);
        this.arrayStringFilesPath = new ArrayList<>();
        this.stemmer = new PorterStemmer();
        this.mDocInfo = new HashMap<>();
        this.iNumOfDocs = 0;
        this.iNumOfParts = iNumOfParts;
        this.searcher = null;

    }

    /**
     * Build the data base and index from the path we got in the builder.
     * first we will collect all the files paths, and then will send each path to the readFile.
     * after the reading we will send each document to parsing.
     */
    public void fnBuildDB()
    {
        long startTime = System.currentTimeMillis();
        fnCreateFolder();
        fnFindFilesInDB();
        this.parse.setHashSetStopWords(readFile.fnReadStopWords(this.sStopWordsPath));
        int                 iNumOfFiles        = this.arrayStringFilesPath.size(); // Num of files in the db
        int                 iPercent           = iNumOfFiles / this.iNumOfParts;
        ArrayList<Document> listDocumentInFile = new ArrayList<>();
        // Looping over all the files in the db
        for (int iIndex = 0, iItr = 0, iTimeToRead = 0; iIndex < iNumOfFiles; iIndex++, iItr++)
        {
            File fileToRead = new File(this.arrayStringFilesPath.get(iIndex));
            listDocumentInFile.addAll(readFile.fnReadFile(fileToRead)); // All the documents in a file

            if (iItr < iPercent && iTimeToRead < 8)
            {
                continue;
            }

            if ((iTimeToRead == (this.iNumOfParts - 1)) && (iIndex < (iNumOfFiles - 1)))
            {
                continue;
            }

            ArrayList<Term> listOfTerms = new ArrayList<>(); // All the terms in the file

            // Looping over all the documents in each file
            int listDocumentInFileLength = listDocumentInFile.size();
            iNumOfDocs += listDocumentInFileLength;

            for (int iIndex2 = 0; iIndex2 < listDocumentInFileLength; iIndex2++)
            {
                StringBuilder sbTextToParse = listDocumentInFile.get(iIndex2).getText();
                StringBuilder sbDocName     = new StringBuilder(listDocumentInFile.get(iIndex2).getName());
                String        sDocFile      = listDocumentInFile.get(iIndex2).getsFileName();
                if (sbDocName.charAt(0) == ' ')
                {
                    sbDocName.deleteCharAt(0);
                }
                if (sbDocName.charAt(sbDocName.length() - 1) == ' ')
                {
                    sbDocName.deleteCharAt(sbDocName.length() - 1);
                }


                org.jsoup.nodes.Document doc = Jsoup.parse(String.valueOf(sbTextToParse));
                sbTextToParse = new StringBuilder(doc.text());
                MutablePair<ArrayList<Term>, int[]> m           = parse.fnParseText1(sbTextToParse, String.valueOf(sbDocName));
                int                                 iCurrLength = mDocInfo.size();
                listOfTerms.addAll(m.getLeft());
                double[] d = {m.getRight()[0], m.getRight()[1], 0};
                mDocInfo.put(String.valueOf(sbDocName), new MutablePair<>(d, sDocFile));

            }
            if (this.bToStem)
            {
                for (int iIndex3 = 0, iSize = listOfTerms.size(); iIndex3 < iSize; iIndex3++)
                {
                    Term termTemp = listOfTerms.get(iIndex3);
                    if (termTemp.getType() == 1)
                    {
                        String sStemmed = this.stemmer.stemTerm(termTemp.getsName());
                        termTemp.setsName(sStemmed);

                    }
                }
            }

            indexer.fnIndexWords(listOfTerms);

            listDocumentInFile = new ArrayList<>();
            iItr = 0;
            iTimeToRead++;
        }

        indexer.setTotalDocs(iNumOfDocs);
        try
        {
            indexer.fnMergePostings(this.mDocInfo);
            long   endTime   = System.currentTimeMillis();
            double totalTime = (endTime - startTime) / Math.pow(10, 3);
            buildBDInfo = "" + fnCreateEndMessage(totalTime);
            //fnWriteCahce();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        setChanged();
        notifyObservers("index end");
    }

    /**
     * Creates the end message to represent in the gui
     *
     * @param dTotalTime the total running time in seconds
     */
    private StringBuilder fnCreateEndMessage(double dTotalTime)
    {
        StringBuilder sbEndMessage = new StringBuilder("");
        sbEndMessage.append("The number of document indexed is: ").append(iNumOfDocs).append("\n");
        sbEndMessage.append("The size of index in bytes is: ").append(this.indexer.fnGetIndexSize()).append("\n");
        sbEndMessage.append("The size of cache in bytes is: ").append(this.indexer.getCacheSize()).append("\n");
        sbEndMessage.append("The total time is: ").append(dTotalTime).append("\n");
        return sbEndMessage;
    }

    /**
     * Create new folder for saving the index files.
     *
     * @return path for the folder who created
     */
    private String fnCreateFolder()
    {
        File   file = new File(sPathForPosting);
        String sResult;
        if (!file.exists())
        {
            file.mkdir();
        }
        if (this.bToStem)
        {
            sResult = sPathForPosting + "\\Stemmed";
            File file2 = new File(sResult);
            if (!file2.exists())
            {
                file2.mkdir();
            }
            else
            {
                fnClearFolder(file2);
            }
        }
        else
        {
            sResult = sPathForPosting + "\\Non Stemmed";
            File file2 = new File(sResult);
            if (!file2.exists())
            {
                file2.mkdir();
            }
            else
            {
                fnClearFolder(file2);
            }
        }
        return sResult;
    }

    /**
     * this function will get all the files in the path we got in the builder
     */
    private void fnFindFilesInDB()
    {
        File   fileDirToRead = new File(this.sRootPath);
        File[] files         = fileDirToRead.listFiles(); // Use this method to make sure we will not get an npe in listFiles() method
        if (null != files)
        {
            for (int iIndex = 0, filesLength = files.length; iIndex < filesLength; iIndex++)
            {
                File fileEntry = files[iIndex];
                if (null != fileEntry && fileEntry.isDirectory())
                {
                    File[] files2 = fileEntry.listFiles();
                    if (null != files2)
                    {
                        int files2Length = files2.length;

                        for (int i = 0; i < files2Length; i++)
                        {
                            this.arrayStringFilesPath.add(files2[i].getAbsolutePath());
                        }
                    }
                }
                else
                {
                    if (null != fileEntry)
                    {
                        this.sStopWordsPath = fileEntry.getAbsolutePath();
                    }

                }
            }
        }
        else
        {
            System.out.println("The root path you gave is not a folder");
        }
    }

    /**
     * Reset the whole db
     */
    public void fnReset()
    {
        String sPathForIndexer = fnCreateFolder();
        this.indexer.fnResetIndex();
        File file  = new File(this.sPathForPosting);
        File file2 = new File("Resources");
        if (file2.exists())
        {
            File[] files = file2.listFiles();
            if (null != files)
            {
                for (int i = 0, filesLength = files.length; i < filesLength; i++)
                {
                    File file3 = files[i];
                    if (file3.getName().equals("Cache Words") || file.getName().equals("Stemmed Cache Words"))
                    {
                        continue;
                    }
                    file3.delete();
                }
            }

        }
        this.indexer = null;
        System.gc();
        this.indexer = new MyIndexer(sPathForIndexer, bToStem);
    }

    /**
     * Clears the folder from all it's files
     *
     * @param file objects represent the folder
     */
    private void fnClearFolder(File file)
    {
        File[] files = file.listFiles();
        if (files != null)
        {
            if (files.length != 0)
            {
                for (int iIndex = 0, iSize = files.length; iIndex < iSize; iIndex++)
                {
                    File file2 = files[iIndex];
                    if (!file2.isDirectory())
                    {
                        file2.delete();
                    }
                    else
                    {
                        fnClearFolder(file2);
                    }
                }
            }
        }
    }

    /**
     * Set the boolean true if we need to use stemmer and false if we don't need to use stemmer
     *
     * @param bToStem to stem or not to stem
     */
    public void setbToStem(boolean bToStem)
    {
        this.bToStem = bToStem;
        this.indexer.setbToStem(bToStem);
    }

    /**
     * Save the Dictionary and Cache in a specific path
     *
     * @param sPathForObjects where to save the files
     */
    public void fnSaveDicAndCache(String sPathForObjects)
    {
        this.indexer.fnWriteDicAndCache(sPathForObjects);
    }

    /**
     * Sort the cache from the indexer. Return first the term with most idf score
     *
     * @return priority queue
     */
    private PriorityQueue<MutablePair<String, Float>> fnCreateCache()
    {
        HashMap<String, MutableTriple<Integer[], Float, Long>> hashMap = this.indexer.getDictionary();
        PriorityQueue<MutablePair<String, Float>> pairs = new PriorityQueue<>((o1, o2) ->
        {
            float f = o1.getRight() - o2.getRight();
            return (f < 0 ? -1 : (f > 0) ? 1 : 0);
        });

        for (String sTerm : hashMap.keySet())
        {
            pairs.add(new MutablePair<>(sTerm, hashMap.get(sTerm).getMiddle()));
        }

        return pairs;
    }

    /**
     * Write the cache to file
     */
    private void fnWriteCahce()
    {
        File file = new File("Resources\\Cache");
        if (!file.exists())
        {
            try
            {
                file.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        try
        {
            BufferedWriter                            bf    = new BufferedWriter(new FileWriter(file));
            PriorityQueue<MutablePair<String, Float>> pairs = fnCreateCache();
            for (MutablePair<String, Float> pair : pairs)
            {
                bf.write(pair.getLeft());
                bf.newLine();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Load the dictionary and cache from a path.
     * The files needed to be in specific form and in specific name.
     *
     * @param sPathForObjects from where to load the files
     */
    public void fnLoadObjects(String sPathForObjects)
    {
        this.indexer.setPathForObjects(sPathForObjects);

        File file = new File(sPathForObjects + "\\cacheStemmed");
        if (file.exists())
        {
            this.bToStem = true;
            this.indexer.setbToStem(true);
        }
        else
        {
            this.bToStem = false;
            this.indexer.setbToStem(false);
        }

        this.indexer.fnReadCache(sPathForObjects);
        this.indexer.fnReadDictionary(sPathForObjects);
        this.indexer.fnReadDocsGrades(sPathForObjects);
        this.mDocInfo = indexer.getHashMapDocsGrade();
        this.searcher = new Searcher(bToStem, this.indexer.getHashMapDocsGrade(), this.sRootPath, this.indexer.getDictionary(), this.indexer.getCache());
        HashSet<String> stopWords = readFile.fnReadStopWords(sPathForObjects + "\\stop_words.txt");
        this.searcher.fnSetStopWords(stopWords);
        fnSetPostingFileReader(sPathForObjects + "\\ConstPost.txt");

        setChanged();
        notifyObservers("Load objects end");
    }

    /**
     * Save the cache in text file under the Resources folder for show on screen.
     */
    public void fnShowCache()
    {
        File file = new File("Resources\\Cache.txt");
        if (!file.exists())
        {
            try
            {
                file.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        BufferedWriter bw = null;
        try
        {
            bw = new BufferedWriter(new FileWriter(file));
            this.indexer.fnWriteCache(bw);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (bw != null)
            {
                try
                {
                    bw.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Save the dictionary under the Resources folder for show on screen
     */
    public void fnShowDictionary()
    {
        HashMap<String, MutableTriple<Integer[], Float, Long>> dictionary = this.indexer.getDictionary();
        TreeSet<String>                                        treeSet    = new TreeSet<>(dictionary.keySet());

        File file = new File("Resources\\Dictionary.txt");
        if (!file.exists())
        {
            try
            {
                file.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        BufferedWriter bw = null;
        try
        {
            bw = new BufferedWriter(new FileWriter(file));
            for (String sTerm : treeSet)
            {
                MutableTriple<Integer[], Float, Long> triple = dictionary.get(sTerm);
                bw.write(sTerm + ", " + triple.getLeft()[1]);
                bw.newLine();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (null != bw)
            {
                try
                {
                    bw.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Initialize random access file in the searcher to read the posting file
     *
     * @param sReadPosting path to the posting file
     */
    public void fnSetPostingFileReader(String sReadPosting)
    {
        if (this.searcher == null)
        {
            this.searcher = new Searcher(bToStem, this.indexer.getHashMapDocsGrade(), this.sRootPath, this.indexer.getDictionary(), this.indexer.getCache());
            HashSet<String> stopWords = readFile.fnReadStopWords("C:\\Users\\IBM_ADMIN\\IdeaProjects\\test1\\corpus\\stop_words.txt");
            this.searcher.fnSetStopWords(stopWords);
        }
        this.searcher.fnInitializeReader(sReadPosting);
    }

    /**
     * Find 5 most important sentences in a specific document in the corpus
     *
     * @param sDocName document name
     */
    public void fnMostImportant(String sDocName)
    {
        DisplayQ = new StringBuilder();
        ArrayList<MutablePair<String, Double>> arrayListPairs = this.searcher.fnMostImportant(sDocName);
        String[]                               result         = new String[5];
        if(arrayListPairs==null){
            setChanged();
            notifyObservers("Top 5 fail : doc or corpus not found");
            return;
        }
        for (int iIndex = 0, iSize = arrayListPairs.size(); iIndex < iSize && iIndex < 5; iIndex++)
        {
            MutablePair<String, Double> pair = arrayListPairs.get(iIndex);
            result[iIndex] = pair.getLeft();
            //*/
            DisplayQ.append("{Grade:").append(5 - iIndex).append("}").append(arrayListPairs.get(iIndex).getLeft()).append("\n");

        }
        setChanged();
        notifyObservers("Top 5 end");
    }

    /**
     * Run a simple and single query. Can be extended by wikipedia page, but at that case the query needed to be
     * single word.
     *
     * @param Query  the query to search for
     * @param extend should we look at wikipedia to improve the query
     */
    public void fnRunSimpleQuery(String Query, boolean extend)
    {

        long                                 startTime = System.currentTimeMillis();
        ArrayList<Map.Entry<String, Double>> ans       = searcher.fnSearch(new StringBuilder(Query), extend);
        int                                  retC      = ans.size();
        String                               docN;
        long                                 endTime   = System.currentTimeMillis();
        double                               totalTime = (endTime - startTime) / Math.pow(10, 3);
        QtoSave = new StringBuilder();
        DisplayQ = new StringBuilder("Number of relevent document return: " + String.valueOf(retC) + "\n");
        DisplayQ.append("Processing time: ").append(String.valueOf(totalTime)).append("\n");
        DisplayQ.append("Documents: ");
        for (int i = 0; i < retC; i++)
        {
            docN = ans.get(i).getKey();
            DisplayQ.append(String.valueOf(docN)).append(", ");
            if (i % 5 == 0 && i != 0)
            {
                DisplayQ.append("\n");
            }
            QtoSave.append("1 1 ").append(String.valueOf(docN)).append(" 1 1.0 mt\n");

        }
        DisplayQ.append(".\n");

        setChanged();
        notifyObservers("SimpleQ returned");
    }

    /**
     * Search number of queries. The file needed to be at treceval format.
     *
     * @param path path to the file
     */
    public void fnRunFileQuery(String path)
    {
        long startTime = System.currentTimeMillis();
        fnRunArrQ(fnTreck(path));
        long   endTime   = System.currentTimeMillis();
        double totalTime = (endTime - startTime) / Math.pow(10, 3);
        DisplayQ.append("Total processing time: ").append(String.valueOf(totalTime)).append("\n");
        setChanged();
        notifyObservers("FileQ end");

    }

    private void fnRunArrQ(ArrayList<MutablePair<String, StringBuilder>> Queries)
    {
        QtoSave = new StringBuilder();
        DisplayQ = new StringBuilder();
        for (int i = 0; i < Queries.size(); i++)
        {
            ArrayList<Map.Entry<String, Double>> ans  = searcher.fnSearch(Queries.get(i).getRight(), false);
            int                                  retC = ans.size();
            String                               docN;
            String                               Qid  = Queries.get(i).getLeft();
            DisplayQ.append("Query id - ").append(Qid).append(" results:\n");
            DisplayQ.append("" + "Number of relevant document return: ").append(String.valueOf(retC)).append("\n");
            DisplayQ.append("Documents: ");
            for (int j = 0; j < retC; j++)
            {
                docN = ans.get(j).getKey();
                DisplayQ.append(String.valueOf(docN)).append(", ");
                if (j % 5 == 0 && j != 0)
                {
                    DisplayQ.append("\n");
                }
                QtoSave.append(Qid).append(" 1 ").append(String.valueOf(docN)).append(" 1 1.0 mt\n");

            }
            DisplayQ.append("\n#################################################\n");
        }

    }

    /**
     * Parse the query file from treceval format to queries we can search.
     *
     * @param sPath path to the file we want to parse
     * @return list of paris: left is the query id in string, the right is the query itself
     */
    private ArrayList<MutablePair<String, StringBuilder>> fnTreck(String sPath)
    {
        BufferedReader                                br              = null;
        ArrayList<MutablePair<String, StringBuilder>> arrayListResult = new ArrayList<>();
        try
        {
            File file = new File(sPath);
            if (file.exists())
            {
                br = new BufferedReader(new FileReader(file));
                String        sID = "";
                String        sLine;
                StringBuilder sb  = new StringBuilder();
                while (null != (sLine = br.readLine()))
                {
                    if (sLine.contains("<title>"))
                    {
                        String[] strings = sLine.split("<title>");
                        sb = new StringBuilder();
                        sb.append(strings[1]);
                    }
                    if (sLine.contains("<num>"))
                    {
                        String[] strings = sLine.split(" ");
                        sID = strings[2];
                    }

                    if (sLine.contains("<desc> Description:"))
                    {

                        while (!(sLine = br.readLine()).contains("<narr>"))
                        {
                            if (sLine.equals(""))
                            {
                                continue;
                            }
                            sb.append(' ').append(sLine);
                        }

                        sb.deleteCharAt(0);
                        MutablePair<String, StringBuilder> pair = new MutablePair<>(sID, sb);
                        arrayListResult.add(pair);
                    }
                }
                return arrayListResult;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return arrayListResult;
        }
        finally
        {
            if (null != br)
            {
                try
                {
                    br.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

        }
        return arrayListResult;
    }

    /**
     * Save the result of the query in a file.
     *
     * @param pathToSave where to save the query
     */
    public void fnSaveQ(String pathToSave) throws IOException {
        File file = new File(pathToSave);
        if (!file.exists())
        {
            try
            {
                file.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }


            BufferedWriter bf = new BufferedWriter(new FileWriter(file));
            bf.write(QtoSave.toString());
            bf.flush();


    }

    public void fnResetQHistory(String sLastQSPath) {
        File file  = new File(sLastQSPath);
        file.delete();
    }
/***********************************to be deleted*****************************/
    public HashMap<String, MutableTriple<Integer[], Float, Long>> fnGetDic() {
        return indexer.getDictionary();
    }

    /**
     * return the cache
     *
     * @return cache
     */
    public HashMap<String, MutablePair<String, Long>> fnGetCache()
    {
        return indexer.getCache();
    }

    /**
     * Calculate the average document length
     */
    public void fnGetAvgDocsLength()
    {
        double dFinal = 0;
        for (MutablePair<double[], String> pair : this.indexer.getHashMapDocsGrade().values())
        {
            dFinal += pair.getLeft()[1];
        }
        dFinal = dFinal / 472525;
        System.out.println(dFinal);
    }

    /**
     * Set the corpus path.
     *
     * @param corpusP path to the corpus
     */
    public void setCorpusP(String corpusP)
    {
        this.sRootPath = corpusP;
        searcher.sCorpusPath = corpusP;
    }


}




