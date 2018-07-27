package lucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.JSONArray;

import extractor.TermScore;

public class Searcher {
	public Searcher() {};
	
	public static List<String> search(Document queryDoc) throws IOException, org.apache.lucene.queryparser.classic.ParseException {
		File file = new File("index");
		int numWanted = 3;
		
		if(file.exists()) {
			if(file.list().length == 0) {
				return null;
			}
		}
		else {
			return null;
		}
		
		
		List<String> searchResults = new ArrayList<String>();
		Directory indexDirectory = FSDirectory.open(Paths.get("index"));
		IndexReader indexReader = DirectoryReader.open(indexDirectory);
        Analyzer analyzer = new StandardAnalyzer();
        final IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        
        
        Float lambda = (float) 0.6;
        indexSearcher.setSimilarity(new LMJelinekMercerSimilarity(lambda));
        
        //System.out.println(content);
        
//        List<String> TermList = new ArrayList<String>(Arrays.asList(content.split(";")));
//        System.out.println(TermList.get(1));
//        List<String> TermClass = new ArrayList<String>(Arrays.asList(TermList.get(0).split("\r\n")));
//        List<String> TermMemvar = new ArrayList<String>(Arrays.asList(TermList.get(1).split("\r\n")));
//        List<String> TermMethod = new ArrayList<String>(Arrays.asList(TermList.get(2).split("\r\n")));
        
        Query q = null;
        Builder builder = new BooleanQuery.Builder();
        
        int termCount = 0;
		TokenStream fs = null;
        
        List<IndexableField> fields = queryDoc.getFields();
        
        for (IndexableField field : fields) {
            String fieldName = field.name();
            if (fieldName.equals("path") ||
                fieldName.equals("id"))
                continue;   // ignore non-searchable fields

            List<TermScore> topList = selterm(queryDoc, field.name());
			for (TermScore ts : topList) {
				if(ts.term.equals(" ") || ts.term.equals("\n") || ts.term.equals(""));
        		Term thisTerm = new Term(field.name(), ts.term);
	   			builder.add(new TermQuery(thisTerm), BooleanClause.Occur.SHOULD);
			}
        }
        
//        for(String classname : TermClass) {
//        	Term thisTerm = new Term("class", classname);
//   			builder.add(new TermQuery(thisTerm), BooleanClause.Occur.SHOULD);
//        }
//        
//        for(String memvar : TermMemvar) {
//        	Term thisTerm = new Term("membervar", memvar);
//   			builder.add(new TermQuery(thisTerm), BooleanClause.Occur.SHOULD);
//        }
//        
//        for(String methoddef : TermMethod) {
//        	Term thisTerm = new Term("methoddef", methoddef);
//   			builder.add(new TermQuery(thisTerm), BooleanClause.Occur.SHOULD);
//        }
        
        q = builder.build();
        
      TopScoreDocCollector collector = TopScoreDocCollector.create(numWanted);
      indexSearcher.search(q, collector);
	  TopDocs results = collector.topDocs();
	  ScoreDoc[] hits = results.scoreDocs;
	  //System.out.println(results.totalHits);
	  for(int i = 0; i < (int) hits.length; i++) {
		  Document doc = indexSearcher.doc(hits[i].doc);
		  //System.out.println(doc.get("path") + " " + hits[i].score);
		  if(i > 0 && ((hits[i-1].score - hits[i].score) < (float)0.1))
			  searchResults.add(doc.get("path"));
		  //searchResult[lineCount][i] = doc.get("path");
	  }
        indexReader.close();
  	    
  	    searchResults = (ArrayList<String>) searchResults.stream().distinct().collect(Collectors.toList());
  	    if(searchResults.size() == 0) {
  	    	return null;
  	    }
  	    	
  	    return searchResults;
        
	}
	
	static List<TermScore> selterm(Document doc, String fieldname) throws IOException {
    	IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("index")));
    	
    	int num_q_terms = 5;
		int N = reader.numDocs();
		List<TermScore> tlist = new Vector<>();
		float lambda = (float) 0.6;
		
		String[] TERMS = doc.getValues(fieldname);
		if(TERMS == null || TERMS.length == 0)
			return tlist;
        
        HashMap<String, Integer> myTermsCount = new HashMap<String, Integer>();
        for (String s : TERMS){
            if (myTermsCount.containsKey(s)) myTermsCount.replace(s, myTermsCount.get(s) + 1);
            else myTermsCount.put(s, 1);
        }
        
        // access the terms for this field
		for (String term : TERMS) {// explore the terms for this field
			Term t = new Term(fieldname, term);

			//get the term frequency in the document
			int tf = myTermsCount.get(term);
			float ntf = tf/(float)myTermsCount.get(term);
			int df = (int)(reader.totalTermFreq(t));
			float idf = N/(float)df;
			float tf_idf = lambda*ntf + (1-lambda)*idf;

			tlist.add(new TermScore(term.toString(), tf_idf));
		}

		Collections.sort(tlist); // desc
		List<TermScore> topList = tlist.subList(0, Math.min(tlist.size(), num_q_terms));
		return topList;
    }
}
