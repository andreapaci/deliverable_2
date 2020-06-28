package logic.deliverable1;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import entity.BugTicket;
import entity.Commit;
import entity.CommitFileOperation;
import entity.Version;
import util.FileLogger;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

//Classe che si occupa del recupero dei commit e della 
// raccolta di informazioni di una repository su Github
public class GithubHandler 
{
	private String clonePath = "tmp";			//Path su dove effettuare il clone della repository
	private Git git;							//Repository Git presente localmente sulla macchina
	
	
	public Commit[] retreiveFileVersion(String url, BugTicket[] tickets, Version[] versions)
	{
		
		VersionHandler versionHandler = new VersionHandler();
		Commit[] commits = null;
		
		
		//Clonazione della repository
		cloneRepository(url);
		
		FileLogger.getLogger().info("Analisi del log delle commit.\n\n");
		
		
		
		//Ottengo tutti i commit, li ordino per data 
		
		try { commits = addAllCommits(tickets, versions); } 
		catch (IOException e) { FileLogger.getLogger().error("Errore nell'aggiunta di tutte le commit"); System.exit(1); }
		
		orderCommits(commits);
		
		FileLogger.getLogger().info("\n\nAnalisi delle commit terminato.\n\nCommit trovate: " + commits.length);
		
		
		FileLogger.getLogger().info("\n\nFine commit trovate\n\n");
		
		
		FileLogger.getLogger().info("\n\nAggiungo le rispettive informazioni alle versioni (file e metriche)\n\n");
		
		versionHandler.handleVersion(git, commits, versions);
		
		FileLogger.getLogger().info("\n\nCalcolo delle versioni e delle metriche terminato. \n\n");
		
		
		//Chiusura repository
		git.close();
		
		
		//Eliminazione della directory temporanea con la repository
		try { FileUtils.deleteDirectory(new File(clonePath)); } 
		catch (IOException e) { 
			FileLogger.getLogger().warning("Errore nell'eliminnazione della directory del clone: " + e.getStackTrace()); 
		}
		

		return commits;
		
	}
	
	
	//Funzione che ottiene tutte le commit, usata per il coldstart
	public Commit[] getAllCommits(String url, BugTicket[] tickets, Version[] versions) {
		
		//Clonazione della repository
		cloneRepository(url);
		
		//Recupero del log
		Iterable<RevCommit> log = null;
			
		try { log = git.log().call(); } 
		catch (GitAPIException e) {FileLogger.getLogger().error("Errore nel recupero del log: " + e.getMessage()); System.exit(1);}
			
		ArrayList<Commit> commits =		//Array di commit
				new ArrayList<>();
		
		//Trovo la commit più recente per il ticket specificato da parametro
		for ( Iterator<RevCommit> iterator = log.iterator(); iterator.hasNext();) {
		    
			RevCommit rev = iterator.next();
		    String s = rev.getFullMessage();
		    ArrayList<BugTicket> currTickets = new ArrayList<>();
		    
		    //Verifico se il commit è relativo ad un bug presente su JIRA
			for(BugTicket ticket : tickets)
				if(ticketMatch(s, ticket.getTicketId())) {
						currTickets.add(ticket);
				}	
			       		
			for(Version version : versions)
				if((new Date((long) rev.getCommitTime() * 1000L).toInstant().atZone(ZoneId.of("Z")).toLocalDate()).isBefore(version.getDate()))
				{
					commits.add(new Commit(rev.getShortMessage(), currTickets, new Date((long)rev.getCommitTime() * 1000L), version, null));
					break;
				}   	
		}
			
		
		//Chiusura repository
		git.close();
				
				
		//Eliminazione della directory temporanea con la repository
		try { FileUtils.deleteDirectory(new File(clonePath)); } 
		catch (IOException e) { 
			FileLogger.getLogger().warning("Errore nell'eliminnazione della directory del clone: " + e.getStackTrace()); 
		}
				
		
		return commits.toArray(new Commit[0]);
				
				
	}
	
	
	
	
	
	//Metodo che clona la repository
	private void cloneRepository(String url)
	{
		FileLogger.getLogger().info("Clonazione della repository su: " + System.getProperty("user.dir") + "\\" + this.clonePath);
		
		try {
			this.git = Git.cloneRepository()
			   		  .setURI(url)
			   		  .setDirectory(new File(this.clonePath))
			   		  .call();
		} 
		catch (GitAPIException e) {
			FileLogger.getLogger().error("Errore nella clonazione della repository.");
			System.exit(1);
		}
		
		FileLogger.getLogger().info("Clonazione della repository effettuata con successo.\n\n");
	}

	
	
	
	
	
	
	
	
