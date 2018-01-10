package Documnet;

import java.io.Serializable;

/**
 * @author Shaked
 * @since 26-Nov-17
 */
public class Document implements Serializable
{

    /***********************************************************************************************
     *                                      Fields                                                 *
     ***********************************************************************************************/
    private float  fGrade;
    private String sName, sFileName;
    private StringBuilder sbText;

    /***********************************************************************************************
     *                                      Constructors                                           *
     ***********************************************************************************************/
    /**
     * We build a new document from a full text. we will separate the doc name and the relevant text for later use
     *
     * @param sbDoc the full text that represent the hole document
     */
    public Document(StringBuilder sbDoc)
    {
        int iIndexOfStartName = sbDoc.indexOf("<DOCNO>");
        int iIndexOfEndName   = sbDoc.indexOf("</DOCNO>");
        this.sName = sbDoc.substring(iIndexOfStartName + 7, iIndexOfEndName);


        int iIndexOfStartText = sbDoc.indexOf("<TEXT>");
        int iIndexOfEndText;
        if (-1 == iIndexOfStartText)
        {
            iIndexOfStartText = sbDoc.indexOf("<DATELINE>");
            if (-1 == iIndexOfStartText)
            {
                iIndexOfStartText = sbDoc.indexOf("<P>");
                if (-1 != iIndexOfStartText)
                {
                    iIndexOfEndText = sbDoc.lastIndexOf("</P>");
                    this.sbText = new StringBuilder(sbDoc.substring(iIndexOfStartText + 10, iIndexOfEndText));

                }
            }
            else
            {
                iIndexOfEndText = sbDoc.indexOf("</DATELINE>");
                this.sbText = new StringBuilder(sbDoc.substring(iIndexOfStartText + 6, iIndexOfEndText));

            }
        }
        else
        {
            iIndexOfEndText = sbDoc.indexOf("</TEXT>");
            this.sbText = new StringBuilder(sbDoc.substring(iIndexOfStartText + 6, iIndexOfEndText));

        }


    }

    /***********************************************************************************************
     *                                      Getters                                                *
     ***********************************************************************************************/

    /**
     * @return the document name
     */
    public String getName()
    {
        return sName;
    }

    /**
     * @return the text of the document
     */
    public StringBuilder getText()
    {
        return sbText;
    }

    /**
     * get the grade of this document
     *
     * @return the grade
     */
    public float getfGrade()
    {
        return fGrade;
    }

    /**
     * Get the File this document is at in the corpus
     *
     * @return the file name
     */
    public String getsFileName()
    {
        return sFileName;
    }

    /***********************************************************************************************
     *                                      Setters                                                *
     ***********************************************************************************************/

    /**
     * Set the text of the document
     *
     * @param sbText the text to save
     */
    public void setText(StringBuilder sbText)
    {
        this.sbText = sbText;
    }

    /**
     * Set new name for the document
     *
     * @param sName the new name
     */
    public void setsName(String sName)
    {
        this.sName = sName;
    }

    /**
     * Set new grade for this document
     *
     * @param fGrade the new grade
     */
    public void setfGrade(float fGrade)
    {
        this.fGrade = fGrade;
    }

    /**
     * Set new File name to the document. The Name should be the file in the corpus that this document is at.
     *
     * @param sFileName new file name
     */
    public void setsFileName(String sFileName)
    {
        this.sFileName = sFileName;
    }
}
