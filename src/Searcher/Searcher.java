package Searcher;

import Parse.Parse;
import Parse.Term;
import Stemmer.PorterStemmer;
import Tuple.MutablePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Searcher
{
    private Parse   parse;
    private boolean bWikipedia, bToStem;
    private PorterStemmer stemmer;


    public Searcher(Parse parse, boolean bToStem, PorterStemmer stemmer)
    {
        this.parse = parse;
        this.bToStem = bToStem;
        this.stemmer = stemmer;

    }


    /**
     * Will get query and parse it to separate words.
     * Can also bring more data from wikipedia if the right check box is mark in the gui and the query is one word
     *
     * @param sQuery the query to search in the database
     * @return parsed words.
     */
    public String[] fnSearch(StringBuilder sQuery)
    {
        MutablePair<ArrayList<Term>, int[]> pair = parse.fnParseText1(sQuery, "");
        String[]                            result;
        if (bWikipedia)
        {
            String[] arrayWordsFromWikipedia = fnGetWordsFromWikipedia(sQuery);
            result = null;
        }
        else
        {
            result = new String[pair.getLeft().size()];
            ArrayList<Term> left = pair.getLeft();
            for (int i = 0, leftSize = left.size(); i < leftSize; i++)
            {
                Term term = left.get(i);
                if (bToStem)
                {
                    result[i] = this.stemmer.stemTerm(term.getsName());
                }
                else
                {
                    result[i] = term.getsName();
                }

            }

        }
        return result;
    }

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

    public void setbWikipedia(boolean bWikipedia)
    {
        this.bWikipedia = bWikipedia;
    }

    //TODO: get query and parse it. return query words.

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

    private String fnGetNewForm(String word)
    {
        switch (word)
        {
            case "humans":
                return "human";
        }
        return word;
    }

    public void fnSetStopWords(HashSet<String> stopWords)
    {
        this.parse.setHashSetStopWords(stopWords);
    }


}
