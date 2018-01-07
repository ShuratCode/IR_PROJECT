package Documnet;

import java.io.Serializable;

/**
 * @author Shaked
 * @since 26-Nov-17
 */
public class Document implements Serializable
{
    private float  fGrade;
    private String sName, sFileName;
    private StringBuilder sbText;

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
     * Set the text of the document
     *
     * @param sbText the text to save
     */
    public void setText(StringBuilder sbText)
    {
        this.sbText = sbText;
    }

    public void setsName(String sName)
    {
        this.sName = sName;
    }

    public float getfGrade()
    {
        return fGrade;
    }

    public void setfGrade(float fGrade)
    {
        this.fGrade = fGrade;
    }

    public String getsFileName()
    {
        return sFileName;
    }

    public void setsFileName(String sFileName)
    {
        this.sFileName = sFileName;
    }
}
