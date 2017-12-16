package Indexer;

import Parse.Term;
import Tuple.MutableTriple;

import java.io.*;
import java.util.*;


/**
 * @author Shaked
 * @since 23-Nov-17
 */
public class MyIndexer {
    private HashMap<String, MutableTriple<Integer[], Float, Long>> dictionary; // Represent the dictionary, key is the term and the triple is: left = num of files the terms are in, middle is idf score, long is pointer to posting file
    private int iNumOfPostingFiles; // Count the number of temp posting files we create
    private TreeMap<String, StringBuilder> hashMapTempStrings; // will use to save the line of each term to be to be written to a posting file
    private int iTotalDocs;
    private HashMap<String, String> cache;
    private String sPath, sPathForObject;
    private boolean bToStem;


    /**
     * Creating new Indexer.
     * Also Create new directory With the name Posting
     */
    public MyIndexer(String sPath, boolean bToStem) {
        this.dictionary = new HashMap<>();
        this.hashMapTempStrings = new TreeMap<>();
        this.iNumOfPostingFiles = 0;
        this.iTotalDocs = 0;
        this.cache = new HashMap<>();
        this.sPath = sPath;
        this.sPathForObject = null;
        this.bToStem = bToStem;
    }

    public void setsPath(String sPath) {
        this.sPath = sPath;
    }
    public void setb2Stem(boolean b2Stem){
        this.bToStem=b2Stem;
    }
    /**
     * Set the total number of docs we parse
     *
     * @param iTotalDocs the total docs number we parse
     */
    public void setTotalDocs(int iTotalDocs) {
        this.iTotalDocs = iTotalDocs;
    }

    /**
     * We will update the dictionary and will create new posting file.
     *
     * @param arrayListTerms list of terms we want to index
     */
    public void fnIndexWords(ArrayList<Term> arrayListTerms) {

        if (null == arrayListTerms) {
            System.out.println("Indexer got null as list of terms");
        } else {
            //Collections.sort(arrayListTerms); // Sorting the terms
            this.hashMapTempStrings = new TreeMap<>();

            // Looping over all the terms and update the dictionary and the hashMap
            for (int i = 0, arrayListTermsSize = arrayListTerms.size(); i < arrayListTermsSize; i++) {
                Term term = arrayListTerms.get(i);
                String sTerm = term.getsName();

                if (this.hashMapTempStrings.containsKey(term.getsName())) {
                    fnUpdateTempString(sTerm, term);
                } else {
                    fnAddTempString(sTerm, term);
                }
            }

            fnWriteToTempFile();
            this.hashMapTempStrings = null;
        }
    }



    /**
     * Creates a new line to be written in the temp posting file.
     * We are using the toString() method to get the line.
     *
     * @param sTerm the term we want to create the line for
     * @param term  the object that represent the term.
     */
    private void fnAddTempString(String sTerm, Term term) {
        StringBuilder sbLineForTerm = new StringBuilder(term.toString());
        this.hashMapTempStrings.put(sTerm, sbLineForTerm);
    }


    /**
     * Update the hashMap for a specific term.
     *
     * @param sTerm the term value
     * @param term  the object that represent the term
     */
    private void fnUpdateTempString(String sTerm, Term term) {
        StringBuilder sbLineForTerm = this.hashMapTempStrings.get(sTerm);
        String sTemp = term.toString();
        sbLineForTerm.append(sTemp);

    }

