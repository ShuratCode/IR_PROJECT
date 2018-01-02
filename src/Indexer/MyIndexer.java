package Indexer;

import Parse.Term;
import Tuple.MutablePair;
import Tuple.MutableTriple;

import java.io.*;
import java.util.*;


/**
 * @author Shaked
 * @since 23-Nov-17
 */
public class MyIndexer
{
    private HashMap<String, MutableTriple<Integer[], Float, Long>> dictionary; // Represent the dictionary, key is the term and the triple is: left = num of files the terms are in, middle is idf score, long is pointer to posting file
    private int                                                    iNumOfPostingFiles; // Count the number of temp posting files we create
    private TreeMap<String, StringBuilder>                         hashMapTempStrings; // will use to save the line of each term to be to be written to a posting file
    private int                                                    iTotalDocs;
    private HashMap<String, MutablePair<String, Long>>             cache;
    private String                                                 sPath, sPathForObject;
    private boolean                 bToStem;
    private HashMap<String, Double> hashMapDocsGrade;




    /**
     * Creating new Indexer.
     * Also Create new directory With the name Posting
     */
    public MyIndexer(String sPath, boolean bToStem)
    {
        this.dictionary = new HashMap<>();
        this.hashMapTempStrings = new TreeMap<>();
        this.iNumOfPostingFiles = 0;
        this.iTotalDocs = 0;
        this.cache = new HashMap<>();
        this.sPath = sPath;
        this.sPathForObject = null;
        this.bToStem = bToStem;
        this.hashMapDocsGrade = new HashMap<>();
    }

    /**
     * Set the total number of docs we parse
     *
     * @param iTotalDocs the total docs number we parse
     */
    public void setTotalDocs(int iTotalDocs)
    {
        this.iTotalDocs = iTotalDocs;
    }

