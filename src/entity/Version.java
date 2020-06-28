package entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import util.FileLogger;

//Classe che memorizza informazioni sulle versioni
public class Version 
{
	private int index;							//Indice della versione (associa una numerazione da "1" a "n" secondo un ordine cronologico)
	private String id;							//ID della versione
	private String versionName;					//Nome della versione
	private LocalDate date;						//Data di rilascio della versione
	private List<VersionFile> filesPath;		//Array contenente ogni file di una relativa versione
	
	
	
	public Version(int index, String id, String versionName, LocalDate date) {
		this.index = index;
		this.id = id;
		this.versionName = versionName;
		this.date = date;
		this.filesPath = new ArrayList<>();
	}
	
	public String printVersion(){
		String print = "";
		StringBuilder sb = new StringBuilder(print);
		sb.append("Versione #" + this.index + "[" + this.versionName + "]:\n");
		for(VersionFile filePath : filesPath)
			sb.append(" |\t" + filePath.getFilePath() + "\n");
		
		sb.append(" +--------------------------------------------------------------------------------------");
		return sb.toString();
	}
	
	
	public void addFilePath(String filePath) {
		 this.filesPath.add(new VersionFile(filePath));
	}
	
	public void removeFilePath(String filePath) {
		for(VersionFile file : filesPath)
			if(file.getFilePath().equals(filePath)) {
				if(!this.filesPath.remove(file)) FileLogger.getLogger().info("\tVersione[" + index + "]:Errore nel remove filepath: " + filePath);
				break;
			}
	}
	
	public void addFileLoc(String filePath, int loc) {
		if(!this.contains(filePath)) {
			FileLogger.getLogger().error("Errore nel aggiunta della dimensione del file per " + filePath);
			System.exit(1);
		}
		for(VersionFile file : filesPath)
			if(file.getFilePath().equals(filePath)) {
				file.setSize(loc);
				break;
			}
		
		
	}
				
	public List<VersionFile> getFilesPath() {
		return filesPath;
	}
	
	public boolean contains(String filePath){
		for(VersionFile file : filesPath)
			if(file.getFilePath().equals(filePath)) return true;
		
		return false;
		
	}

	public int getIndex() {
		return index;
	}
	public String getId() {
		return id;
	}
	public String getVersionName() {
		return versionName;
	}
	public LocalDate getDate() {
		return date;
	}
	
	
	

}
