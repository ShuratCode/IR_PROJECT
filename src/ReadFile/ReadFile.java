package ReadFile;

import Documnet.Document;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * This class will read all the data in the database.
 *
 * @author Shaked
 * @since 14-Nov-17
 */
public class ReadFile
{
    /**
     * read a file and separate it to different documents.
     *
     * @param fileToRead a file object to read for the db
     * @return array list of documents in the file
     */
    public ArrayList<Document> fnReadFile(File fileToRead)
    {
        String                   sFileName     = fileToRead.getName();
        ArrayList<Document>      listFiles     = new ArrayList<>();
        ArrayList<String>        ArrayListFile = fnGetData(fileToRead);
        ArrayList<StringBuilder> arrayListDocs = fnSeparateToDocs(ArrayListFile);
        for (int iIndex = 0, iArrayListDocsSize = arrayListDocs.size(); iIndex < iArrayListDocsSize; iIndex++)
        {
            StringBuilder sbDoc    = arrayListDocs.get(iIndex);
            Document      document = new Document(sbDoc);
            document.setsFileName(sFileName);
            listFiles.add(document);
        }

        return listFiles;
    }

    /**
     * Read the file of the stop words.
     *
     * @param sStopWords the path of the file
     * @return HashSet with all the stop words
     */
    public HashSet<String> fnReadStopWords(String sStopWords)
    {
        BufferedReader bfReader = null;
        try
        {
            File fileStopWords = new File(sStopWords);
            bfReader = new BufferedReader(new FileReader(fileStopWords));
            String          sLine;
            HashSet<String> stringHashSetResult = new HashSet<>();

            while (null != (sLine = bfReader.readLine()))
            {
                stringHashSetResult.add(sLine);
            }
            bfReader.close();
            return stringHashSetResult;
        }
        catch (FileNotFoundException e)
        {
            System.out.println("The file is not found");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (bfReader != null)
            {
                try
                {
                    bfReader.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    /**
     * @param fileToRead File object of the file we want to read
     * @return array list the contains the lines of the file
     */
    private ArrayList<String> fnGetData(File fileToRead)
    {
        ArrayList<String> sResult  = new ArrayList<>();
        BufferedReader    bfReader = null;
        try
        {

            bfReader = new BufferedReader(new FileReader(fileToRead));
            String sLine;

            while (null != (sLine = bfReader.readLine()))
            {
                sResult.add(sLine);
            }

            bfReader.close();
        }
        catch (FileNotFoundException e)
        {
            System.out.println("The file:" + fileToRead + "is not found");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (bfReader != null)
            {
                try
                {
                    bfReader.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }


        return sResult;
    }

    /**
     * @param stringArrayList an array list with all the text in the file
     * @return array list with each strings represent a document
     */
    private ArrayList<StringBuilder> fnSeparateToDocs(ArrayList<String> stringArrayList)
    {
        ArrayList<StringBuilder> stringBuilderArrayList = new ArrayList<>();
        StringBuilder            sbResult               = new StringBuilder("");
        int                      iLength                = stringArrayList.size();

        for (int iIndex = 0; iIndex < iLength; iIndex++)
        {
            String sLine = stringArrayList.get(iIndex);
            if (stringArrayList.get(iIndex).equals("</DOC>"))
            {
                sbResult.append(sLine);
                stringBuilderArrayList.add(sbResult);
                sbResult = new StringBuilder("");
            }
            else
            {
                sbResult.append(' ');
                sbResult.append(sLine);
            }

        }

        return stringBuilderArrayList;
    }


}