    /**
     * We will update the dictionary and will create new posting file.
     *
     * @param arrayListTerms list of terms we want to index
     */
    public void fnIndexWords(ArrayList<Term> arrayListTerms)
    {

        if (null == arrayListTerms)
        {
            System.out.println("Indexer got null as list of terms");
        }
        else
        {
            //Collections.sort(arrayListTerms); // Sorting the terms
            this.hashMapTempStrings = new TreeMap<>();

            // Looping over all the terms and update the dictionary and the hashMap
            for (int i = 0, arrayListTermsSize = arrayListTerms.size(); i < arrayListTermsSize; i++)
            {
                Term   term  = arrayListTerms.get(i);
                String sTerm = term.getsName();

                if (this.hashMapTempStrings.containsKey(term.getsName()))
                {
                    fnUpdateTempString(sTerm, term);
                }
                else
                {
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
    private void fnAddTempString(String sTerm, Term term)
    {
        StringBuilder sbLineForTerm = new StringBuilder(term.toString());
        this.hashMapTempStrings.put(sTerm, sbLineForTerm);
    }

    /**
     * Update the hashMap for a specific term.
     *
     * @param sTerm the term value
     * @param term  the object that represent the term
     */
    private void fnUpdateTempString(String sTerm, Term term)
    {
        StringBuilder sbLineForTerm = this.hashMapTempStrings.get(sTerm);
        String        sTemp         = term.toString();
        sbLineForTerm.append(sTemp);

    }

    /**
     * Merge all the temp posting files to one single posting file
     *
     * @throws IOException throws exception from creating a new file or create new RandomAccessFile
     * @see File
     * @see RandomAccessFile
     */
    public void fnMergePostings() throws IOException
    {
        fnCreateCache();
        File[]           files = fnGetAllTempFiles(); // Get all the file in the Posting dir
        RandomAccessFile raf   = new RandomAccessFile(fnCreateConstPosting(), "rw");


        if (null != files)
        {
            int              iSize            = files.length;
            BufferedReader[] bfrForFiles      = fnCreateBufferReaderArray(files); //BufferedReader for each file
            String[]         arrayStringLines = new String[iSize]; //array for lines. one for each file

            boolean[] arrFilesDone = new boolean[iSize]; // array to mark if we finished to read each file

            // Initialize the boolean array
            for (int iIndex = 0; iIndex < iSize; iIndex++)
            {
                arrFilesDone[iIndex] = false;
            }

            // Get Line from each file
            for (int iIndex = 0; iIndex < iSize; iIndex++)
            {
                arrayStringLines[iIndex] = bfrForFiles[iIndex].readLine();
                if (arrayStringLines[iIndex] == null) // finished to read this file
                {
                    arrFilesDone[iIndex] = true;
                }
            }
            int  iIndex = 0;
            long lPointer;
            while (!fnDoneToRead(arrFilesDone)) // loop until we will finish to read all the files
            {
                String[]           arrTerms               = fnGetTermsName(arrayStringLines, arrFilesDone); //get all the terms from each line
                String             sMinTerm               = fnGetMinTerm(arrTerms, iIndex, iSize);
                ArrayList<Integer> arrayListSmallestIndex = fnGetSmallest(arrFilesDone, sMinTerm, arrTerms, iIndex, iSize);
                StringBuilder      sbLineToWrite          = fnCreateConstLine(arrayStringLines, arrayListSmallestIndex);

                lPointer = raf.getFilePointer();
                if (1 == fnAddDictionaryEntry(sMinTerm, sbLineToWrite))
                {
                    fnSetIDF(sMinTerm);
                    fnAddDocsGrades(sMinTerm, sbLineToWrite);
                    if (this.cache.containsKey(sMinTerm))
                    {
                        fnAddCacheEntry(sMinTerm, sbLineToWrite, raf);
                    }
                    else
                    {
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

    private void fnAddDocsGrades(String sMinTerm, StringBuilder sbLineToWrite)
    {
        String   sLine   = String.valueOf(sbLineToWrite);
        String[] strings = sLine.split("!#");
        for (int iIndex = 1, iSize = strings.length; iIndex < iSize; iIndex++)
        {
            MutablePair<String, Integer> pair = new MutablePair<>(strings[iIndex], Integer.valueOf((strings[iIndex + 1])));
            iIndex++;
            float  fIdf   = this.dictionary.get(sMinTerm).getMiddle();
            double dGrade = fIdf * pair.getRight();
            dGrade = Math.pow(dGrade, 2);
            double dCurr = this.hashMapDocsGrade.get(pair.getLeft());
            this.hashMapDocsGrade.put(pair.getLeft(), dCurr + dGrade);
        }
    }

    /**
     * Add new entry to the cache. Will Save 0.3 percent of the term line, and the rest will write to the posting file
     * and save the pointer.
     *
     * @param sMinTerm      the term to add to the cache
     * @param sbLineToWrite the line of the term
     * @param raf           RandomAccessFile to write the posting file
     * @throws IOException in the write.
     */
    private void fnAddCacheEntry(String sMinTerm, StringBuilder sbLineToWrite, RandomAccessFile raf) throws IOException
    {
        StringBuilder                               sbResult = new StringBuilder();
        String                                      sLine    = String.valueOf(sbLineToWrite);
        String[]                                    strings  = sLine.split("!#");
        PriorityQueue<MutablePair<String, Integer>> pairs    = new PriorityQueue<>(Comparator.comparingInt(MutablePair::getRight));
        for (int iIndex = 1, iSize = strings.length; iIndex < iSize; iIndex++)
        {
            MutablePair<String, Integer> pair = new MutablePair<>(strings[iIndex], Integer.valueOf(strings[iIndex + 1]));
            iIndex++;
            pairs.add(pair);
        }

        int           iNumOfDocsToSave = (int) (pairs.size() * 0.3);
        StringBuilder sbForCache       = new StringBuilder();
        for (int iNum = 0; iNum < iNumOfDocsToSave; iNum++)
        {
            MutablePair<String, Integer> pair = pairs.poll();
            sbForCache.append(pair.toString());
        }

        this.cache.put(sMinTerm, new MutablePair<>(String.valueOf(sbForCache), raf.getFilePointer()));
        int iSize = pairs.size();
        for (int i = 0; i < iSize; i++)
        {
            MutablePair<String, Integer> pair = pairs.poll();
            sbResult.append(pair.toString());
        }
        raf.writeBytes(String.valueOf(sbResult));

    }

    /**
     * Add new Entry to the dictionary. will save the total tf, df, idf and pointer to the posting file
     *
     * @param sMinTerm      the term to save
     * @param sbLineToWrite the line to write to the posting file
     * @return -1 if df is 1, else return 1
     */
    private int fnAddDictionaryEntry(String sMinTerm, StringBuilder sbLineToWrite)
    {
        String[] strings  = String.valueOf(sbLineToWrite).split("!#");
        int      iDocsNum = 0, iTotalTF = 0;
        for (int iIndex = 1, iLength = strings.length; iIndex < iLength; iIndex++)
        {
            iDocsNum++;
            iIndex++;
            String sTf = strings[iIndex];
            iTotalTF += Integer.parseInt(sTf);
        }
        if (1 == iDocsNum)
        {
            return -1;
        }
        Integer[] integers = {iDocsNum, iTotalTF};
        Float     f        = 0f;
        Long      l        = (long) -1;
        this.dictionary.put(sMinTerm, new MutableTriple<>(integers, f, l));
        return 1;
    }

    /**
     * Read the cache words we saved from before to the memory and add each word to the cache.
     */
    private void fnCreateCache()
    {
        File fileCache;
        if (bToStem)
        {
            fileCache = new File("Resources\\Stemmed Cache Words");
        }
        else
        {
            fileCache = new File("Resources\\Cache Words");
        }

        BufferedReader bf = null;
        if (fileCache.exists())
        {
            try
            {
                bf = new BufferedReader(new FileReader(fileCache));
                String sLine;
                while (null != (sLine = bf.readLine()))
                {
                    this.cache.put(sLine, new MutablePair<>("", (long) -1));
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (bf != null)
                {
                    try
                    {
                        {
                            bf.close();
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Write the cache and dictionary files for calculate the size of each one
     */
    private void fnWriteTemp()
    {
        String sPathForCache, sPathForDic, sPathForGrades;
        if (bToStem)
        {
            sPathForCache = "Resources\\Stemmed Cache.txt";
            sPathForDic = "Resources\\Stemmed Dictionary.txt";
            sPathForGrades = "Resources\\Stemmed Grades";
        }
        else
        {
            sPathForCache = "Resources\\Non Stemmed Cache.txt";
            sPathForDic = "Resources\\Non Stemmed Dictionary.txt";
            sPathForGrades = "Resources\\Non Stemmed Docs Grades";
        }

        BufferedWriter     outputStreamForCache = null, outputStreamForDictionary = null;
        ObjectOutputStream outputStream         = null;
        {
            try
            {
                File fileCache      = new File(sPathForCache);
                File fileDictionary = new File(sPathForDic);
                File source         = new File(sPathForGrades);
                if (!fileCache.exists())
                {
                    fileCache.createNewFile();
                }
                if (!fileDictionary.exists())
                {
                    fileDictionary.createNewFile();
                }
                if (!source.exists())
                {
                    source.createNewFile();
                }
                outputStreamForCache = new BufferedWriter(new FileWriter(fileCache));
                outputStreamForDictionary = new BufferedWriter(new FileWriter(fileDictionary));
                outputStream = new ObjectOutputStream(new FileOutputStream(source));

                fnWriteCache(outputStreamForCache);
                fnWriteDictionary(outputStreamForDictionary);
                outputStream.writeObject(this.hashMapDocsGrade);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (outputStreamForCache != null)
                {
                    try
                    {
                        outputStreamForCache.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                if (outputStreamForDictionary != null)
                {
                    try
                    {
                        {
                            outputStreamForDictionary.close();
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                if (outputStream != null)
                {
                    try
                    {
                        outputStream.close();
                    }
                    catch (IOException e)
                    {
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
    private void fnUpdateLines(ArrayList<Integer> arrayListSmallestIndex, String[] arrayStringLines, BufferedReader[] bfrForFiles, boolean[] arrFilesDone) throws IOException
    {
        for (int iIndex = 0, iSize = arrayListSmallestIndex.size(); iIndex < iSize; iIndex++)
        {
            int iFileNum = arrayListSmallestIndex.get(iIndex);

            arrayStringLines[iFileNum] = bfrForFiles[iFileNum].readLine();
            if (null == arrayStringLines[iFileNum])
            {
                arrFilesDone[iFileNum] = true;

            }
        }
    }

    /**
     * Delete all the temp posting files
     *
     * @param files array with all the temp posting files
     */
    private void fnDeleteTempFiles(File[] files)
    {
        for (int i = 0, filesLength = files.length; i < filesLength; i++)
        {
            File file = files[i];
            if (file.getName().equals("ConstPost.txt"))
            {
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
    private boolean fnDoneToRead(boolean[] arrFilesDone)
    {
        for (int iIndex = 0, iSize = arrFilesDone.length; iIndex < iSize; iIndex++)
        {
            if (!arrFilesDone[iIndex])
            {
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
    private StringBuilder fnCreateConstLine(String[] arrayStringLines, ArrayList<Integer> arrayListSmallestIndex)
    {
        StringBuilder sbResult   = new StringBuilder();
        int           iFirst     = arrayListSmallestIndex.get(0);
        int           iEndOfTerm = arrayStringLines[iFirst].indexOf("!#");
        int           iSize      = arrayListSmallestIndex.size();
        for (int iIndex = 0; iIndex < iSize; iIndex++)
        {
            Integer iLineToConnect = arrayListSmallestIndex.get(iIndex);
            String  sTemp          = arrayStringLines[iLineToConnect].substring(iEndOfTerm);
            sbResult.append(sTemp);
        }

        return sbResult;
    }

    /**
     * Close all the buffered readers used in creating the const posting files
     *
     * @param bfrForFiles array of the buffered readers we need to write
     */
    private void fnCloseBufferReader(BufferedReader[] bfrForFiles)
    {
        for (int i = 0, bfrForFilesLength = bfrForFiles.length; i < bfrForFilesLength; i++)
        {
            BufferedReader bf = bfrForFiles[i];
            try
            {
                bf.close();
            }
            catch (IOException e)
            {
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
    private BufferedReader[] fnCreateBufferReaderArray(File[] files)
    {
        int              iSize          = files.length;
        BufferedReader[] bfrArrayResult = new BufferedReader[iSize];
        for (int iIndex = 0; iIndex < iSize; iIndex++)
        {
            try
            {
                FileReader fr = new FileReader(files[iIndex]);
                bfrArrayResult[iIndex] = new BufferedReader(fr);
            }
            catch (FileNotFoundException e)
            {
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
    private File fnCreateConstPosting() throws IOException
    {
        String sConstFilePath      = this.sPath + "\\ConstPost.txt";
        File   fileConstantPosting = new File(sConstFilePath);
        if (!fileConstantPosting.exists())
        {
            fileConstantPosting.createNewFile();
        }

        return fileConstantPosting;
    }

    /**
     * Gets all the paths of all the current posting files
     *
     * @return absolute path of all the posting files
     */
    private File[] fnGetAllTempFiles()
    {
        File fileRoot = new File(sPath);
        if (fileRoot.exists())
        {
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
    private BufferedWriter fnCreateTempPostingFile() throws IOException
    {
        this.iNumOfPostingFiles++;
        String sPostingFilePath = sPath + "/" + this.iNumOfPostingFiles + ".txt";
        File   postingFile      = new File(sPostingFilePath);
        if (!postingFile.exists())
        {
            postingFile.createNewFile();
        }
        return new BufferedWriter(new FileWriter(sPostingFilePath));
    }

    /**
     * Writes all the data to the temporary file
     */
    private void fnWriteToTempFile()
    {
        BufferedWriter bfWriter = null;
        try
        {
            bfWriter = fnCreateTempPostingFile();
            TreeSet<String> hashSetOfKeys = new TreeSet<>(this.hashMapTempStrings.keySet());
            for (Iterator<String> iterator = hashSetOfKeys.iterator(); iterator.hasNext(); )
            {
                String        sTerm  = iterator.next();
                StringBuilder sbTerm = new StringBuilder(sTerm);
                StringBuilder sbLine = this.hashMapTempStrings.get(sTerm);
                sbTerm.append(sbLine);
                String sLine = String.valueOf(sbTerm);
                bfWriter.write(sLine);
                bfWriter.newLine();
            }


        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (bfWriter != null)
            {
                try
                {
                    bfWriter.close();
                }
                catch (IOException e)
                {
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
    private ArrayList<Integer> fnGetSmallest(boolean[] arrFilesDone, String sMinTerm, String[] arrTerms, int iIndex, int iLength)
    {
        ArrayList<Integer> arrayListFileNum = new ArrayList<>();

        for (; iIndex < iLength; iIndex++)
        {
            if (!arrFilesDone[iIndex] && null != arrTerms[iIndex] && sMinTerm.equals(arrTerms[iIndex]))
            {
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
    private String fnGetMinTerm(String[] arrTerms, int iIndex, int iLength)
    {
        String sMin = arrTerms[iIndex];
        iIndex++;
        for (; iIndex < iLength; iIndex++)
        {
            if (arrTerms[iIndex] != null && sMin.compareTo(arrTerms[iIndex]) > 0)
            {
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
    private String[] fnGetTermsName(String[] arrLines, boolean[] arrFilesDone)
    {
        int      iSize       = arrLines.length;
        String[] arrTermName = new String[iSize];
        for (int iIndex = 0; iIndex < iSize; iIndex++)
        {
            if (!arrFilesDone[iIndex])
            {
                String sLine      = arrLines[iIndex];
                int    iEndOfTerm = sLine.indexOf("!#");
                arrTermName[iIndex] = sLine.substring(0, iEndOfTerm);
            }
            else
            {
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
    private int fnFindStartIndex(String[] arrLines)
    {
        int iLength = arrLines.length;
        for (int iIndex = 0; iIndex < iLength; iIndex++)
        {
            if (null != arrLines[iIndex])
            {
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
    private void fnSetIDF(String sTerm)
    {
        MutableTriple<Integer[], Float, Long> triple = this.dictionary.get(sTerm);
        Float                                 dIDF   = (float) (Math.log(this.iTotalDocs / triple.getLeft()[0]) / Math.log(2));
        triple.setMiddle(dIDF);
    }

    /**
     * Write the cache and the dictionary to a files.
     * Based on the path we got in the gui
     *
     * @param sPathForObjects Path to write the cache and dictionary
     */
    public void fnWriteDicAndCache(String sPathForObjects)
    {
        this.sPathForObject = sPathForObjects;
        String   sPathForDic, sPathForCache;
        String[] sPaths = fnGetPathForDicAndCache();
        sPathForDic = sPaths[0];
        sPathForCache = sPaths[1];

        File fileForDictionary = new File(sPathForDic);
        File fileForCache      = new File(sPathForCache);
        if (!fileForDictionary.exists())
        {
            try
            {
                fileForDictionary.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        if (!fileForCache.exists())
        {
            try
            {
                fileForCache.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }


        try
        {
            BufferedWriter outputDic   = new BufferedWriter(new FileWriter(sPathForDic));
            BufferedWriter outputCache = new BufferedWriter(new FileWriter(sPathForCache));

            fnWriteDictionary(outputDic);
            fnWriteCache(outputCache);

            outputCache.close();
            outputDic.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Read dictionary form the disk to memory
     */
    public void fnReadDictionary(String sPathToRead)
    {
        String sPathForDictionary;
        if (bToStem)
        {
            sPathForDictionary = sPathToRead + "\\dicStemmed";
        }
        else
        {
            sPathForDictionary = sPathToRead + "\\dicNonStemmed";
        }

        BufferedReader br   = null;
        File           file = new File(sPathForDictionary);
        try
        {
            if (!file.exists())
            {
                System.out.println("We can't read the dictionary if you don't have file in this path");
            }
            else
            {
                br = new BufferedReader(new FileReader(file));
                String sLine;
                while ((sLine = br.readLine()) != null)
                {
                    int       iEndIndex = sLine.indexOf(':');
                    String    sTerm     = sLine.substring(0, iEndIndex);
                    String    sData     = sLine.substring(iEndIndex + 1);
                    String[]  arrData   = sData.split(",");
                    Integer[] integers  = {Integer.valueOf(arrData[0]), Integer.valueOf(arrData[1])};
                    Float     f         = Float.valueOf(arrData[2]);
                    Long      l         = Long.valueOf(arrData[3]);
                    this.dictionary.put(sTerm, new MutableTriple<>(integers, f, l));
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (br != null)
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
    }

    /**
     * Read cache from disk to the memory
     */
    public void fnReadCache(String sPathToRead)
    {
        String sPathForCache;
        if (bToStem)
        {
            sPathForCache = sPathToRead + "\\cacheStemmed";
        }
        else
        {
            sPathForCache = sPathToRead + "\\cacheNonStemmed";
        }

        BufferedReader br   = null;
        File           file = new File(sPathForCache);
        try
        {
            if (!file.exists())
            {
                System.out.println("We can't read the cache if you don't have file");
            }
            else
            {
                br = new BufferedReader(new FileReader(file));
                String sLine;
                while ((sLine = br.readLine()) != null)
                {
                    int      iEndIndex = sLine.indexOf(":");
                    String   sTerm     = sLine.substring(0, iEndIndex);
                    String   sData     = sLine.substring(iEndIndex + 1);
                    String[] arrData   = sData.split("&&");
                    this.cache.put(sTerm, new MutablePair<>(arrData[0], Long.valueOf(arrData[1])));
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
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

    }

    /**
     * Reset the index
     */
    public void fnResetIndex()
    {
        fnDeletePosting();
    }

    /**
     * Delete the posting file we saved after the last index run
     */
    private void fnDeletePosting()
    {
        File file = new File(this.sPath);
        if (file.exists())
        {
            File[] files = file.listFiles();
            if (null != files)
            {
                for (int i = 0, filesLength = files.length; i < filesLength; i++)
                {
                    File file1 = files[i];
                    file1.delete();
                }
            }
            file.delete();
        }
    }

    /**
     * Get the dictionary now save in the memory
     *
     * @return dictionary
     */
    public HashMap<String, MutableTriple<Integer[], Float, Long>> getDictionary()
    {
        return this.dictionary;
    }

    /**
     * Calculate the cache file size in bytes
     *
     * @return -1 if the cache file is not existed, else the value in long
     */
    public long getCacheSize()
    {
        String sPathForCache;
        if (bToStem)
        {
            sPathForCache = "Resources\\Stemmed Cache.txt";
        }
        else
        {
            sPathForCache = "Resources\\Non Stemmed Cache.txt";
        }
        File file = new File(sPathForCache);
        if (file.exists())
        {
            Long l = file.length();
            file.delete();
            return l;
        }
        else
        {
            return -1;
        }
    }

    /**
     * Calculate the index size. The const posting file size and the dictionary size
     *
     * @return the size of index in size, 0 if one of the objects didn't create.
     */
    public long fnGetIndexSize()
    {
        long   lResult = 0;
        String sPathForDic;
        if (bToStem)
        {
            sPathForDic = "Resources\\Stemmed Dictionary.txt";
        }
        else
        {
            sPathForDic = "Resources\\Non Stemmed Dictionary.txt";
        }
        String sConstFilePath  = this.sPath + "\\ConstPost.txt";
        File   fileDictionary  = new File(sPathForDic);
        File   fileContPosting = new File(sConstFilePath);

        if (fileDictionary.exists())
        {
            if (fileContPosting.exists())
            {
                lResult = fileDictionary.length() + fileContPosting.length();
                fileDictionary.delete();
            }
            else
            {
                System.out.println("The const posting file was not created");
            }
        }
        else
        {
            System.out.println("The dictionary file was not created");
        }
        return lResult;
    }

    /**
     * Set new value to bToStem
     *
     * @param bToStem true to use stemmer, else false
     */
    public void setbToStem(boolean bToStem)
    {
        this.bToStem = bToStem;
    }

    /**
     * Writes the dictionary as a text file.
     *
     * @param bf Buffered Writer we use to write with. should be initialize to the file we want to write to.
     *           The caller need to close this.
     * @throws IOException BufferedWriter Exceptions.
     */
    private void fnWriteDictionary(BufferedWriter bf) throws IOException
    {
        for (String sTerm : this.dictionary.keySet())
        {
            MutableTriple<Integer[], Float, Long> triple = this.dictionary.get(sTerm);
            bf.write(sTerm + ":");
            bf.write(triple.getLeft()[0] + "," + triple.getLeft()[1] + "," + triple.getMiddle() + "," + triple.getRight());
            bf.newLine();
        }
    }

    /**
     * Write the cache as text file
     *
     * @param bf use this to write the cache, should be initialize with the file we want to write to.
     *           Caller need to close this.
     * @throws IOException buffered writer exceptions
     */
    public void fnWriteCache(BufferedWriter bf) throws IOException
    {
        TreeSet<String> treeSet = new TreeSet<>(this.cache.keySet());
        for (String sTerm : treeSet)
        {
            MutablePair<String, Long> pair = this.cache.get(sTerm);
            bf.write(sTerm + ":");
            bf.write(pair.getLeft() + "&&" + pair.getRight());
            bf.newLine();
        }
    }

    /**
     * Set the path for saving the cache and dictionary
     *
     * @param pathForObjects directory. should be existed.
     */
    public void setPathForObjects(String pathForObjects)
    {
        this.sPathForObject = pathForObjects;
    }

    /**
     * Build a string for path to save the dictionary and cache.
     *
     * @return full path to the file of the dictionary (first cell in the array), full path for the cache file
     * (second cell in the array)
     */
    private String[] fnGetPathForDicAndCache()
    {
        String sPathForDic, sPathForCache;
        if (bToStem)
        {
            sPathForDic = this.sPathForObject + "\\dicStemmed";
            sPathForCache = this.sPathForObject + "\\cacheStemmed";
        }
        else
        {
            sPathForDic = this.sPathForObject + "\\dicNonStemmed";
            sPathForCache = this.sPathForObject + "\\cacheNonStemmed";
        }

        return new String[]{sPathForDic, sPathForCache};
    }

    public float fnGetIDFGrade(String sTerm)
    {
        return this.dictionary.get(sTerm).getMiddle();
    }

    public void fnReadDocsGrades(String sPathForObjects)
    {
        String sPathForGrades;
        if (bToStem)
        {
            sPathForGrades = "Resources\\Stemmed Grades";
        }
        else
        {
            sPathForGrades = "Resources\\Non Stemmed Docs Grades";
        }
        File              source      = new File(sPathForGrades);
        ObjectInputStream inputStream = null;
        if (source.exists())
        {
            try
            {
                inputStream = new ObjectInputStream(new FileInputStream(source));
                this.hashMapDocsGrade = (HashMap<String, Double>) inputStream.readObject();
            }
            catch (FileNotFoundException | ClassNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (null != inputStream)
                {
                    try
                    {
                        inputStream.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    public HashMap<String, Double> getHashMapDocsGrade()
    {
        return hashMapDocsGrade;
    }
}
