package Model;


import Documnet.Document;
import Indexer.MyIndexer;
import Parse.Parse;
import Parse.Term;
import ReadFile.ReadFile;
import Stemmer.PorterStemmer;
import Tuple.MutablePair;
import Tuple.MutableTriple;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


/**
 * @author Shaked
 * @since 23-Nov-17
 */
public class MyModel{

    private ReadFile readFile;
    private Parse parse;
    private MyIndexer indexer;
    private String sRootPath, sStopWordsPath, sPathForPosting;
    private ArrayList<String> arrayStringFilesPath;
    private PorterStemmer stemmer;
    private boolean bToStem;
    private int iNumOfDocs, iNumOfParts;

    public String buildBDInfo;




    public MyModel(String sRootPath, boolean bToStem, String sPathForPosting, int iNumOfParts) {
        this.sRootPath = sRootPath;
        this.readFile = new ReadFile();
        this.parse = new Parse();
        this.bToStem = bToStem;
        this.sPathForPosting = sPathForPosting + "\\Posting";
        String sPathForIndexer = fnCreateFolder();
        this.indexer = new MyIndexer(sPathForIndexer, bToStem);
        this.arrayStringFilesPath = new ArrayList<>();
        this.stemmer = new PorterStemmer() {
        };

        this.iNumOfDocs = 0;
        this.iNumOfParts = iNumOfParts;
    }

