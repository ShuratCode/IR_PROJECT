package Parse;

import Tuple.MutablePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

/**
 * this class take adocument and parse it intoo list of terms
 */
public class Parse
    {


    private HashSet<String> hashSetStopWords;

    public void setHashSetStopWords(HashSet<String> hashSetStopWords)
        {
        this.hashSetStopWords = hashSetStopWords;
        }


    /**
     * this is the main method,
     * it takes text and return a list of terms
     *
     * @param sbText ,docName
     *
     * @return arraylist of terms
     */
    public MutablePair<ArrayList<Term>, int[]> fnParseText1(StringBuilder sbText, String docName)
        {

        String[] arrStringWords = sbText.toString().split("[ \t\n\r\f:;?!`/|()<#>*&+=]");  // delimeters
        String sWord, dd, mm, yyyy, bigLetters, number;
        int orgLen;
        HashMap<String, Term> termMap = new HashMap<>();
        boolean isDirty;
        for (int iIndex = 0, arrayStringWordsLength = arrStringWords.length; iIndex < arrayStringWordsLength; iIndex++)
            {
            sWord = arrStringWords[iIndex];
            sWord = fnCleanStart(sWord);
            orgLen = sWord.length();
            isDirty = orgLen != sWord.length();
            if (sWord.length() <= 1)
                {
                continue;
                }
            if ('a' <= sWord.charAt(0) && 'z' >= sWord.charAt(0))
                {
                sWord = fnCleanS(sWord);
                orgLen = sWord.length();
                sWord = fnRemoveAllCharOfKind(sWord, '.');
                sWord = fnRemoveAllCharOfKind(sWord, ',');
                isDirty = orgLen != sWord.length();
                if (sWord.length() <= 1 || isDirty)
                    {
                    continue;
                    }
                if (hashSetStopWords.contains(sWord))
                    {
                    continue;
                    }
                if (termMap.containsKey(sWord))
                    {
                    termMap.get(sWord).raisC();
                    }
                else
                    {
                    termMap.put(sWord, new Term(sWord, docName, 1, iIndex, 1));
                    }
                }
            else if ('A' <= sWord.charAt(0) && 'Z' >= sWord.charAt(0))//if big letter
                {
                if (!Objects.equals("", mm = fnIsMonth(sWord)))
                    {//date parsing starts with month
                    if (iIndex + 1 < arrayStringWordsLength)
                        {
                        dd = fnCleanS(arrStringWords[iIndex + 1]);
                        if (2 >= dd.length() && 31 >= S2Int(dd) && 0 < S2Int(dd) && !dd.equals(""))
                            {
                            if (iIndex + 2 < arrayStringWordsLength)
                                {
                                yyyy = fnRemoveAllCharOfKind(arrStringWords[iIndex + 2], ',');
                                if (0 != S2Int(yyyy) && 4 == yyyy.length())
                                    {
                                    if (termMap.containsKey(dd + "/" + mm + "/" + yyyy))
                                        {
                                        termMap.get(dd + "/" + mm + "/" + yyyy).raisC();
                                        }
                                    else
                                        {
                                        termMap.put(dd + "/" + mm + "/" + yyyy, new Term(dd + "/" + mm + "/" + yyyy, docName, 1, iIndex, 2));
                                        }
                                    iIndex += 2;
                                    }
                                else
                                    {
                                    if (termMap.containsKey(dd + "/" + mm))
                                        {
                                        termMap.get(dd + "/" + mm).raisC();
                                        }
                                    else
                                        {
                                        termMap.put(dd + "/" + mm, new Term(dd + "/" + mm, docName, 1, iIndex, 2));
                                        }
                                    iIndex += 1;
                                    }
                                }
                            }
                        else if (0 != S2Int(dd) && 4 == dd.length())
                            {//dd is yyyy
                            if (termMap.containsKey(mm + "/" + dd))
                                {
                                termMap.get(mm + "/" + dd).raisC();
                                }
                            else
                                {
                                termMap.put(mm + "/" + dd, new Term(mm + "/" + dd, docName, 1, iIndex, 2));
                                }
                            iIndex += 1;
                            }
                        }
                    }
                else
                    {// parsing big letters

                    //sWord = fnRemoveAllCharOfKind(sWord, '.');
                    //sWord = fnRemoveAllCharOfKind(sWord, ',');
                    sWord = fnCleanS(sWord);
                    isDirty = orgLen != sWord.length();
                    if (sWord.length() > 1 && isDirty)
                        {
                        if (!this.hashSetStopWords.contains(sWord = sWord.toLowerCase()) && !Objects.equals(sWord, ""))
                            {
                            if (termMap.containsKey(sWord))
                                {
                                termMap.get(sWord).raisC();
                                }
                            else
                                {
                                termMap.put(sWord, new Term(sWord, docName, 1, iIndex, 1));
                                continue;
                                }

                            }
                        }
                    if (sWord.length() <= 1)
                        {
                        continue;
                        }
                    sWord = sWord.toLowerCase();
                    if (Objects.equals(sWord, "mr") || Objects.equals(sWord, "ms") || Objects.equals(sWord, "mrs") || Objects.equals(sWord, "miss"))
                        {
                        continue;
                        }
                    bigLetters = sWord;

                    while (iIndex + 1 < arrayStringWordsLength)
                        {// saves each word
                        if (!this.hashSetStopWords.contains(sWord = sWord.toLowerCase()) && !Objects.equals(sWord, ""))
                            {
                            if (termMap.containsKey(sWord))
                                {
                                termMap.get(sWord).raisC();
                                }
                            else
                                {
                                termMap.put(sWord, new Term(sWord, docName, 1, iIndex, 1));
                                }

                            }
                        if (!isDirty && arrStringWords[iIndex + 1].length() > 0 && 'Z' >= arrStringWords[iIndex + 1].charAt(0) && 'A' <= arrStringWords[iIndex + 1].charAt(0))
                            {
                            iIndex++;
                            sWord = arrStringWords[iIndex].toLowerCase();
                            sWord = fnCleanS(sWord);
                            sWord = fnRemoveAllCharOfKind(sWord, '.');
                            sWord = fnRemoveAllCharOfKind(sWord, ',');
                            isDirty = sWord.length() != arrStringWords[iIndex].length();//set dirty bit
                            if (Objects.equals(sWord, "mr") || Objects.equals(sWord, "ms") || Objects.equals(sWord, "mrs") || Objects.equals(sWord, "miss"))
                                {
                                break;
                                }
                            bigLetters += (" " + sWord);
                            }
                        else
                            {
                            break;
                            }
                        }
                    if (bigLetters.length() != sWord.length() && !Objects.equals(bigLetters, ""))
                        {// save the hole thing if needed
                        if (termMap.containsKey(bigLetters = bigLetters.toLowerCase()))
                            {
                            termMap.get(bigLetters).raisC();
                            }
                        else
                            {
                            if (bigLetters.charAt(bigLetters.length() - 1) == ' ')
                                {
                                continue;
                                }
                            termMap.put(bigLetters, new Term(bigLetters, docName, 1, iIndex, 5));
                            }
                        }
                    }

                }
            else
                {//nums and so
                sWord = fnRemoveAllCharOfKind(sWord, ',');
                sWord = fnCleanS(sWord);
                if (Objects.equals(sWord, ""))
                    {
                    continue;
                    }
                if ('$' == sWord.charAt(0))
                    {
                    number = fnParseNumber(sWord.substring(1));
                    if (Objects.equals(number, ""))
                        {
                        continue;
                        }
                    if (termMap.containsKey("$" + number))
                        {
                        termMap.get("$" + number).raisC();
                        }
                    else
                        {
                        termMap.put("$" + number, new Term("$" + number, docName, 1, iIndex, 6));
                        }
                    }
                else if (2 == sWord.length())
                    {// check is date
                    dd = sWord;
                    if (31 >= S2Int(dd) && 0 < S2Int(dd) && S2Int(dd) != 0)
                        {
                        if (iIndex + 1 < arrayStringWordsLength)
                            {
                            if (!Objects.equals("", mm = fnIsMonth(arrStringWords[iIndex + 1])))
                                {
                                if (iIndex + 2 < arrayStringWordsLength)
                                    {
                                    yyyy = fnRemoveAllCharOfKind(arrStringWords[iIndex + 2], ',');
                                    if (0 != S2Int(yyyy) && (4 == yyyy.length()))
                                        {
                                        if (termMap.containsKey(dd + "/" + mm + "/" + yyyy))
                                            {
                                            termMap.get(dd + "/" + mm + "/" + yyyy).raisC();
                                            }
                                        else
                                            {
                                            termMap.put(dd + "/" + mm + "/" + yyyy, new Term(dd + "/" + mm + "/" + yyyy, docName, 1, iIndex, 2));
                                            }
                                        iIndex += 2;
                                        }
                                    else if (2 == yyyy.length() && 0 != S2Int(yyyy))
                                        {
                                        if (termMap.containsKey(dd + "/" + mm + "/" + yyyy))
                                            {
                                            termMap.get(dd + "/" + mm + "/" + yyyy).raisC();
                                            }
                                        else
                                            {
                                            termMap.put(dd + "/" + mm + "/" + yyyy, new Term(dd + "/" + mm + "/" + yyyy, docName, 1, iIndex, 2));
                                            }
                                        iIndex += 2;
                                        }
                                    else
                                        {
                                        if (termMap.containsKey(dd + "/" + mm))
                                            {
                                            termMap.get(dd + "/" + mm).raisC();
                                            }
                                        else
                                            {
                                            termMap.put(dd + "/" + mm, new Term(dd + "/" + mm, docName, 1, iIndex, 2));
                                            }
                                        iIndex += 1;
                                        }
                                    }
                                }
                            }
                        }
                    }
                else if (4 == sWord.length() && 't' == sWord.charAt(2) && 'h' == sWord.charAt(3))
                    {
                    dd = sWord.substring(0, 2);
                    if (31 >= S2Int(dd) && 0 < S2Int(dd))
                        {
                        if (iIndex + 1 < arrayStringWordsLength)
                            {
                            if (!Objects.equals("", mm = fnIsMonth(arrStringWords[iIndex + 1])))
                                {
                                if (iIndex + 2 < arrayStringWordsLength)
                                    {
                                    yyyy = fnRemoveAllCharOfKind(arrStringWords[iIndex + 2], ',');
                                    if (0 != S2Int(yyyy) && (4 == yyyy.length()))
                                        {
                                        if (termMap.containsKey(dd + "/" + mm + "/" + yyyy))
                                            {
                                            termMap.get(dd + "/" + mm + "/" + yyyy).raisC();
                                            }
                                        else
                                            {
                                            termMap.put(dd + "/" + mm + "/" + yyyy, new Term(dd + "/" + mm + "/" + yyyy, docName, 1, iIndex, 2));
                                            }
                                        iIndex += 2;
                                        }
                                    }
                                }
                            }
                        }
                    }
                else
                    {
                    if ('%' == sWord.charAt(sWord.length() - 1))
                        {//perecent
                        number = fnParseNumber(sWord.substring(0, sWord.length() - 1));
                        if (Objects.equals(number, ""))
                            {
                            continue;
                            }
                        if (hashSetStopWords.contains(number + " percent"))
                            {
                            continue;
                            }
                        if (termMap.containsKey(number + " percent"))
                            {
                            termMap.get(number + " percent").raisC();
                            }
                        else
                            {
                            termMap.put(number + " percent", new Term(number + " percent", docName, 1, iIndex, 3));
                            }
                        }
                    else
                        {
                        number = fnParseNumber(sWord);
                        if (Objects.equals(number, ""))
                            {
                            continue;
                            }
                        if (iIndex + 1 < arrayStringWordsLength && (Objects.equals("percent", arrStringWords[iIndex + 1]) || Objects.equals("percentage", arrStringWords[iIndex + 1])))
                            {
                            if (hashSetStopWords.contains(number + " percent"))
                                {
                                continue;
                                }
                            if (termMap.containsKey(number + " percent"))
                                {
                                termMap.get(number + " percent").raisC();
                                }
                            else
                                {
                                termMap.put(number + " percent", new Term(number + " percent", docName, 1, iIndex, 3));
                                }
                            iIndex++;
                            }
                        else if (!Objects.equals(number, ""))
                            {
                            if (hashSetStopWords.contains(number))
                                {
                                continue;
                                }
                            if (termMap.containsKey(number))
                                {
                                termMap.get(number).raisC();
                                }
                            else
                                {
                                termMap.put(number, new Term(number, docName, 1, iIndex, 4));
                                }
                            }
                        }
                    }
                }
            }
        ArrayList<Term> res = new ArrayList<>(termMap.values());
        int MaxTF = 0, DLen = 0, temp;
        for (int i = 0; i < res.size(); i++)
            {
            temp = res.get(i).getiNumOfTimes();
            DLen += temp;
            if (temp > MaxTF)
                {
                MaxTF = temp;
                }

            }
        int[] mem = {MaxTF, DLen};
        return new MutablePair<>(res, mem);
        }

    /**
     * clean strings: takes out symbols that are not part of the term
     *
     * @param s
     *
     * @return clean string
     */
    private String fnCleanS(String s)
        {
        while (s.length() > 0 && (s.charAt(0) == ',' || s.charAt(0) == '.' || s.charAt(0) == '/' || s.charAt(0) == '"' || s.charAt(0) == '[' || s.charAt(0) == ']' || s.charAt(0) == '-'))
            {
            s = s.substring(1);

            }
        int iSize = s.length();
        while (iSize > 0 && (s.charAt(iSize - 1) == ',' || s.charAt(iSize - 1) == '.' || s.charAt(iSize - 1) == '/' || s.charAt(iSize - 1) == '"' || s.charAt(iSize - 1) == ']' || s.charAt(iSize - 1) == '['))
            {
            s = s.substring(0, iSize - 1);
            iSize--;
            }
        int idx;
        if ((idx = s.indexOf('-')) != -1)
            {
            if (idx > s.length() - idx)
                {//take till idx
                s = s.substring(0, idx);
                }
            else
                {
                s = s.substring(idx + 1);
                }
            }
        while (s.length() > 0 && (s.charAt(0) == ',' || s.charAt(0) == '.' || s.charAt(0) == '/' || s.charAt(0) == '"' || s.charAt(0) == '[' || s.charAt(0) == ']' || s.charAt(0) == '-'))
            {
            s = s.substring(1);

            }
        iSize = s.length();
        while (iSize > 0 && (s.charAt(iSize - 1) == ',' || s.charAt(iSize - 1) == '.' || s.charAt(iSize - 1) == '/' || s.charAt(iSize - 1) == '"' || s.charAt(iSize - 1) == ']' || s.charAt(iSize - 1) == '['))
            {
            s = s.substring(0, iSize - 1);
            iSize--;
            }
        if (s.indexOf('-') != -1)
            {
            s = "";
            }
        return s;
        }

    private String fnCleanStart(String s)
        {
        while (s.length() > 0 && (s.charAt(0) == ',' || s.charAt(0) == '.' || s.charAt(0) == '/' || s.charAt(0) == '"' || s.charAt(0) == '[' || s.charAt(0) == ']' || s.charAt(0) == '-'))
            {
            s = s.substring(1);

            }
        return s;
        }

    /**
     * used to check is the srting is a valid month
     *
     * @param sWord
     *
     * @return String translated input into number or empty string if not valid
     */
    private String fnIsMonth(String sWord)
        { // switch checkes for month num
        switch (sWord.toLowerCase())
            {
            case "january":
                return "01";
            case "february":
                return "02";
            case "march":
                return "03";
            case "april":
                return "04";
            case "may":
                return "05";
            case "june":
                return "06";
            case "july":
                return "07";
            case "august":
                return "08";
            case "september":
                return "09";
            case "october":
                return "10";
            case "november":
                return "11";
            case "december":
                return "12";
            }
        return "";
        }

    /**
     * takes a string and parse it into integer,
     * if itn not a valid number it returns 0
     *
     * @param s
     *
     * @return int
     */
    private int S2Int(String s)
        {

        try
            {
            return Integer.parseInt(s);
            }
        catch (Exception e)
            {
            return 0;
            }
        }

    /**
     * parse a string that represent a number. if the number has more then 2 digits after the floating point, we cut the
     * extra digits
     *
     * @param sWord the string we want to parse
     *
     * @return the parse string
     */
    private String fnParseNumber(String sWord) // ******for now no 2/3 ****
        {
        String sResult = sWord;
        int r, l;
        String le;
        sResult = fnRemoveAllCharOfKind(sResult, ',');
        while (sResult.length() > 2 && sResult.charAt(0) == '0' && sResult.charAt(1) != '.')
            {
            sResult = sResult.substring(1);
            }

        int iPoint = sResult.indexOf('.');

        if (sResult.length() - iPoint >= 3 && iPoint != -1)
            {
            sResult = sResult.substring(0, iPoint + 3);
            }
        iPoint = sResult.indexOf('.');
        le = sResult.substring(iPoint + 1);
        if (iPoint != -1)
            {
            l = S2Int(sResult.substring(0, iPoint));
            if (l > 0 || (iPoint == 1 && sResult.charAt(0) == '0'))
                {//l is good
                if (le.length() == 0)
                    {
                    return sResult.substring(0, iPoint - 1);
                    }
                if (le.length() == 1 && (le.charAt(0) <= '9' && '0' <= le.charAt(0)))
                    {
                    return sResult + "0";
                    }
                if (le.length() == 2 && (le.charAt(0) <= '9' && '0' <= le.charAt(0)) && (le.charAt(1) <= '9' && '0' <= le.charAt(1)))
                    {
                    return sResult;
                    }
                return "";
                }
            else
                {
                return "";
                }
            }
        if (0 < S2Int(sResult))
            {
            return sResult;
            }

        return "";

        }

    /**
     * This function will remove all appearance of specific char from string
     *
     * @param s the string we want to delete from
     * @param c the char we want to delete
     *
     * @return the string without the char
     */
    private String fnRemoveAllCharOfKind(String s, char c)
        {
        String sResult = s;
        int iIndex;
        while (-1 != (iIndex = sResult.indexOf(c)))
            {
            String sTemp1 = sResult.substring(0, iIndex);
            String sTemp2 = sResult.substring(iIndex + 1);
            sResult = sTemp1 + sTemp2;
            }
        return sResult;
        }

    /**
     * Check if a certain word is a stop word or not
     *
     * @param sWord the word to check
     *
     * @return true if it in the stop word file, false if not
     */
    public boolean fnIsStopWords(String sWord)
        {
        return this.hashSetStopWords.contains(sWord);
        }

    /**
     * Parse a text to sentences
     *
     * @param sbText the text to parse
     *
     * @return list of sentences
     */
    public ArrayList<String> fnGetSentences(StringBuilder sbText)
        {


        Document doc = Jsoup.parse(String.valueOf(sbText));

        ArrayList<String> result = new ArrayList<>();

        String[] sLines = String.valueOf(doc.text()).split("[ :;!?,\n\r\t\f\\[\\]]+");
        StringBuilder sb = new StringBuilder();

        for (int i = 0, sLinesLength = sLines.length; i < sLinesLength; i++)
            {
            String sWord = sLines[i];
            sWord = sWord.toLowerCase();
            if (fnIsShortcut(sWord))
                {
                sb.append(sWord).append(" ");
                continue;
                }
            int iNum = fnCountNumOfChar('.', sWord);
            if (0 == iNum)
                {
                sb.append(sWord).append(" ");
                continue;
                }
            if (iNum > 1)
                {
                sb.append(sWord).append(" ");
                continue;
                }
            sb.append(sWord);
            result.add(String.valueOf(sb));
            sb = new StringBuilder();

            }

        return result;

        }

    /**
     * Count how many times a specific char in in a string
     *
     * @param c     the char to count
     * @param sWord the string to check
     *
     * @return number of times a char in this string. 0 if the char does not show in the string
     */
    private int fnCountNumOfChar(char c, String sWord)
        {
        int iResult = 0;
        for (int i = 0, iSize = sWord.length(); i < iSize; i++)
            {
            if (c == sWord.charAt(i))
                {
                iResult++;
                }
            }
        return iResult;
        }

    /**
     * Check if a word is in a list. The list is words in the corpus who have 1 dot in the string but do not represent
     * an end of sentences.
     *
     * @param s the word to check
     *
     * @return true if the word is in the list, false if not
     */
    private boolean fnIsShortcut(String s)
        {
        switch (s.toLowerCase())
            {
            case "dr.":
                return true;
            case "mr.":
                return true;
            case "ms.":
                return true;
            case "lt.":
                return true;
            case "col.":
                return true;
            case "minister.":
                return true;
            case "jan.":
                return true;
            case "feb.":
                return true;

            }
        return false;
        }


    }

