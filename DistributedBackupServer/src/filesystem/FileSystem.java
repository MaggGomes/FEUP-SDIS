package filesystem;

import java.util.HashMap;

public class FileSystem {
	
	private HashMap<String, File> files;
	
	public FileSystem(){
		this.files = new HashMap<>();
		
	}

	public HashMap<String, File> getFiles() {
		return files;
	}

	public void setFiles(HashMap<String, File> files) {
		this.files = files;
	}

}
