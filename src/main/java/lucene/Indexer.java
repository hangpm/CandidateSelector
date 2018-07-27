package lucene;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Indexer {
	
	@SuppressWarnings("resource")
	public static void deleteDoc(int docID) throws IOException, ParseException {
		String indexPath = "index";
		
		String id = String.valueOf(docID).trim();
		
		File file = new File("index");
		if(file.exists()) {
			if(file.list().length == 0) {
				return;
			}
		}
		else {
			return;
		}
		
		Directory dir = FSDirectory.open(Paths.get(indexPath));
        Analyzer analyzer = new StandardAnalyzer();
        
        
        IndexReader indexReader = DirectoryReader.open(dir);
        final IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        Query query = new TermQuery(new Term("id", id));
        TopDocs results = indexSearcher.search(query,1);
        
        if(results.totalHits == 0) {
        	//System.out.println("id " + id + " not found in index");
        	indexReader.close();
        	return;
        }
        
        
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(dir, iwc);
        
        writer.deleteDocuments(new Term("id", id));
        
        writer.commit();
        
        indexReader.close();
        writer.close();
	}
	
    /** Index all text files under a directory. */
    public static void doIndex(String srcCodePath, Document Doc, boolean create, int id) {
  	     
      String indexPath = "index";

      final Path srcpath = Paths.get(srcCodePath);

      try {
        //System.out.println("Indexing to directory '" + indexPath + "'...");

        Directory dir = FSDirectory.open(Paths.get(indexPath));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

        if (create) {
          // Create a new index in the directory, removing any
          // previously indexed documents:
          iwc.setOpenMode(OpenMode.CREATE);
        } else {
          // Add new documents to an existing index:
          iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
        }

        // Optional: for better indexing performance, if you
        // are indexing many documents, increase the RAM
        // buffer.  But if you do this, increase the max heap
        // size to the JVM (eg add -Xmx512m or -Xmx1g):
        //
        iwc.setRAMBufferSizeMB(256.0);

        IndexWriter writer = new IndexWriter(dir, iwc);
        indexDocs(writer, Doc, srcpath, id);

        // NOTE: if you want to maximize search performance,
        // you can optionally call forceMerge here.  This can be
        // a terribly costly operation, so generally it's only
        // worth it when your index is relatively static (ie
        // you're done adding documents to it):
        //
        // writer.forceMerge(1);

        writer.close();

        //Date end = new Date();
        //System.out.println(end.getTime() - start.getTime() + " total milliseconds");

        
      } catch (IOException e) {
        System.out.println(" caught a " + e.getClass() +
         "\n with message: " + e.getMessage());
      }
    }
      
 // static void indexDocs(final IndexWriter writer, Path path, Path srcpath) throws IOException {
    static void indexDocs(final IndexWriter writer, Document Doc, Path srcpath, int id) throws IOException {
//	    if (Files.isDirectory(srcpath)) {
//	      Files.walkFileTree(srcpath, new SimpleFileVisitor<Path>() {
//	        @Override
//	        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//	          try {
//	            indexDoc(writer, file, attrs.lastModifiedTime().toMillis(), srcpath);
//	          } catch (IOException ignore) {
//	            // don't index files that can't be read.
//          }
//          return FileVisitResult.CONTINUE;
//        }
//      });
//    } else {
//      indexDoc(writer, contents, Files.getLastModifiedTime(path).toMillis(), srcpath);
//    }
    	indexDoc(writer, Doc, Files.getLastModifiedTime(srcpath).toMillis(), srcpath, id);
  }

  /** Indexes a single document */
//  static void indexDoc(IndexWriter writer, Path file, long lastModified, Path srcpath) throws IOException {
  static void indexDoc(IndexWriter writer, Document Doc, long lastModified, Path srcpath, int id) throws IOException {
    //try (InputStream stream = Files.newInputStream(file)) {
	//try (InputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8))) {
      // make a new, empty document
      Document doc = Doc;
      
      // Add the path of the file as a field named "path".  Use a
      // field that is indexed (i.e. searchable), but don't tokenize 
      // the field into separate words and don't index term frequency
      // or positional information:
      //Field pathField = new StringField("path", file.toString(), Field.Store.YES);
//      Field pathField = new StringField("path", srcpath.toString(), Field.Store.YES);
//      doc.add(pathField);
      
     
      //doc.add(new StringField("id", String.valueOf(id).trim() , Field.Store.YES));
      
      // Add the last modified date of the file a field named "modified".
      // Use a LongPoint that is indexed (i.e. efficiently filterable with
      // PointRangeQuery).  This indexes to milli-second resolution, which
      // is often too fine.  You could instead create a number based on
      // year/month/day/hour/minutes/seconds, down the resolution you require.
      // For example the long value 2011021714 would mean
      // February 17, 2011, 2-3 PM.
      doc.add(new LongPoint("modified", lastModified));
      
      // Add the contents of the file to a field named "contents".  Specify a Reader,
      // so that the text of the file is tokenized and indexed, but not stored.
      // Note that FileReader expects the file to be in UTF-8 encoding.
      // If that's not the case searching for special characters will fail.
      //doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));
      
      
      if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
        // New index, so we just add the document (no old document can be there):
        //System.out.println("adding " + file);
        writer.addDocument(doc);
      } else {
        // Existing index (an old copy of this document may have been indexed) so 
        // we use updateDocument instead to replace the old one matching the exact 
        // path, if present:
        //System.out.println("updating " + file);
        writer.updateDocument(new Term("path", srcpath.toString()), doc);
      }
    }
}
