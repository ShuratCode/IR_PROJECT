package Parse;

/**
 * @author Shaked
 * @since 29-Nov-17
 */
public class Term implements Comparable<Term>
{
    private String sName, sDocName;
    private int iNumOfTimes;
    private int iType;
    private int iPlace;

    public Term(String sName, String sDocName, int iNumOfTimes, int iPlace, int iType)
    {
        this.sName = sName;
        this.sDocName = sDocName;
        this.iNumOfTimes = iNumOfTimes;
        this.iPlace = iPlace;
        this.iType = iType;
    }

    /**
     * @return the name of the term, can be null
     */
    public String getsName()
    {
        return this.sName;
    }

    /**
     * @param sName set a new name to tne term
     */
    public void setsName(String sName)
    {
        this.sName = sName;
    }

    /**
     * raise the number of times shown in document by 1
     */
    public void raisC()
    {
        iNumOfTimes++;
    }

    /**
     * @return the document id, can be null
     */
    public String getsDocName()
    {
        return this.sDocName;
    }

    /**
     * @param sDocName set a new document name.
     */
    public void setsDocName(String sDocName)
    {
        this.sDocName = sDocName;
    }

    /**
     * @return number of time the term shown in the document
     */
    public int getiNumOfTimes()
    {
        return this.iNumOfTimes;
    }

    /**
     * @param iNumOfTimes new number of times shown in the document
     */
    public void setiNumOfTimes(int iNumOfTimes)
    {
        this.iNumOfTimes = iNumOfTimes;
    }

    /**
     * @return first place this term is shown in the document
     */
    public int getiPlace()
    {
        return iPlace;
    }

    /**
     * @param iPlace first place this term had shown in document
     */
    public void setiPlace(int iPlace)
    {
        this.iPlace = iPlace;
    }

    /**
     * @return the type of the document
     */
    public int getType()
    {
        return iType;
    }

    /**
     * @see Object
     */
    @Override
    public String toString()
    {
        return "!#" + sDocName + "!#" + iNumOfTimes;
    }

    /**
     * Compare this term name to another term name
     *
     * @param o term to check
     * @return 0 if terms name is lexicographic equal, less then 0 if this is lexicographic smaller then o,
     * greater then 0 if this is lexicographic larger then o.
     */
    @Override
    public int compareTo(Term o)
    {
        String sOtherName = o.getsName();
        return this.sName.compareTo(sOtherName);
    }
}