    /**
     * Build the data base and index from the path we got in the builder.
     * first we will collect all the files paths, and then will send each path to the readFile.
     * after the reading we will send each document to parsing.
     */
    public void fnBuildDB() {
        long startTime = System.currentTimeMillis();
        fnCreateFolder();
        fnFindFilesInDB();
        parse.setHashSetStopWords(readFile.fnReadStopWords(this.sStopWordsPath));
        int iNumOfFiles = this.arrayStringFilesPath.size(); // Num of files in the db
        int iPercent = iNumOfFiles / this.iNumOfParts;
        ArrayList<Document> listDocumentInFile = new ArrayList<>();
        // Looping over all the files in the db
        for (int iIndex = 0, iItr = 0, iTimeToRead = 0; iIndex < iNumOfFiles; iIndex++, iItr++) {
            File fileToRead = new File(this.arrayStringFilesPath.get(iIndex));
            listDocumentInFile.addAll(readFile.fnReadFile(fileToRead)); // All the documents in a file
            if (iItr < iPercent && iTimeToRead < 8) {
                continue;
            }

            if ((iTimeToRead == (this.iNumOfParts - 1)) && (iIndex < (iNumOfFiles - 1))) {
                continue;
            }

            ArrayList<Term> listOfTerms = new ArrayList<>(); // All the terms in the file
            //ArrayList<Term> listOfStemmed = new ArrayList<>();// For Stemmed word
            // Looping over all the documents in each file
            int listDocumentInFileLength = listDocumentInFile.size();
            iNumOfDocs += listDocumentInFileLength;

            for (int iIndex2 = 0; iIndex2 < listDocumentInFileLength; iIndex2++) {
                StringBuilder sbTextToParse = listDocumentInFile.get(iIndex2).getSbText();
                StringBuilder sbDocName = new StringBuilder(listDocumentInFile.get(iIndex2).getsName());
                sbDocName.deleteCharAt(0);
                sbDocName.deleteCharAt(sbDocName.length() - 1);
                listOfTerms.addAll(parse.fnParseText1(sbTextToParse, String.valueOf(sbDocName)));
            }
            if (this.bToStem) {
                for (int iIndex3 = 0, iSize = listOfTerms.size(); iIndex3 < iSize; iIndex3++) {
                    Term termTemp = listOfTerms.get(iIndex3);
                    if (termTemp.getType() == 1) {
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
        try {
            indexer.fnMergePostings();
            long endTime = System.currentTimeMillis();
            double totalTime = (endTime - startTime) / Math.pow(10, 3);
            fnWriteCache();
            buildBDInfo=""+fnCreateEndMessage(totalTime);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * Creates the end message to represent in the gui
     *
     * @param dTotalTime the total running time in seconds
     */
    private StringBuilder fnCreateEndMessage(double dTotalTime) {
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
    private String fnCreateFolder() {
        File file = new File(sPathForPosting);
        String sResult;
        if (!file.exists()) {
            file.mkdir();
        }
        if (this.bToStem) {
            sResult = sPathForPosting + "\\Stemmed";
            File file2 = new File(sResult);
            if (!file2.exists()) {
                file2.mkdir();
            } else {
                fnClearFolder(file2);
            }
        } else {
            sResult = sPathForPosting + "\\Non Stemmed";
            File file2 = new File(sResult);
            if (!file2.exists()) {
                file2.mkdir();
            } else {
                fnClearFolder(file2);
            }
        }
        return sResult;
    }


    /**
     * this function will get all the files in the path we got in the builder
     */
    private void fnFindFilesInDB() {
        File fileDirToRead = new File(this.sRootPath);
        File[] files = fileDirToRead.listFiles(); // Use this method to make sure we will not get an npe in listFiles() method
        if (null != files) {
            for (int iIndex = 0, filesLength = files.length; iIndex < filesLength; iIndex++) {
                File fileEntry = files[iIndex];
                if (null != fileEntry && fileEntry.isDirectory()) {
                    File[] files2 = fileEntry.listFiles();
                    if (null != files2) {
                        int files2Length = files2.length;

                        for (int i = 0; i < files2Length; i++) {
                            this.arrayStringFilesPath.add(files2[i].getAbsolutePath());
                        }
                    }
                } else {
                    if (null != fileEntry) {
                        this.sStopWordsPath = fileEntry.getAbsolutePath();
                    }

                }
            }
        } else {
            System.out.println("The root path you gave is not a folder");
        }
    }

    /**
     * Reset the whole db
     */
    public void fnReset() {
        String sPathForIndexer = fnCreateFolder();
        this.indexer.fnResetIndex();
        File file = new File(this.sPathForPosting);
        File file2 = new File("Resources");
        if (file2.exists()) {
            File[] files = file2.listFiles();
            if (null != files) {
                for (int i = 0, filesLength = files.length; i < filesLength; i++) {
                    File file3 = files[i];
                    if (file3.getName().equals("Cache Words")) {
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
    private void fnClearFolder(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            if (files.length != 0) {
                for (int iIndex = 0, iSize = files.length; iIndex < iSize; iIndex++) {
                    File file2 = files[iIndex];
                    if (!file2.isDirectory()) {
                        file2.delete();
                    } else {
                        fnClearFolder(file2);
                    }
                }
            }
        }
    }

    /**
     * @return dictionary with term and total tf
     */
    public TreeMap<String, Integer> fnGetDictionary() {
        TreeMap<String, Integer> treeMap = new TreeMap<>();
        HashMap<String, MutableTriple<Integer[], Float, Long>> dictionary = this.indexer.getDictionary();
        for (String sTerm : dictionary.keySet()) {
            treeMap.put(sTerm, dictionary.get(sTerm).getLeft()[1]);
        }

        return treeMap;
    }


    /**
     * Set the boolean true if we need to use stemmer and false if we don't need to use stemmer
     *
     * @param bToStem to stem or not to stem
     */
    public void setbToStem(boolean bToStem) {
        this.bToStem = bToStem;
        this.indexer.setbToStem(bToStem);
    }

    /**
     * @return the total number of docs we parse
     */
    public int getiNumOfDocs() {
        return iNumOfDocs;
    }

    private PriorityQueue<MutablePair<String, Float>> fnCreateCahce() {
        HashMap<String, MutableTriple<Integer[], Float, Long>> hashMap = this.indexer.getDictionary();
        PriorityQueue<MutablePair<String, Float>> pairs = new PriorityQueue<>((o1, o2) -> Float.compare(o1.getRight(), o2.getRight()));

        for (String sTerm : hashMap.keySet()) {
            pairs.add(new MutablePair<>(sTerm, hashMap.get(sTerm).getMiddle()));
        }

        return pairs;
    }

    private void fnWriteCache() {
        File file = new File("Resources\\Cache");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        BufferedWriter bf = null;
        try {
            bf = new BufferedWriter(new FileWriter(file));
            PriorityQueue<MutablePair<String, Float>> pairs = fnCreateCahce();
            for (MutablePair<String, Float> pair : pairs) {
                bf.write(pair.getLeft());
                bf.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != bf) {
                try {
                    bf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void fnSaveChache(String sPathForObjects) {
        this.indexer.fnWriteDicAndCache(sPathForObjects);

    }

    public void fnLoadObjects(String sPathForObjects) {
        this.indexer.setPathForObjects(sPathForObjects);
        this.indexer.fnReadCache();
        this.indexer.fnReadDictionary();
    }

    public HashMap<String, String> getCache() {
        return this.indexer.getCache();
    }

    public TreeMap<String, Integer> getDictionary() {
        TreeMap<String, Integer> treeMap = new TreeMap<>();
        HashMap<String, MutableTriple<Integer[], Float, Long>> hashMap = this.indexer.getDictionary();
        for (String sTerm : hashMap.keySet()) {
            treeMap.put(sTerm, hashMap.get(sTerm).getLeft()[1]);
        }

        return treeMap;
    }


}
