package searchinfolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

public class searchfiles {
	private String fileNameToSearch;
	  private List<String> result = new ArrayList<String>();
		
	  public String getFileNameToSearch() {
		return fileNameToSearch;
	  }

	  public void setFileNameToSearch(String fileNameToSearch) {
		this.fileNameToSearch = fileNameToSearch;
	  }

	  public List<String> getResult() {
		return result;
	  }

	  public static List<String> searchSourceFiles(String dir) {

		  searchfiles fileSearch = new searchfiles();
	  
	        //try different directory and filename :)
		fileSearch.searchDirectory(new File(dir), "java");
		
		return fileSearch.getResult();
//		int count = fileSearch.getResult().size();
//		if(count ==0){
//		    System.out.println("\nNo result found!");
//		}else{
//		    System.out.println("\nFound " + count + " result!\n");
//		    for (String matched : fileSearch.getResult()){
//			System.out.println("Found : " + matched);
//		    }
//		}
	  }

	  public void searchDirectory(File directory, String fileNameToSearch) {

		setFileNameToSearch(fileNameToSearch);

		if (directory.isDirectory()) {
		    search(directory);
		} else {
		    System.out.println(directory.getAbsoluteFile() + " is not a directory!");
		}

	  }

	  private void search(File file) {

		if (file.isDirectory()) {
		  //System.out.println("Searching directory ... " + file.getAbsoluteFile());
			
	            //do you have permission to read this directory?	
		    if (file.canRead()) {
			for (File temp : file.listFiles()) {
			    if (temp.isDirectory()) {
				search(temp);
			    } else {
			    	//System.out.println(FilenameUtils.getExtension(temp.getName()));
				if (getFileNameToSearch().equals(FilenameUtils.getExtension(temp.getName()))) {			
				    result.add(temp.getAbsoluteFile().toString());
				    //System.out.println(temp.getAbsoluteFile().toString());
			    }

			}
		    }

		 } else {
			System.out.println(file.getAbsoluteFile() + "Permission Denied");
		 }
	      }

	  }
}