    /**
     * Merge all the temp posting files to one single posting file
     *
     * @throws IOException throws exception from creating a new file or create new RandomAccessFile
     * @see File
     * @see RandomAccessFile
     */
    public void fnMergePostings() throws IOException {
        fnCreateCache();
        File[] files = fnGetAllTempFiles(); // Get all the file in the Posting dir
        RandomAccessFile raf = new RandomAccessFile(fnCreateConstPosing(), "rw");


        if (null != files) {
            int iSize = files.length;
            BufferedReader[] bfrForFiles = fnCreateBufferReaderArray(files); //BufferedReader for each file
            String[] arrayStringLines = new String[iSize]; //array for lines. one for each file

            boolean[] arrFilesDone = new boolean[iSize]; // array to mark if we finished to read each file

            // Initialize the boolean array
            for (int iIndex = 0; iIndex < iSize; iIndex++) {
                arrFilesDone[iIndex] = false;
            }

            // Get Line from each file
            for (int iIndex = 0; iIndex < iSize; iIndex++) {
                arrayStringLines[iIndex] = bfrForFiles[iIndex].readLine();
                if (arrayStringLines[iIndex] == null) // finished to read this file
                {
                    arrFilesDone[iIndex] = true;
                }
            }
            int iIndex = 0;
            long lPointer;
            while (!fnDoneToRead(arrFilesDone)) // loop until we will finish to read all the files
            {
                String[] arrTerms = fnGetTermsName(arrayStringLines, arrFilesDone); //get all the terms from each line
                String sMinTerm = fnGetMinTerm(arrTerms, iIndex, iSize);
                ArrayList<Integer> arrayListSmallestIndex = fnGetSmallest(arrFilesDone, sMinTerm, arrTerms, iIndex, iSize);
                StringBuilder sbLineToWrite = fnCreateConstLine(arrayStringLines, arrayListSmallestIndex);
                lPointer = raf.getFilePointer();
                if (1 == fnAddDictionaryEntry(sMinTerm, sbLineToWrite)) {
                    fnSetIDF(sMinTerm);
                    if (this.cache.containsKey(sMinTerm)) {
                        this.cache.put(sMinTerm, String.valueOf(sbLineToWrite));
                    } else {
                        this.dictionary.get(sMinTerm).setRight(lPointer);

                        raf.writeBytes(String.valueOf(sbLineToWrite));
                        raf.writeBytes("\n");
                    }


                }


                fnUpdateLines(arrayListSmallestIndex, arrayStringLines, bfrForFiles, arrFilesDone);
                iIndex = fnFindStartIndex(arrayStringLines);

            }
            fnCloseBufferReader(bfrForFiles);

            fnDeleteTempFiles(files);
            raf.close();

            fnWriteTemp();


        }


    }

    private int fnAddDictionaryEntry(String sMinTerm, StringBuilder sbLineToWrite) {
        String[] strings = String.valueOf(sbLineToWrite).split("!#");
        int iDocsNum = 0, iTotalTF = 0;
        for (int iIndex = 1, iLength = strings.length; iIndex < iLength; iIndex++) {
            iDocsNum++;
            iIndex++;
            String sTf = strings[iIndex];
            iTotalTF += Integer.parseInt(sTf);
        }
        if (1 == iDocsNum) {
            return -1;
        }
        Integer[] integers = {iDocsNum, iTotalTF};
        Float f = 0f;
        Long l = (long) -1;
        this.dictionary.put(sMinTerm, new MutableTriple<>(integers, f, l));
        return 1;
    }

