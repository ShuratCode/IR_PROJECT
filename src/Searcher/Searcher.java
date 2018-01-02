package Searcher;

import Parse.Parse;
import Parse.Term;
import Stemmer.PorterStemmer;
import Tuple.MutablePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import sun.reflect.generics.tree.Tree;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

        ArrayList<String> stringArrayList = new ArrayList<>();
        try
        {

            Document doc  = Jsoup.connect("http://en.wikipedia.org/wiki/" + sQuery).get();
            Element  body = doc.body();
            countWords(body.text());
            Elements bold = body.select("b");
            for (int i = 0, boldSize = bold.size(); i < boldSize; i++)
            {
                Element element = bold.get(i);

                if (element.childNodeSize() == 1)
                {
                    Node   e      = element.childNode(0);
                    String sValue = e.toString();
                    if (sValue.length() > 2 && !sValue.contains("<a"))
                    {
                        stringArrayList.add(sValue);
                    }
                }

            }
            Elements links = body.select("a");
            System.out.println();

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public void setbWikipedia(boolean bWikipedia)
    {
        this.bWikipedia = bWikipedia;
    }

    //TODO: get query and parse it. return query words.

    private ArrayList<Map.Entry<String, Integer>> countWords(String text)
    {
        try
        {
            HashMap<String, Integer> countmap = new HashMap<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))));
            String sLine;
            while ((sLine = reader.readLine()) != null)
            {
                String[] words = sLine.split("[^A-ZÃƒâ€¦Ãƒâ€žÃƒâ€“a-zÃƒÂ¥ÃƒÂ¤ÃƒÂ¶]+");
                for (int i = 0, wordsLength = words.length; i < wordsLength; i++)
                {
                    String word = words[i];
                    if ("".equals(word) || this.parse.fnIsStopWords(word))
                    {
                        continue;
                    }
                    ArrayList<Term> terms = this.parse.fnParseText1(new StringBuilder(word), "").getLeft();
                    if (!countmap.containsKey(word))
                    {
                        countmap.put(word, 0);
                    }
                    else
                    {
                        Integer iNewValue = countmap.get(word) + 1;
                        countmap.put(word, iNewValue);
                    }
                }
            }
            reader.close();
            ArrayList<Map.Entry<String, Integer>> sorted = new ArrayList<>(countmap.entrySet());
            Collections.sort(sorted, (o1, o2) -> o2.getValue() - o1.getValue());
            return sorted;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }

    }

    public void fnSetStopWords(HashSet<String> stopWords)
    {
        this.parse.setHashSetStopWords(stopWords);
    }

}
