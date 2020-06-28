package entity;

import org.eclipse.jgit.diff.DiffEntry.ChangeType;

//Classe che memorizza il "git diff", cioè l'insieme di operazioni fatte in una determinata commit rispetto alla commit precedente
public class CommitFileOperation {
	
	private String newPath;			//Path da aggiungere
	private String oldPath;			//Path da eliminare
	private ChangeType opType;		//Tipo operazioni
	private int locTouched;			//Metrica LOC Touched
	private int locAdded;			//Metrica LOC Added
	private int churn;				//Metrica Churn
	private String author;			//Metrica authors
	private int chgSetSize;			//Metrica ChgSetSize
	


	public CommitFileOperation(String filePath, String oldPath, ChangeType opType, int locTouched, int locAdded, String author, int[] chgChurn) {
	
		this.newPath = filePath;
		this.oldPath = oldPath;
		this.opType = opType;
		this.locTouched = locTouched;
		this.locAdded = locAdded;
		this.author = author;
		this.chgSetSize = chgChurn[0];
		this.churn = chgChurn[1];
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

	public int getChurn() {
		return churn;
	}
	
	
	
	

}
