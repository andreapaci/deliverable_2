package logic;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry.ChangeType;

import entity.Commit;
import entity.CommitFileOperation;
import entity.Version;
import entity.VersionFile;

public class MetricHandler {

	public void calculateMetrics(Commit[] commits, Version[] versions) {
		for (Version version : versions) {
			calculateVersionMetric(commits, version);
		}
	}

	private void calculateVersionMetric(Commit[] commits, Version version) {
		//FileLogger.getLogger().info("Version #" + version.getIndex());
		for (VersionFile file : version.getFilesPath()) {
			
			List<String> filesPath = new ArrayList<>();
			int locTouched = 0;
			int locAdded = 0;
			int maxLocAdded = 0;
			int chgSetSize = 0;
			int maxChgSetSize = 0; 
			int numberRevision = 0;
			long age = (new Date()).getTime();
			List<String> authors = new ArrayList<>();

			// Innanzitutto ci troviamo tutti i rename, così sappiamo a quali file ci
			// dobbiamo riferire

			filesPath.add(file.getFilePath());

			for (int i = commits.length - 1; i >= 0; i--)  {
				for (CommitFileOperation fileTouched : commits[i].getFileTouched())
					if (fileTouched.getOpType() == ChangeType.RENAME) {
							
						if (filesPath.contains(fileTouched.getFilePath())
								&& !filesPath.contains(fileTouched.getOldPath()))
							filesPath.add(fileTouched.getOldPath());
						if (!filesPath.contains(fileTouched.getFilePath())
								&& filesPath.contains(fileTouched.getOldPath()))
							filesPath.add(fileTouched.getFilePath());
					}
			}
			
	
				
			
			/*FileLogger.getLogger().info(" |\t File: " + file.getFilePath());
			for( String filePath : filesPath)
				FileLogger.getLogger().info(" |\t |\t"+ filePath);
*/
			// Ho tutti gli alias di tutti i file, ora calcolo le metriche
		
			for (Commit commit : commits) {
				
				//Con questo calcolo l'age del file
				for(CommitFileOperation filePath : commit.getFileTouched())
					if(filesPath.contains(filePath.getFilePath()) && (filePath.getOpType() == ChangeType.ADD || filePath.getOpType() == ChangeType.COPY)) {
						if(age > commit.getDate().getTime()) age = commit.getDate().getTime();
					}
						
						
				if (commit.getVersion() == version) {
					for (CommitFileOperation fileOp : commit.getFileTouched())
						if (filesPath.contains(fileOp.getFilePath())) {
							if (fileOp.getOpType() == ChangeType.DELETE)
								FileLogger.getLogger().warning("Versione[" + version.getIndex() + "]File["
										+ fileOp.getOldPath() + "] Il file è stato eliminato");
							else {

								locTouched += fileOp.getLocTouched();
								locAdded += fileOp.getLocAdded();
								if (maxLocAdded < fileOp.getLocAdded())
									maxLocAdded = fileOp.getLocAdded();
								chgSetSize += fileOp.getChgSetSize();
								if (maxChgSetSize < fileOp.getChgSetSize())
									maxChgSetSize = fileOp.getChgSetSize();
								if (!authors.contains(fileOp.getAuthor()))
									authors.add(fileOp.getAuthor());
								numberRevision++;

							}

						}
				}

			}
			
			file.setLocTouched(locTouched);
			file.setRevisionNumber(numberRevision);
			file.setNumberAuthor(authors.size());
			file.setLocAdded(locAdded);
			file.setLocMax(maxLocAdded);
			if(numberRevision == 0) file.setLocAvg(0);
			else file.setLocAvg(locAdded/numberRevision);
			int weeksAge = (int) (((Date.from(version.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant())).getTime() - age)/604800000L);
			file.setAge(weeksAge);
			file.setChgSize(chgSetSize);
			file.setMaxChg(maxChgSetSize);
			if(numberRevision == 0) file.setAvgChg(0);
			else file.setAvgChg(chgSetSize/numberRevision);
			
			
			
			
		}

	}

}
