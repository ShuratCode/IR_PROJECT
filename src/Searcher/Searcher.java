package Searcher;

import Parse.Parse;
import Parse.Term;
import Ranker.Ranker;
import ReadFile.ReadFile;
import Stemmer.PorterStemmer;
import Tuple.MutablePair;
import Tuple.MutableTriple;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Searcher
{
    public  String  sCorpusPath;
    private Parse   parse;
    private boolean bWikipedia, bToStem;
    private PorterStemmer                                          stemmer;
    private ReadFile                                               readFile;
    private HashMap<String, MutablePair<double[], String>>         hashMapDocs;
    private HashMap<String, MutableTriple<Integer[], Float, Long>> dictionary;
    private RandomAccessFile                                       randomAccessFile;
    private HashMap<String, MutablePair<String, Long>>             cache;
    private HashSet<String>                                        stopWords;
    private Ranker                                                 ranker;


    public Searcher(boolean bToStem, HashMap<String, MutablePair<double[], String>> hashMapDocs, String sCorpusPath,
                    HashMap<String, MutableTriple<Integer[], Float, Long>> dictionary, HashMap<String, MutablePair<String, Long>> cache)
    {
        this.parse = new Parse();
        this.bToStem = bToStem;
        this.stemmer = new PorterStemmer();
        this.hashMapDocs = hashMapDocs;
        this.sCorpusPath = sCorpusPath;
        this.readFile = new ReadFile();
        this.dictionary = dictionary;
        this.cache = cache;
        this.ranker = new Ranker(hashMapDocs, dictionary, cache);

    }


    /**
     * Will get query and parse it to separate words.
     * Can also bring more data from wikipedia if the right check box is mark in the gui and the query is one word
     *
     * @param sQuery the query to search in the database
     * @return parsed words.
     */
    public ArrayList<Map.Entry<String, Double>> fnSearch(StringBuilder sQuery, boolean bExtend)
    {
        MutablePair<ArrayList<Term>, int[]> pair           = parse.fnParseText1(sQuery, "");
        String[]                            result;
        String[]                            sQueryWords;
        ArrayList<String>                   arrayListQuery = new ArrayList<>();
        if (bExtend)
        {
            String[] arrayWordsFromWikipedia = fnGetWordsFromWikipedia(sQuery);

            arrayListQuery.add(String.valueOf(sQuery));
            arrayListQuery.add(arrayWordsFromWikipedia[0]);
            arrayListQuery.add(arrayWordsFromWikipedia[1]);
            arrayListQuery.add(arrayWordsFromWikipedia[2]);
            arrayListQuery.add(arrayWordsFromWikipedia[3]);
            arrayListQuery.add(arrayWordsFromWikipedia[4]);
            return this.ranker.fnGetBestDocs(arrayListQuery, 70);
        }
        else
        {

            ArrayList<Term> left = pair.getLeft();
            for (int i = 0, leftSize = left.size(); i < leftSize; i++)
            {
                Term term = left.get(i);
                if (bToStem)
                {
                    String sWord = this.stemmer.stemTerm(term.getsName());
                    for(int j=0;j<term.getiNumOfTimes();j++){
                        arrayListQuery.add(sWord);
                    }

                }
                else
                {
                    String sWord = term.getsName();
                    for(int j=0;j<term.getiNumOfTimes();j++){
                        arrayListQuery.add(sWord);
                    }
                }

            }
            return this.ranker.fnGetBestDocs(arrayListQuery, 50);

        }

    }

    /**
     * Get another words from wikipedia page to improve the query. will search in wikipedia for relevant article and
     * will parse it and choose the 5 most important
     *
     * @param sQuery the query to improve
     * @return 5 words to add to the query
     */
    public String[] fnGetWordsFromWikipedia(StringBuilder sQuery)
    {

        try
        {

            Document doc = Jsoup.connect("https://en.wikipedia.org/w/api.php?format=xml&action=query&prop=extracts&titles=" + sQuery + "&redirects=true").get();
            doc = Jsoup.parse(doc.text());
            ArrayList<String> words = countWords(doc.text());
            if (null != words)
            {
                String[] sresult = new String[5];
                for (int i = 0, wordsSize = words.size(), iIndex = 0; i < wordsSize && iIndex < 5; i++)
                {
                    String word = words.get(i);
                    if (word.contentEquals(sQuery))
                    {
                        continue;
                    }
                    sresult[iIndex] = word;
                    iIndex++;
                }

                return sresult;
            }
            return null;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }


    }

    /**
     * set the boolean of the class to check if we need
     *
     * @param bWikipedia true if we want to search in wikipedia, false if not.
     */
    public void setbWikipedia(boolean bWikipedia)
    {
        this.bWikipedia = bWikipedia;
    }

    /**
     * Count the words in the text.
     *
     * @param text the text to count the words
     * @return list of sorted words by the tf in the text. the maximum at the top
     */
    private ArrayList<String> countWords(String text)
    {
        try
        {
            HashMap<String, Integer> countmap = new HashMap<>();
            BufferedReader           reader   = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))));
            String                   sLine;
            while ((sLine = reader.readLine()) != null)
            {

                String[] words = sLine.split("[^A-ZÃƒâ€¦Ãƒâ€žÃƒâ€“a-zÃƒÂ¥ÃƒÂ¤ÃƒÂ¶]+");

                for (int i = 0, wordsLength = words.length; i < wordsLength; i++)
                {
                    String word = words[i];
                    word = word.toLowerCase();
                    word = fnGetNewForm(word);
                    if ("".equals(word) || this.parse.fnIsStopWords(word))
                    {
                        continue;
                    }

                    if (countmap.containsKey(word))
                    {
                        Integer iNewValue = countmap.get(word) + 1;
                        countmap.put(word, iNewValue);
                    }
                    else
                    {
                        if (word.charAt(word.length() - 1) == 's')
                        {
                            String sSingle = word.substring(0, word.length() - 1);
                            if (countmap.containsKey(sSingle))
                            {
                                Integer iNewValue = countmap.get(sSingle) + 1;
                                countmap.put(sSingle, iNewValue);
                            }
                            else
                            {
                                countmap.put(word, 1);
                            }
                        }
                        else
                        {
                            countmap.put(word, 1);
                        }

                    }

                }
            }
            reader.close();
            ArrayList<Map.Entry<String, Integer>> sorted = new ArrayList<>(countmap.entrySet());
            sorted.sort((o1, o2) -> o2.getValue() - o1.getValue());
            ArrayList<String> result = new ArrayList<>(sorted.size());
            for (Map.Entry<String, Integer> entry : sorted)
            {
                result.add(entry.getKey());
            }
            return result;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Change the word many form to single form if we had it in the list.
     *
     * @param word the word to change
     * @return the new form of the word
     */
    private String fnGetNewForm(String word)
    {
        switch (word)
        {
            case "humans":
                return "human";
            case "years":
                return "year";
            case "governments":
                return "government";
            case "groups":
                return "group";
            case "times":
                return "time";
            case "peoples":
                return "people";
            case "states":
                return "state";
            case "ends":
                return "end";
            case "cents":
                return "cent";
            case "parts":
                return "part";
            case "shares":
                return "share";
            case "texts":
                return "text";
            case "reports":
                return "report";
            case "ministers":
                return "minister";
            case "companies":
                return "company";
            case "presidents":
                return "president";

        }
        return word;
    }

    /**
     * Get a stop words set and save it in this class and in the parser
     *
     * @param stopWords Hash set of stop words. can't be null
     */
    public void fnSetStopWords(HashSet<String> stopWords)
    {
        this.stopWords = stopWords;
        this.parse.setHashSetStopWords(stopWords);
    }

    /**
     * Find the most important sentences in a specific doc.
     *
     * @param sDocName the doc we want to get the sentences for
     * @return list of sorted pairs, left is the sentence, the right is the sentence grade
     */
    public ArrayList<MutablePair<String, Double>> fnMostImportant(String sDocName)
    {
        if(!hashMapDocs.containsKey(sDocName)){
            return null;
        }
        String sFileName = this.hashMapDocs.get(sDocName).getRight();
        String sDocPath  = this.sCorpusPath + "\\" + sFileName + "\\" + sFileName;
        File   file      = new File(sDocPath);
        if (!file.exists())
        {
            System.out.println("Problem with file");
        }
        else
        {
            ArrayList<Documnet.Document> docs = this.readFile.fnReadFile(file);
            Documnet.Document            doc  = fnGetCorrectDoc(docs, sDocName);
            if (null != doc)
            {
                ArrayList<String>                      sLines = this.parse.fnGetSentences(doc.getText());
                ArrayList<MutablePair<String, Double>> pairs  = fnGetLinesGrades(sLines, sDocName);
                pairs.sort(Comparator.comparingDouble(MutablePair::getRight));
                Collections.reverse(pairs);
                return pairs;
            }

        }
        return null;
    }

    /**
     * Calculate grade for each sentence. the grade is sum of all the words TF/MaxTF. stop words and words we don't have
     * in the dictionary get the grade 0
     *
     * @param sLines   list of sentences to calculate grade for.
     * @param sDocName the document we calculate the grades for.
     * @return list of pairs with sentence and grade
     */
    private ArrayList<MutablePair<String, Double>> fnGetLinesGrades(ArrayList<String> sLines, String sDocName)
    {
        ArrayList<MutablePair<String, Double>> result = new ArrayList<>();
        for (int i = 0, sLinesSize = sLines.size(); i < sLinesSize; i++)
        {
            String                              sLine         = sLines.get(i);
            MutablePair<ArrayList<Term>, int[]> pair          = this.parse.fnParseText1(new StringBuilder(sLine), sDocName);
            ArrayList<Term>                     termArrayList = pair.getLeft();
            double                              iMaxTF        = this.hashMapDocs.get(sDocName).getLeft()[0];
            double                              dLineGrade    = 0;
            for (int iIndex = 0, sWordsLength = termArrayList.size(); iIndex < sWordsLength; iIndex++)
            {
                String sWord = termArrayList.get(iIndex).getsName();
                if (this.stopWords.contains(sWord))
                {
                    continue;
                }
                int    iTF    = fnGetTF(sWord, sDocName);
                double dGrade = iTF / iMaxTF;
                //result.add(new MutablePair<>(sWord, dGrade));
                dLineGrade += dGrade;
            }
            result.add(new MutablePair<>(sLine, dLineGrade));
        }
        return result;
    }

    /**
     * Find the frequency of term in a specific document by reading the posting file.
     * @param sWord the term to find the tf for.
     * @param sDocName the document we want the tf in.
     * @return tf of the term in the document. 0 if not found
     */
    private int fnGetTF(String sWord, String sDocName)
    {
        MutableTriple<Integer[], Float, Long> triple = this.dictionary.get(sWord);
        if (null != triple)
        {
            long     lPointer = triple.getRight();
            String   sLine;
            String[] strings;
            int      iResult  = 0;
            if (-1 == lPointer)
            {
                sLine = this.cache.get(sWord).getLeft();
                strings = sLine.split("!#");
                iResult = fnGetTFFRomLine(strings, sDocName);
                if (iResult == 0)
                {
                    try
                    {
                        lPointer = this.cache.get(sWord).getRight();
                        this.randomAccessFile.seek(lPointer);
                        sLine = this.randomAccessFile.readLine();
                        strings = sLine.split("!#");
                        iResult = fnGetTFFRomLine(strings, sDocName);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    return iResult;
                }

            }
            else
            {
                try
                {
                    this.randomAccessFile.seek(lPointer);
                    sLine = this.randomAccessFile.readLine();
                    strings = sLine.split("!#");
                    iResult = fnGetTFFRomLine(strings, sDocName);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            return iResult;

        }
        return 0;
    }

    /**
     * Find the current document to get the tf for from posting line
     * @param strings string array of documents names and tfs
     * @param sDocName the specific document to look for
     * @return the correct tf. 0 if not found
     */
    private int fnGetTFFRomLine(String[] strings, String sDocName)
    {
        for (int iIndex = 1, iLength = strings.length; iIndex < iLength; iIndex++)
        {
            String sDocTemp = strings[iIndex];

            iIndex++;
            if (sDocName.equals(sDocTemp))
            {
                String sNum = strings[iIndex];
                return Integer.parseInt(sNum);
            }
        }
        return 0;
    }

    /**
     * Return Document object that is the representation of the document name from a list of document
     * @param docs list of documents objects
     * @param sDocName the specific document name we need
     * @return the document object
     */
    private Documnet.Document fnGetCorrectDoc(ArrayList<Documnet.Document> docs, String sDocName)
    {


        for (int i = 0, docsSize = docs.size(); i < docsSize; i++)
        {
            Documnet.Document document  = docs.get(i);
            StringBuilder     sbDocName = new StringBuilder(document.getName());
            if (sbDocName.charAt(0) == ' ')
            {
                sbDocName.deleteCharAt(0);
            }
            if (sbDocName.charAt(sbDocName.length() - 1) == ' ')
            {
                sbDocName.deleteCharAt(sbDocName.length() - 1);
            }
            if (String.valueOf(sbDocName).equals(sDocName))
            {
                return document;
            }
        }
        return null;
    }

    /**
     * Initialize the random access file object to read the posting file.
     * @param sReadPosting path to the posting file
     */
    public void fnInitializeReader(String sReadPosting)
    {
        File posting = new File(sReadPosting);
        try
        {
            this.randomAccessFile = new RandomAccessFile(posting, "r");
            this.ranker.fnRandomAccessFileInitialize(sReadPosting);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }
}