    /**
     * Read the cache words we saved from before to the memory and add each word to the cache.
     */
    private void fnCreateCache() {
        File fileCache;
        if (bToStem) {
            fileCache = new File("Resources\\Stemmed Cache Words");
        } else {
            fileCache = new File("Resources\\Cache Words");
        }

        BufferedReader bf = null;
        if (fileCache.exists()) {
            try {
                bf = new BufferedReader(new FileReader(fileCache));
                String sLine;
                while (null != (sLine = bf.readLine())) {
                    this.cache.put(sLine, "");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bf != null) {
                    try {
                        {
                            bf.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Write the cache and dictionary files for calculate the size of each one
     */
    private void fnWriteTemp() {
        String sPathForCache, sPathForDic;
        if (bToStem) {
            sPathForCache = "Resources\\Stemmed Cache";
            sPathForDic = "Resources\\Stemmed Dictionary";
        } else {
            sPathForCache = "Resources\\Non Stemmed Cache";
            sPathForDic = "Resources\\Non Stemmed Dictionary";
        }
        ObjectOutputStream outputStreamForCache = null, outputStreamForDictionary;
        {
            try {
                File fileCache = new File(sPathForCache);
                File fileDictionary = new File(sPathForDic);
                if (!fileCache.exists()) {
                    fileCache.createNewFile();
                }
                if (!fileDictionary.exists()) {
                    fileDictionary.createNewFile();
                }
                outputStreamForCache = new ObjectOutputStream(new FileOutputStream(fileCache));
                outputStreamForDictionary = new ObjectOutputStream(new FileOutputStream(fileDictionary));
                outputStreamForCache.writeObject(this.cache);
                outputStreamForDictionary.writeObject(this.dictionary);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (outputStreamForCache != null) {
                    try {
                        outputStreamForCache.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * For each line we combined we read a new line from the temp posting file.
     *
     * @param arrayListSmallestIndex the indexes of the lines we need to combine.
     * @param arrayStringLines       the current lines we read.
     * @param bfrForFiles            reader for each posting lines
     * @param arrFilesDone           array to check each file if he is done
     * @throws IOException can throw IO Exception while reading new lines
     */
    private void fnUpdateLines(ArrayList<Integer> arrayListSmallestIndex, String[] arrayStringLines, BufferedReader[] bfrForFiles, boolean[] arrFilesDone) throws IOException {
        for (int iIndex = 0, iSize = arrayListSmallestIndex.size(); iIndex < iSize; iIndex++) {
            int iFileNum = arrayListSmallestIndex.get(iIndex);

            arrayStringLines[iFileNum] = bfrForFiles[iFileNum].readLine();
            if (null == arrayStringLines[iFileNum]) {
                arrFilesDone[iFileNum] = true;

            }
        }
    }

    /**
     * Delete all the temp posting files
     *
     * @param files array with all the temp posting files
     */
    private void fnDeleteTempFiles(File[] files) {
        for (int i = 0, filesLength = files.length; i < filesLength; i++) {
            File file = files[i];
            if (file.getName().equals("ConstPost.txt")) {
                continue;
            }
            file.delete();

        }
    }

    /**
     * Check if we done to read all the temp posting files
     *
     * @param arrFilesDone array of booleans, false mean we didn't finish to read
     * @return true if all files are finished, false else
     */
    private boolean fnDoneToRead(boolean[] arrFilesDone) {
        for (int iIndex = 0, iSize = arrFilesDone.length; iIndex < iSize; iIndex++) {
            if (!arrFilesDone[iIndex]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Connects all the lines from all the temp posting files to one line for the const posting file
     *
     * @param arrayStringLines       array of lines from all the files
     * @param arrayListSmallestIndex arrayList with the indexes of the lines to connect
     * @return one line to write to the posting file
     */
    private StringBuilder fnCreateConstLine(String[] arrayStringLines, ArrayList<Integer> arrayListSmallestIndex) {
        StringBuilder sbResult = new StringBuilder();
        int iFirst = arrayListSmallestIndex.get(0);
        int iEndOfTerm = arrayStringLines[iFirst].indexOf("!#");
        int iSize = arrayListSmallestIndex.size();
        for (int iIndex = 0; iIndex < iSize; iIndex++) {
            Integer iLineToConnect = arrayListSmallestIndex.get(iIndex);
            String sTemp = arrayStringLines[iLineToConnect].substring(iEndOfTerm);
            sbResult.append(sTemp);
        }

        return sbResult;
    }

    /**
     * Close all the buffered readers used in creating the const posting files
     *
     * @param bfrForFiles array of the buffered readers we need to write
     */
    private void fnCloseBufferReader(BufferedReader[] bfrForFiles) {
        for (int i = 0, bfrForFilesLength = bfrForFiles.length; i < bfrForFilesLength; i++) {
            BufferedReader bf = bfrForFiles[i];
            try {
                bf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates an array with bufferReader for each file in the array
     *
     * @param files array of File Objects to read
     * @return Array of BufferReader for each file
     */
    private BufferedReader[] fnCreateBufferReaderArray(File[] files) {
        int iSize = files.length;
        BufferedReader[] bfrArrayResult = new BufferedReader[iSize];
        for (int iIndex = 0; iIndex < iSize; iIndex++) {
            try {
                FileReader fr = new FileReader(files[iIndex]);
                bfrArrayResult[iIndex] = new BufferedReader(fr);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return bfrArrayResult;
    }

    /**
     * Creates a new posting file. this one will be our last
     *
     * @return File object represent the new posting file
     * @throws IOException can throw exception in createNewFile() method
     */
    private File fnCreateConstPosing() throws IOException {
        String sConstFilePath = this.sPath + "\\ConstPost.txt";
        File fileConstantPosting = new File(sConstFilePath);
        if (!fileConstantPosting.exists()) {
            fileConstantPosting.createNewFile();
        }

        return fileConstantPosting;
    }


    /**
     * Gets all the paths of all the current posting files
     *
     * @return absolute path of all the posting files
     */
    private File[] fnGetAllTempFiles() {
        File fileRoot = new File(sPath);
        if (fileRoot.exists()) {
            return fileRoot.listFiles();
        }
        return null;
    }


    /**
     * Creates temp posting file
     *
     * @return BufferWriter to write to this file
     * @throws IOException can throw exception when creating the file.
     */
    private BufferedWriter fnCreateTempPostingFile() throws IOException {
        this.iNumOfPostingFiles++;
        String sPostingFilePath = sPath + "/" + this.iNumOfPostingFiles + ".txt";
        File postingFile = new File(sPostingFilePath);
        if (!postingFile.exists()) {
            postingFile.createNewFile();
        }
        return new BufferedWriter(new FileWriter(sPostingFilePath));
    }

    /**
     * Writes all the data to the temporary file
     */
    private void fnWriteToTempFile() {
        BufferedWriter bfWriter = null;
        try {
            bfWriter = fnCreateTempPostingFile();
            TreeSet<String> hashSetOfKeys = new TreeSet<>(this.hashMapTempStrings.keySet());
            for (Iterator<String> iterator = hashSetOfKeys.iterator(); iterator.hasNext(); ) {
                String sTerm = iterator.next();
                StringBuilder sbTerm = new StringBuilder(sTerm);
                StringBuilder sbLine = this.hashMapTempStrings.get(sTerm);
                sbTerm.append(sbLine);
                String sLine = String.valueOf(sbTerm);
                bfWriter.write(sLine);
                bfWriter.newLine();
            }


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bfWriter != null) {
                try {
                    bfWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Find all the indexes who contains the less lexicographically term in the array
     *
     * @param arrFilesDone boolean array to check if file is done or not
     * @return ArrayList with all the indexes
     */
    private ArrayList<Integer> fnGetSmallest(boolean[] arrFilesDone, String sMinTerm, String[] arrTerms, int iIndex, int iLength) {
        ArrayList<Integer> arrayListFileNum = new ArrayList<>();

        for (; iIndex < iLength; iIndex++) {
            if (!arrFilesDone[iIndex] && null != arrTerms[iIndex] && sMinTerm.equals(arrTerms[iIndex])) {
                arrayListFileNum.add(iIndex);
            }
        }

        return arrayListFileNum;
    }

    /**
     * Find the less lexicographically term
     *
     * @param arrTerms the terms to check
     * @return the less lexicographically term
     */
    private String fnGetMinTerm(String[] arrTerms, int iIndex, int iLength) {
        String sMin = arrTerms[iIndex];
        iIndex++;
        for (; iIndex < iLength; iIndex++) {
            if (arrTerms[iIndex] != null && sMin.compareTo(arrTerms[iIndex]) > 0) {
                sMin = arrTerms[iIndex];
            }
        }
        return sMin;
    }

    /**
     * Extract the term name from each posting line
     *
     * @param arrLines     the lines we want to get the term name
     * @param arrFilesDone check if files are done
     * @return array with term name
     */
    private String[] fnGetTermsName(String[] arrLines, boolean[] arrFilesDone) {
        int iSize = arrLines.length;
        String[] arrTermName = new String[iSize];
        for (int iIndex = 0; iIndex < iSize; iIndex++) {
            if (!arrFilesDone[iIndex]) {
                String sLine = arrLines[iIndex];
                int iEndOfTerm = sLine.indexOf("!#");
                arrTermName[iIndex] = sLine.substring(0, iEndOfTerm);
            } else {
                arrTermName[iIndex] = null;
            }

        }
        return arrTermName;
    }

    /**
     * Get the first index of the file who didn't end to read
     *
     * @param arrLines lines now in the memory
     * @return index of the first not null line
     */
    private int fnFindStartIndex(String[] arrLines) {
        int iLength = arrLines.length;
        for (int iIndex = 0; iIndex < iLength; iIndex++) {
            if (null != arrLines[iIndex]) {
                return iIndex;
            }
        }
        return -1;
    }

    /**
     * Calculate the idf score for the term. keep it at the triple in the dictionary and return the value
     *
     * @param sTerm term to calculate idf score
     */
    private void fnSetIDF(String sTerm) {
        MutableTriple<Integer[], Float, Long> triple = this.dictionary.get(sTerm);
        Float dIDF = (float) (Math.log(this.iTotalDocs / triple.getLeft()[0]) / Math.log(2));
        triple.setMiddle(dIDF);
    }

    /**
     * Write the cache and the dictionary to a files.
     * Based on the path we got in the gui
     * @param sPathForObjects
     */
    public void fnWriteDicAndCache(String sPathForObjects) {
        this.sPathForObject = sPathForObjects;
        File file = new File(this.sPathForObject + "\\Objects");
        String sPathForDic, sPathForCache;
        if (!file.exists()) {
            file.mkdir();
        }
        if (bToStem) {
            sPathForDic = this.sPathForObject + "Objects\\dicStemmed";
            sPathForCache = this.sPathForObject + "Objects\\cacheStemmed";
        } else {
            sPathForDic = this.sPathForObject + "Objects\\dicNonStemmed)";
            sPathForCache = this.sPathForObject + "Objects\\cacheNonStemmed";
        }


        try {
            ObjectOutputStream outputDic = new ObjectOutputStream(new FileOutputStream(sPathForDic));
            ObjectOutputStream outputCache = new ObjectOutputStream(new FileOutputStream(sPathForCache));

            outputDic.writeObject(this.dictionary);
            outputCache.writeObject(this.cache);

            outputCache.close();
            outputDic.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read dictionary form the disk to memory
     */
    public void fnReadDictionary() {
        String sPathForDic = sPath + "Objects\\dic";
        ObjectInputStream inputStream = null;
        try {
            inputStream = new ObjectInputStream(new FileInputStream(sPathForDic));
            this.dictionary = (HashMap<String, MutableTriple<Integer[], Float, Long>>) inputStream.readObject();
            //inputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Read cache from disk to the memory
     */
    public void fnReadCache() {
        String sPathForCache = sPath + "Objects\\cache";
        ObjectInputStream inputStream = null;
        try {
            inputStream = new ObjectInputStream(new FileInputStream(sPathForCache));
            this.cache = (HashMap<String, String>) inputStream.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    /**
     * Reset the index
     */
    public void fnResetIndex() {
        File file = new File(this.sPath);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (null != files) {
                for (int i = 0, filesLength = files.length; i < filesLength; i++) {
                    File file1 = files[i];
                    file1.delete();
                }
            }
            file.delete();
        }

        if (null != this.sPathForObject) {
            File fileObjects = new File(this.sPathForObject);
            if (fileObjects.exists()) {
                File[] files = fileObjects.listFiles();
                if (null != files) {
                    for (int i = 0, filesLength = files.length; i < filesLength; i++) {
                        File file1 = files[i];
                        file1.delete();
                    }
                }
            }
        }
    }

    public HashMap<String, String> getCache() {
        return this.cache;
    }

    public HashMap<String, MutableTriple<Integer[], Float, Long>> getDictionary() {
        return this.dictionary;
    }

    /**
     * Calculate the cache file size in bytes
     *
     * @return -1 if the cache file is not existed, else the value in long
     */
    public long getCacheSize() {
        String sPathForCache;
        if (bToStem) {
            sPathForCache = "Resources\\Stemmed Cache";
        } else {
            sPathForCache = "Resources\\Non Stemmed Cache";
        }
        File file = new File(sPathForCache);
        if (file.exists()) {
            Long l = file.length();
            file.delete();
            return l;
        } else {
            return -1;
        }
    }

    /**
     * Calculate the index size. The const posting file size and the dictionary size
     *
     * @return the size of index in size, 0 if one of the objects didn't create.
     */
    public long fnGetIndexSize() {
        long lResult = 0;
        String sPathForDic;
        if (bToStem) {
            sPathForDic = "Resources\\Stemmed Dictionary";
        } else {
            sPathForDic = "Resources\\Non Stemmed Dictionary";
        }
        String sConstFilePath = this.sPath + "\\ConstPost.txt";
        File fileDictionary = new File(sPathForDic);
        File fileContPosting = new File(sConstFilePath);

        if (fileDictionary.exists()) {
            if (fileContPosting.exists()) {
                lResult = fileDictionary.length() + fileContPosting.length();
                fileDictionary.delete();
            } else {
                System.out.println("The const posting file was not created");
            }
        } else {
            System.out.println("The dictionary file was not created");
        }
        return lResult;
    }

    public void setbToStem(boolean bToStem) {
        this.bToStem = bToStem;
    }

    public void setPathForObjects(String pathForObjects) {
        this.sPathForObject = pathForObjects;
    }
}
