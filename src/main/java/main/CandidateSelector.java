package main;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.queryparser.classic.ParseException;

import DatabaseConnect.ConnectDB;
import extractor.FeatureRetrieve;
import lucene.Indexer;
import lucene.Searcher;
import org.apache.lucene.document.Document;
import searchinfolder.searchfiles;

public class CandidateSelector {
	@SuppressWarnings("null")
	public static void main(String[] args) throws Exception {
		//To mark that Lucene has already be called
		Date start = new Date();
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
			    new FileOutputStream("../Original_submission/lucene_status.txt"), "UTF-8"));
		try {
		    out.write("1");
		} finally {
		    out.close();
		}
		
		boolean indexonly = false;
		for(int i = 0; i < args.length; i++) {
			if(args[i] == "-index")
				indexonly = true;
		}
		
		List<String> ListDir = ConnectDB.getDB();

		for(int idir = 0; idir < ListDir.size(); idir++) {
			//Do each directory
			String[] directory = ListDir.get(idir).split(" ");
			if(directory.length < 2) {
				continue;
			}
			int id = Integer.parseInt(directory[0].trim());
			
			List<String> listFiles = searchfiles.searchSourceFiles(directory[1]);
			List<Document> filecontents = new Vector<Document>();
			//Indexer.deleteDoc(id);
			
			for(int it = 0; it < listFiles.size(); it++) {
				Document queryDoc = new FeatureRetrieve().buildDoc(id, listFiles.get(it));
				if(queryDoc.getFields().isEmpty()) { //skip error parsing doc
	
					continue;
				}
				
				filecontents.add(queryDoc);
								
				if(!indexonly) {
					List<String> seachResults = Searcher.search(queryDoc);
					
					if(seachResults != null) {
						for(int rit = 0; rit < seachResults.size(); rit++) {
							int idB = 0;
							String findid[] = {};
							if(seachResults.get(rit).contains("/")) {
								findid = seachResults.get(rit).split("/");
							}
							else {
								findid = seachResults.get(rit).split("\\\\");
							}
							for(int f = 0; f < findid.length - 1; f++) {
								if(findid[f].compareTo("Original_submission") == 0) {
									idB = Integer.parseInt(findid[f+1]);
									//System.out.println(idB);
									break;
								}
							}
							//System.out.println(rit + " - " + idB + " - " + seachResults.get(rit));
							ConnectDB.insertDB(id, listFiles.get(it), idB, seachResults.get(rit));
						}
					}
				}
			}
			for(Document indexdoc : filecontents) {
				Indexer.doIndex(indexdoc.get("path").trim(), indexdoc, false, id);
			}
			
		}
		
		//To mark that Lucene finish running
		out = new BufferedWriter(new OutputStreamWriter(
			    new FileOutputStream("../Original_submission/lucene_status.txt"), "UTF-8"));
		try {
		    out.write("0");
		} finally {
		    out.close();
		}
		
		Date end = new Date();
        System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
	}
	
}
