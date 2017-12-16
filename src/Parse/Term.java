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

  public Term (String sName, String sDocName, int iNumOfTimes, int iPlace, int iType)
  {
    this.sName = sName;
    this.sDocName = sDocName;
    this.iNumOfTimes = iNumOfTimes;
    this.iPlace=iPlace;
    this.iType=iType;
  }

  public String getsName ()
  {
    return this.sName;
  }

  public void raisC(){iNumOfTimes++;}

  public void setsName (String sName)
  {
    this.sName = sName;
  }

  public String getsDocName ()
  {
    return this.sDocName;
  }

  public void setsDocName (String sDocName)
  {
    this.sDocName = sDocName;
  }

  public int getiNumOfTimes ()
  {
    return this.iNumOfTimes;
  }

  public void setiNumOfTimes (int iNumOfTimes)
  {
    this.iNumOfTimes = iNumOfTimes;
  }

  public int getiPlace() { return iPlace;  }

  public void setiPlace(int iPlace) { this.iPlace = iPlace; }

  public int getType ()
  {
    return iType;
  }

  @Override
  public String toString ()
  {
      return "!#" + sDocName + "!#" + iNumOfTimes;
  }

  @Override
  public int compareTo (Term o)
  {
    String sOtherName = o.getsName();
    return this.sName.compareTo(sOtherName);
  }
}
