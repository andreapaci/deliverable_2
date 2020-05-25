package logic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import entity.Commit;
import entity.CommitFileOperation;
import entity.Version;
import entity.VersionFile;

public class VersionHandler 
{
	private Git git;
	
	public void handleVersion(Git git, Commit[] commits, Version[] versions)
	{
		this.git = git;
		FileLogger.getLogger().info("Aggiunta dei rispettivi file alle versioni ...\n");
		//Aggiungo le i file alle rispettive versioni
		
		
		addVersionFiles(commits, versions);
		
		
		FileLogger.getLogger().info("File aggiunti. Stampa dei valori ottenuti:\n");
		
		
		
		
		for(Version version : versions)
			try {
				
				getAllCommitFiles(version, commits);
			} catch (IOException e1) {
				FileLogger.getLogger().error("Errore nel recupero dei file di una versione"); System.exit(1);
			}

		
		FileLogger.getLogger().info("\n\nFine recupero file versione\n");
		
		
		
	}
	
	private void addVersionFiles(Commit[] commits, Version[] versions)
	{
		List<String> filesPath = new ArrayList<>();
		
		for(int i = 0; i < versions.length; i++){
			
			for(String file : filesPath)
				versions[i].addFilePath(file);
			
			for(Commit commit : commits)
				if(commit.getVersion() == versions[i])
					for(CommitFileOperation fileOp : commit.getFileTouched())
						switch(fileOp.getOpType())
						{
				
							case ADD:
								versions[i].addFilePath(fileOp.getFilePath());
								filesPath.add(fileOp.getFilePath());
								break;
							case COPY:
								versions[i].addFilePath(fileOp.getFilePath());
								filesPath.add(fileOp.getFilePath());
								break;
							case DELETE:
								versions[i].removeFilePath(fileOp.getOldPath());
								if(!filesPath.remove(fileOp.getOldPath())) FileLogger.getLogger().info("\tVersione[" + versions[i].getIndex() + "]Errore nel delete: " + fileOp.getOldPath());
								break;
							case RENAME:
								versions[i].removeFilePath(fileOp.getOldPath());
								versions[i].addFilePath(fileOp.getFilePath());
								if(!filesPath.remove(fileOp.getOldPath())) FileLogger.getLogger().info("\tVersione[" + versions[i].getIndex() + "]Errore nel rename: " + fileOp.getOldPath());
								filesPath.add(fileOp.getFilePath());
								break;
							case MODIFY:
								if(!versions[i].contains(fileOp.getFilePath())){
									versions[i].addFilePath(fileOp.getFilePath());
									filesPath.add(fileOp.getFilePath());
								}
								break;
							default:
								break;
						}
			
		}
		
	}
	

	
	public void getAllCommitFiles(Version version, Commit[] commits) throws IOException
	{
		//Trovo l'ultima commit di una versione
		
		Commit lastVersionCommit = null;
		
		for(Commit commit : commits)
		{
			if((commit.getDate().toInstant().atZone(ZoneId.of("Z")).toLocalDate()).isBefore(version.getDate())) {
				
				if(lastVersionCommit == null)
					lastVersionCommit = commit;
			
				else {
					if(commit.getDate().getTime() > lastVersionCommit.getDate().getTime())
						lastVersionCommit = commit;
				}
			}
			
		}
		
		FileLogger.getLogger().info("L'ultima commit della versione [" + version.getIndex() + "] è: \n\t" + lastVersionCommit.printCommitValues(false).replace("\n", "\n\t"));
		
		
		
		
		//Parse dell'ultima commit sul log
		Iterable<RevCommit> log = null;	//Log delle commit
		RevCommit rev = null;			//Commit
		
		
		try { log = git.log().call(); } 
		catch (GitAPIException e) { FileLogger.getLogger().error("Errore nel recupero del log: " + e.getMessage()); System.exit(1);}
		
		Iterator<RevCommit> iterator = log.iterator();
		while(iterator.hasNext())
		{
			rev = iterator.next();
			if(rev.getShortMessage().equals(lastVersionCommit.getMessage()) 
					&& rev.getCommitTime()*1000L == lastVersionCommit.getDate().getTime())  break;
				
		}
		
		
		ObjectId treeId = rev.getTree();
		List<String> filesPath = new ArrayList<>();
		
		try (TreeWalk treeWalk = new TreeWalk(git.getRepository())) {
			treeWalk.reset(treeId);
			treeWalk.setRecursive(true);

			while (treeWalk.next()) {

			    int loc = locFile(treeWalk);
			    filesPath.add(treeWalk.getPathString());
			    version.addFileLoc(treeWalk.getPathString(), loc);
			}
			

		} catch (IOException e) {

			FileLogger.getLogger().error("Errore nel parse dei file della commit"); System.exit(1);
		}

		
		if(filesPath.size() != version.getFilesPath().size()) {
			FileLogger.getLogger().warning("\tIl numero di file mon combacia. (" + filesPath.size() + ", " + version.getFilesPath().size() + ")\n");
		}
		else {
			FileLogger.getLogger().info("\tIl numero di file combacia. (" + filesPath.size() + ", " + version.getFilesPath().size() + ")\n");
			
			List<String> compareFiles = new ArrayList<>();
			for(String files : filesPath)
				if(!compareFiles.contains(files)) compareFiles.add(files);
			
			FileLogger.getLogger().info("\tIl numero di file univoci nell'ultima commit sono: (" + compareFiles.size()+ ")\n");
			
			for(VersionFile file : version.getFilesPath())
				filesPath.remove(file.getFilePath());
			
			if(filesPath.size() != 0)
				FileLogger.getLogger().warning("\tI file presenti nella verisione e quelli presenti nella commit non combaciano (" + filesPath.size() +  ")\n");
			else FileLogger.getLogger().warning("\tI file presenti nella verisione e quelli presenti nella commit combaciano\n");
			
			
			
			
		}
		
	}
	
	private int locFile(TreeWalk treeWalk) throws IOException
	{
		ObjectLoader loader = git.getRepository().open(treeWalk.getObjectId(0));
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
		loader.copyTo(output);
		
		String filecontent = output.toString();
	    StringTokenizer token = new StringTokenizer(filecontent, "\n");
	    
	    int count = 0;
	    while(token.hasMoreTokens()) {count++; token.nextToken();}
	      
	     
	    return count;
	
	}

}