	private Commit[] addAllCommits(BugTicket[] tickets, Version[] versions) throws IOException
	{
		ArrayList<Commit> commits =		//Array di commit
				new ArrayList<>();

		RevCommit head = null;
		
		try (RevWalk walk = new RevWalk(git.getRepository())) 
		{
			walk.sort(RevSort.TOPO);
			head = walk.parseCommit(git.getRepository().exactRef("HEAD").getObjectId());

			int numCommit=0;
   
			while (head.getParentCount() != 0) {
				
				ArrayList<BugTicket> currTickets = new ArrayList<>();
				ArrayList<CommitFileOperation> fileTouched = new ArrayList<>();
				RevCommit headParent = 	walk.parseCommit(head.getParent(0));
				
				commitHasTicket(head, tickets, currTickets);
				
				FileLogger.getLogger().info("Questo commit ha " + head.getParentCount() + " parent(s)");
				
				DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
				df.setRepository(git.getRepository());
				df.setDiffComparator(RawTextComparator.DEFAULT);
				df.setContext(0);
				df.setDetectRenames(true);
				List<DiffEntry> diffEntries = new ArrayList<>();
				
				
				diffEntries = df.scan(headParent.getTree(), head.getTree());
				
				FileLogger.getLogger().info(numCommit + "#Commit: "  + head.getFullMessage().replace("\n", "\n |\t") + "\n |\tDifferenze:");
				
				int chgSetSize = calculateChgSetSize(diffEntries);
				
				for(DiffEntry diff : diffEntries)
				{
					int locTouched = 0;
					int locAdded = 0;
					int locDeleted = 0;
					int commitChg = chgSetSize;
					
					for (Edit edit : df.toFileHeader(diff).toEditList()) {
						locDeleted += edit.getEndA() - edit.getBeginA(); //Linee eliminate
						locAdded += edit.getEndB() - edit.getBeginB(); //Linee aggiunte
						
				    }
					
					locTouched = locAdded + locDeleted;
					
					FileLogger.getLogger().info(" |\t |\t" + diff.toString().replace("\n", "\n |\t |\t"));
					
					String oldPath = null;
					if(diff.getChangeType() == ChangeType.DELETE || diff.getChangeType() == ChangeType.RENAME) 
						oldPath = diff.getOldPath();
					if(diff.getChangeType() == ChangeType.DELETE)
						commitChg++; 
					
					int[] chgChurn = {commitChg, locAdded - locDeleted};
					
					fileTouched.add(new CommitFileOperation(diff.getNewPath(), oldPath, diff.getChangeType(), 
								locTouched, locAdded, head.getAuthorIdent().getName(), chgChurn));
				}
					
				
				addFileToVersion(versions, head, currTickets, commits, fileTouched);	
					
				
				df.close();	


				
				FileLogger.getLogger().info(" |\n +----------------------------------------------------------------------------------------------------\n");			       	
				
				head = walk.parseCommit(head.getParent(0));
				numCommit++;

			}
			FileLogger.getLogger().info("Numero commits: " + numCommit);

		
		}
		
		
		return commits.toArray(new Commit[0]);
	}
	
	private int calculateChgSetSize(List<DiffEntry> diffEntries) {
		int count = 0;
		
		for(DiffEntry diff : diffEntries)
			if(diff.getChangeType() != ChangeType.DELETE)
				count ++;
		
		return count - 1;
	}
	
	
	
	
	//Controlla se il commit ha un ticket e nel caso lo aggiunge a currTickets
	private void commitHasTicket(RevCommit head, BugTicket[] tickets, ArrayList<BugTicket> currTickets) {
		
		
		//Verifico se il commit è relativo ad un bug presente su JIRA
		for(BugTicket ticket : tickets)
			if(ticketMatch(head.getFullMessage(), ticket.getTicketId())) {
					FileLogger.getLogger().info("Questo commit è relativo al ticket: " + ticket.getTicketId());
					currTickets.add(ticket);
			}
		
		
	}
	
	//Aggiunge il file alla versione
	private void addFileToVersion(Version[] versions, RevCommit head, ArrayList<BugTicket> currTickets, ArrayList<Commit> commits, ArrayList<CommitFileOperation> fileTouched) {
		
		
		for(Version version : versions)
			if((new Date((long)head.getCommitTime() * 1000L).toInstant().atZone(ZoneId.of("Z")).toLocalDate()).isBefore(version.getDate()))
			{
				commits.add(new Commit(head.getShortMessage(), currTickets, new Date((long)head.getCommitTime() * 1000L), version, fileTouched));
				break;
			}
		
		
	}


	
	
	//Ordino le commits usando un algoritmo Bubble Sort
	private void orderCommits(Commit[] commits)
	{
		
		for(int i = 0; i < commits.length; i++)
			for(int j = 0; j < commits.length - 1; j++)
				if(commits[j].getDate().getTime() > commits[j+1].getDate().getTime()) {
					Commit tempCommit = commits[j];
					commits[j] = commits[j+1];
					commits[j+1] = tempCommit;
				}
	}
	
	
	
	
	
	
	//Questo metodo viene usato per assicurarsi che non vengano presi i commit che hanno come ID gli stessi numeri iniziali
	//(es. Se voglio cercare il ticket PROJ-12, voglio evitare che prenda per esempio PROJ-123)
	private boolean ticketMatch(String commitMessage, String ticket)
	{
		if(commitMessage.contains(ticket))
		{
			for(int i = 0; i < 10; i++)
			{
				if(commitMessage.contains(ticket + Integer.toString(i)))
					return false;
			}
			return true;
		}
		return false;
		
	}
	
}
