package Ranker;

import Tuple.MutablePair;
import Tuple.MutableTriple;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class Ranker
{
    private HashMap<String, MutablePair<double[], String>> hashMapDocsGrades;
    private RandomAccessFile                             randomAccessFile;
    private HashMap<String, MutableTriple<Integer[], Float, Long>> dictionary;
    private HashMap<String, MutablePair<String, Long>>             cache;

    public Ranker(HashMap<String, MutablePair<double[], String>> hashMapDocsGrades, HashMap<String, MutableTriple<Integer[], Float, Long>> dictionary, HashMap<String, MutablePair<String, Long>> cache)
    {
        this.hashMapDocsGrades = hashMapDocsGrades;
        this.dictionary=dictionary;
        this.cache=cache;

    }

    public void setHashMapDocsGrades(HashMap<String, MutablePair<double[], String>> hashMapDocsGrades) {
        this.hashMapDocsGrades = hashMapDocsGrades;
    }

    public void setDictionary(HashMap<String, MutableTriple<Integer[], Float, Long>> dictionary) {
        this.dictionary = dictionary;
    }

    public void setCache(HashMap<String, MutablePair<String, Long>> cache) {
        this.cache = cache;
    }
//TODO: get query words and tfIdf of words in document grade and calculate

    public double fnCosin(ArrayList<Double> wordsGrade, double dDocGrade)
    {
        double dUp       = wordsGrade.stream().mapToDouble(d -> d).sum();
        double dQueryGrade = Math.sqrt(wordsGrade.size());
        double dDown       = dDocGrade * dQueryGrade;
        return dUp / dDown;
    }

    public ArrayList<Map.Entry<String, Double>> fnGetBestDocs(ArrayList<String> query, int iNumReturn){
        HashMap<String,Double> rankList=new HashMap<>();
        String   sLine;
        String[] strings;
        long     lPointer;
        for(int i=0;i<query.size();i++){// loop for each word in q
            String term=query.get(i);
            if(!dictionary.containsKey(term))
                continue;
            lPointer = this.dictionary.get(term).getRight();
            if(dictionary.containsKey(term)){
                float termIDF=dictionary.get(term).getMiddle();
                lPointer=cache.get(term).getRight();
                if(cache.containsKey(term)){
                    sLine=cache.get(term).getLeft();
                    strings = sLine.split("!#");

                    for (int iIndex = 1, iLength = strings.length; iIndex < iLength; iIndex++)//compute docs in cache
                    {
                        String sDocTemp = strings[iIndex];
                        double maxTFi=hashMapDocsGrades.get(sDocTemp).getLeft()[0];
                        iIndex++;
                        int Fi=(int)Integer.parseInt(strings[iIndex]);
                        if(rankList.containsKey(sDocTemp)){
                            rankList.put(sDocTemp,rankList.get(sDocTemp)+((((double)Fi)/maxTFi)*termIDF));
                        }
                        else{
                            rankList.put(sDocTemp,((((double)Fi)/maxTFi)*termIDF));
                        }
                    }
                }
                try{
                    randomAccessFile.seek(lPointer);
                    sLine = randomAccessFile.readLine();
                    strings = sLine.split("!#");
                    for (int iIndex = 1, iLength = strings.length; iIndex < iLength; iIndex++)//compute docs in posting
                    {
                        String sDocTemp = strings[iIndex];
                        double maxTFi=hashMapDocsGrades.get(sDocTemp).getLeft()[0];
                        iIndex++;
                        int Fi=(int)Integer.parseInt(strings[iIndex]);
                        if(rankList.containsKey(sDocTemp)){
                            rankList.put(sDocTemp,rankList.get(sDocTemp)+((((double)Fi)/maxTFi)*termIDF));
                        }
                        else{
                            rankList.put(sDocTemp,((((double)Fi)/maxTFi)*termIDF));
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        ArrayList<Map.Entry<String,Double>> ansRlist=new ArrayList<>(rankList.entrySet());
        for(int i=0;i<ansRlist.size();i++){//normlize the cosim for each doc
            String doc=ansRlist.get(i).getKey();
            double docG=hashMapDocsGrades.get(doc).getLeft()[2];
            double docW=ansRlist.get(i).getValue();
            ansRlist.get(i).setValue(docW/(docG*Math.sqrt((double) query.size())));
        }
        Collections.sort(ansRlist, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                if (o1.getValue() > o2.getValue()){
                    return -1;
                }
                if (o1.getValue() < o2.getValue()){
                    return 1;
                }
                else{
                    return 0;
                }
            }
        });
        if(ansRlist.size()>iNumReturn){
            ArrayList<Map.Entry<String,Double>> ansRlist2=new ArrayList<>(ansRlist.subList(0,iNumReturn));
            return ansRlist2;
        }

        return ansRlist;
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
