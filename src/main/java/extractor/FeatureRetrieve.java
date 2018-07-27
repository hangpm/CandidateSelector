package extractor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;



public class FeatureRetrieve {
	
	public FeatureRetrieve() {};
	
	public Document buildDoc(int SubID, String DocPath) throws FileNotFoundException{
        // creates an input stream for the file to be parsed
        FileInputStream in = new FileInputStream(DocPath);

        Document doc = new Document();

        // parse it
        //System.out.println(DocPath);
        CompilationUnit cu;
        try {
        	cu = JavaParser.parse(in);
		} catch (Exception e) {
			return doc;
		}
        
        
        final String FIELD_DOCNAME = "name";  // doc name
        final String FIELD_SC = "code";  // raw source code    
        final String FIELD_CALLS = "calls";  // function calls 
        final String FIELD_STRING_LITS = "strings";  // function calls 
        final String FIELD_ARRAYS = "arrays";  // arrays
        final String FIELD_FN_DEFS = "fdefs";    // method names
        final String FIELD_CLASS_DEFS = "cdefs";    // class names
        final String FIELD_STMTS = "stmts";    // statements
        final String FIELD_COMMENTS = "comments"; // comments
        final String FIELD_PACKAGE_IMPORTS = "imports"; // imports
        final String FIELD_ALL = "content"; // merge all into one field with default analyzer

        // visit and print the methods names
        //cu.accept(new MethodVisitor(), null);
        //1
        MethodDeclarationVisitor mdv = new MethodDeclarationVisitor();
        mdv.visit(cu, null);
        //System.out.println(((MethodDeclarationVisitor)mdv).buffer.toString());
        doc.add(new StringField(FIELD_FN_DEFS, ((MethodDeclarationVisitor)mdv).buffer.toString(), Field.Store.YES));
        //2
        ClassVisitor cv = new ClassVisitor();
        cv.visit(cu, null);
        //System.out.println(((ClassVisitor)cv).buffer.toString());
        doc.add(new StringField(FIELD_CLASS_DEFS, ((ClassVisitor)cv).buffer.toString(), Field.Store.YES));
        //3
        StringLiteralVisitor sv = new StringLiteralVisitor();
        sv.visit(cu, null);
        //System.out.println(((StringLiteralVisitor)sv).buffer.toString());
        //System.out.println(((StringLiteralVisitor)sv).arrayBuff.toString());
        doc.add(new StringField(FIELD_STRING_LITS, ((StringLiteralVisitor)sv).buffer.toString(), Field.Store.YES));
        //doc.add(new StringField(FIELD_ARRAYS, ((StringLiteralVisitor)sv).getArrayContent(), Field.Store.YES));
        //4
        //MethodVisitor mv = new MethodVisitor();
        //mv.visit(cu,  null);
        //System.out.println(((MethodVisitor)mv).getCalls());
        //System.out.println(((MethodVisitor)mv).getStmts());
        //doc.add(new StringField(FIELD_STMTS, ((MethodVisitor)mv).getStmts(), Field.Store.YES));
        //doc.add(new StringField(FIELD_CALLS, ((MethodVisitor)mv).getCalls(), Field.Store.YES));
        
        doc.add(new StringField("path", DocPath.trim(), Field.Store.YES));
        doc.add(new StringField("id", String.valueOf(SubID).trim() , Field.Store.YES));
        
        return doc;
    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class MethodDeclarationVisitor extends VoidVisitorAdapter<Void> {
    	StringBuffer buffer = new StringBuffer();
        @Override
        public void visit(MethodDeclaration n, Void arg) {
        	buffer.append(n.getName()).append(" ");
//            List<Parameter> plist = n.getParameters();
//            if (plist != null) {
//                for (Parameter p : plist)
//                    buffer.append(p).append(" ");
//            }
            buffer.append("\n");
        }
    }
    
    private static class ClassVisitor extends VoidVisitorAdapter<Void> {
    	StringBuffer buffer = new StringBuffer();
        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            buffer.append(n.getName()).append("\n");
        }
    }
    private static class StringLiteralVisitor extends VoidVisitorAdapter<Void> {
        StringBuffer arrayBuff = new StringBuffer();
        StringBuffer buffer = new StringBuffer();
        
        @Override
        public void visit(StringLiteralExpr n, Void arg) {
            buffer.append(n.getValue()).append("\n");
        }

        public void visit(ArrayInitializerExpr n, Void arg) {
            if (n.getValues() != null) {
                for (Iterator<Expression> i = n.getValues().iterator(); i.hasNext();) {
                    arrayBuff.append(i.next());
                }
            }
            arrayBuff.append("\n");
        }

        public void visit(ArrayAccessExpr n, Void arg) {
            arrayBuff.append(n.getName()).append(" ").append(n.getIndex()).append("\n");
        }

        public void visit(ArrayCreationExpr n, Void arg) {
            arrayBuff.append(n.getElementType()).append(" ");
            List<ArrayCreationLevel> dims = n.getLevels();
            if (dims != null) {
                arrayBuff.append(dims.size());	
            }
            arrayBuff.append("\n");
        }
        
        String getArrayContent() { return arrayBuff.toString(); }
    }
    
    private static class MethodVisitor extends VoidVisitorAdapter<Void> {

        StringBuffer stmts;
        StringBuffer calls;

        public MethodVisitor() {
            stmts = new StringBuffer();
            calls = new StringBuffer();
        }
        
        @Override
        public void visit(BinaryExpr n, Void arg) {
            stmts.append(n.getLeft()).append(" ").
                    append(n.getOperator()).append(" ").
                    append(n.getRight());
            stmts.append("\n");
        }

        @Override
        public void visit(MethodCallExpr n, Void arg) {
            Optional<Expression> scopeExp = n.getScope();
            if (scopeExp != null)
                calls.append(scopeExp.toString()).append(" ");
            
            calls.append(n.getName()).append(" ");
            List<Expression> plist = n.getArguments();
            if (plist != null) {
                for (Expression p : plist)
                    calls.append(p).append(" ");
            }
            calls.append("\n");
        }
        
        //String getCalls() { return calls.toString(); }
        //String getStmts() { return stmts.toString(); }
    }
    
    List<TermScore> selterm(Document doc, String fieldname) throws IOException {
    	IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("index")));
    	
    	int num_q_terms = 10;
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
