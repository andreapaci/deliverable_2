package entity;

import java.util.List;

import org.eclipse.jgit.diff.DiffEntry.ChangeType;

public class CommitFileOperation {
	
	private String newPath;			//Path da aggiungere
	private String oldPath;			//Path da eliminare
	private ChangeType opType;		//Tipo operazioni
	private int locTouched;			//Metrica LOC Touched
	private int locAdded;			//Metrica LOC Added
	private String author;			//Metrica authors
	private int chgSetSize;			//Metrica ChgSetSize
	


	public CommitFileOperation(String filePath, String oldPath, ChangeType opType, int locTouched, int locAdded, String author, int chgSetSize) {
	
		this.newPath = filePath;
		this.oldPath = oldPath;
		this.opType = opType;
		this.locTouched = locTouched;
		this.locAdded = locAdded;
		this.author = author;
		this.chgSetSize = chgSetSize;
	}
	
	public int getChgSetSize() {
		return chgSetSize;
	}

	public String getAuthor() {
		return author;
	}
	
	public int getLocTouched() {
		return locTouched;
	}
	
	public int getLocAdded() {
		return locAdded;
	}

	public String getFilePath() {
		return newPath;
	}
	
	public String getOldPath() {
		return oldPath;
	}

	public ChangeType getOpType() {
		return opType;
	}
	
	

}
