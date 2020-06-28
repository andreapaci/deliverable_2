package logic.deliverable1;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry.ChangeType;

import entity.Commit;
import entity.CommitFileOperation;
import entity.Version;
import entity.VersionFile;
import util.FileLogger;

public class MetricHandler {

	public void calculateMetrics(Commit[] commits, Version[] versions) {
		for (Version version : versions) {
			calculateVersionMetric(commits, version);
		}
	}

	private void calculateVersionMetric(Commit[] commits, Version version) {
		
		
		for (VersionFile file : version.getFilesPath()) {
			
			List<String> filesPath = new ArrayList<>();
			int locTouched = 0;
			int locAdded = 0;
			int maxLocAdded = 0;
			int chgSetSize = 0;
			int maxChgSetSize = 0; 
			int churn = 0;
			int maxChurn = 0;
			int numberRevision = 0;
			long age = (new Date()).getTime();
			List<String> authors = new ArrayList<>();

			// Innanzitutto ci troviamo tutti i rename, così sappiamo a quali file ci
			// dobbiamo riferire

			filesPath.add(file.getFilePath());

			findRenames(commits, version, filesPath);
			
	
			for (Commit commit : commits) {
				
				if (commit.getVersion().getIndex() <= version.getIndex()) {

					age = caluclateAge(commit, filesPath, age);

					int[] metrics = {locTouched, locAdded, maxLocAdded, chgSetSize, maxChgSetSize, churn, maxChurn, numberRevision};
					
					metrics = calculateMetrics(commit, version, filesPath, metrics, authors);
					locTouched = metrics[0];
					locAdded = metrics[1];
					maxLocAdded = metrics[2];
					chgSetSize = metrics[3];
					maxChgSetSize = metrics[4];
					churn = metrics[5];
					maxChurn = metrics[6];
					numberRevision = metrics[7];
					
				}

			}
			
			file.setLocTouched(locTouched);
			file.setRevisionNumber(numberRevision);
			file.setNumberAuthor(authors.size());
			file.setLocAdded(locAdded);
			file.setLocMax(maxLocAdded);
			if(numberRevision == 0) file.setLocAvg(0);
			else file.setLocAvg((float) locAdded/numberRevision);
			int weeksAge = (int) (((Date.from(version.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant())).getTime() - age)/604800000L);
			file.setAge(weeksAge);
			file.setChgSize(chgSetSize);
			file.setMaxChg(maxChgSetSize);
			file.setChurn(churn);
			file.setMaxChurn(maxChurn);
			if(numberRevision == 0) file.setAvgChurn(0);
			else file.setAvgChurn((float)churn/numberRevision);
			if(numberRevision == 0) file.setAvgChg(0);
			else file.setAvgChg((float)chgSetSize/numberRevision);
			
			
			
			
		}

	}
	
	
	//Calcola le metriche
	private int[] calculateMetrics(Commit commit, Version version, List<String> filesPath, int[] metrics, List<String> authors) {
		
		
		if (commit.getVersion() == version) {
			for (CommitFileOperation fileOp : commit.getFileTouched())
				if (filesPath.contains(fileOp.getFilePath())) 
					calculateFileOpMetrics(fileOp, version, metrics, authors);
		}
		
		return metrics;
	}
	
	private void calculateFileOpMetrics(CommitFileOperation fileOp, Version version, int[] metrics, List<String> authors) {
		
		
		if (fileOp.getOpType() == ChangeType.DELETE)
			FileLogger.getLogger().warning("Versione[" + version.getIndex() + "]File["
					+ fileOp.getOldPath() + "] Il file è stato eliminato");
		else {

			metrics[0] += fileOp.getLocTouched();   	//locTouched
			metrics[1] += fileOp.getLocAdded();			//locAdded
			if(metrics[2] < fileOp.getLocAdded())		//maxLocAdded
				metrics[2] = fileOp.getLocAdded();		//
			metrics[3] += fileOp.getChgSetSize();		//chgSetSize
			if(metrics[4] < fileOp.getChgSetSize())		//maxChgSetSize
				metrics[4] = fileOp.getChgSetSize();	//
			metrics[5] += fileOp.getChurn();			//churn
			if(metrics[6] < fileOp.getChurn())			//maxChurn
				metrics[6] = fileOp.getChurn();			//
			if(!authors.contains(fileOp.getAuthor()))	//authors
				authors.add(fileOp.getAuthor());		//
			metrics[7]++;								//numberRevision

		}

	}
	
	
	//Calcola l'age
	private long caluclateAge(Commit commit, List<String> filesPath, long age) {
		
		// Con questo calcolo l'age del file
		
		for (CommitFileOperation filePath : commit.getFileTouched())
			if (filesPath.contains(filePath.getFilePath()) && (filePath.getOpType() == ChangeType.ADD
					|| filePath.getOpType() == ChangeType.COPY) && age > commit.getDate().getTime()) {
					age = commit.getDate().getTime();
			}
		
		return age;
	}
	
	//Trova tutti i renames
	private void findRenames(Commit[] commits, Version version, List<String> filesPath) {
		
		for (int i = commits.length - 1; i >= 0; i--)  {
			if (commits[i].getVersion().getIndex() <= version.getIndex()) {
				for (CommitFileOperation fileTouched : commits[i].getFileTouched())
					if (fileTouched.getOpType() == ChangeType.RENAME) {
							
						addRename(filesPath, fileTouched);
					}
			}
		}
		
		
	}
	
	
	
	//Aggiunge il rename alla lista
	private void addRename(List<String> filesPath, CommitFileOperation fileTouched){
		
		if (filesPath.contains(fileTouched.getFilePath())
				&& !filesPath.contains(fileTouched.getOldPath()))
			filesPath.add(fileTouched.getOldPath());
		if (!filesPath.contains(fileTouched.getFilePath())
				&& filesPath.contains(fileTouched.getOldPath()))
			filesPath.add(fileTouched.getFilePath());
	}

}
