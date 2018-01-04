package Ranker;

import Tuple.MutablePair;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;

public class Ranker
{
    private HashMap<String, MutablePair<Double, String>> hashMapDocsGrades;
    private RandomAccessFile                             randomAccessFile;


    public Ranker(HashMap<String, MutablePair<Double, String>> hashMapDocsGrades)
    {
        this.hashMapDocsGrades = hashMapDocsGrades;
    }

    //TODO: get query words and tfIdf of words in document grade and calculate

    public double fnCosin(ArrayList<Double> wordsGrade, double dDocGrade)
    {
        double dUp       = wordsGrade.stream().mapToDouble(d -> d).sum();
        double dQueryGrade = Math.sqrt(wordsGrade.size());
        double dDown       = dDocGrade * dQueryGrade;
        return dUp / dDown;
    }

    public void fnRandomAccessFileInitialize(String sReadPosting)
    {
        File posting = new File(sReadPosting);
        try
        {
            this.randomAccessFile = new RandomAccessFile(posting, "r");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }
}
