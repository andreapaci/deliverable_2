package logic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import entity.BugTicket;
import entity.Commit;
import entity.CommitFileOperation;
import entity.Version;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
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
		
		try { commits = addAllCommitsv2(tickets, versions); } 
		catch (IOException | GitAPIException e) { FileLogger.getLogger().error("Errore nell'aggiunta di tutte le commit"); System.exit(1); }
		
		orderCommits(commits);
		
		FileLogger.getLogger().info("\n\nAnalisi delle commit terminato.\n\nCommit trovate: " + commits.length);
		
		
		for(Commit commit : commits) 
			FileLogger.getLogger().info(commit.printCommitValues(true));
			
		
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

	
	
	
	
	
	
	
	
	private Commit[] addAllCommitsv2(BugTicket[] tickets, Version[] versions) throws IOException, GitAPIException
	{
		ArrayList<Commit> commits =		//Array di commit
				new ArrayList<>();
		
		//RevWalk revWalk = new RevWalk( git.getRepository() );
		//revWalk.sort( RevSort.TOPO );

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
				//Verifico se il commit è relativo ad un bug presente su JIRA
				for(BugTicket ticket : tickets)
					if(ticketMatch(head.getFullMessage(), ticket.getTicketId())) {
							FileLogger.getLogger().info("Questo commit è relativo al ticket: " + ticket.getTicketId());
							currTickets.add(ticket);
					}	
				
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
					int commitChg = chgSetSize;
					
					for (Edit edit : df.toFileHeader(diff).toEditList()) {
						locTouched += edit.getEndA() - edit.getBeginA(); //Linee eliminate
						locAdded += edit.getEndB() - edit.getBeginB(); //Linee aggiunte
						
				    }
					
					locTouched += locAdded;
					
					//FileLogger.getLogger().info(" |\t |\t" + diff.toString().replace("\n", "\n |\t |\t"));
					
					String oldPath = null;
					if(diff.getChangeType() == ChangeType.DELETE || diff.getChangeType() == ChangeType.RENAME) 
						oldPath = diff.getOldPath();
					if(diff.getChangeType() == ChangeType.DELETE)
						commitChg++; 
					fileTouched.add(new CommitFileOperation(diff.getNewPath(), oldPath, diff.getChangeType(), 
								locTouched, locAdded, head.getAuthorIdent().getName(), commitChg));
				}
					
				for(Version version : versions)
					if((new Date((long)head.getCommitTime() * 1000L).toInstant().atZone(ZoneId.of("Z")).toLocalDate()).isBefore(version.getDate()))
					{
						commits.add(new Commit(head.getShortMessage(), currTickets, new Date((long)head.getCommitTime() * 1000L), version, fileTouched));
						break;
					}
					
					
				
				df.close();	


				
				FileLogger.getLogger().info(" |\n +----------------------------------------------------------------------------------------------------\n");			       	
				
				head = walk.parseCommit(head.getParent(0));
				numCommit++;

			}
			FileLogger.getLogger().info("Numero commits: " + numCommit);

			walk.close();
		}
		
		//revWalk.close();
		return commits.toArray(new Commit[0]);
	}
	
	private int calculateChgSetSize(List<DiffEntry> diffEntries) {
		int count = 0;
		
		for(DiffEntry diff : diffEntries)
			if(diff.getChangeType() != ChangeType.DELETE)
				count ++;
		
		return count - 1;
	}

	private Commit[] addAllCommits(BugTicket[] tickets, Version[] versions) throws IOException, GitAPIException
	{
		
		//Recupero del log
		
		Iterable<RevCommit> log = null;	//Log delle commit
		RevCommit rev = null;			//Commit
		int commitsNumber = 1;			//Numero delle commit (parte da 1 poichè la prima commit non la considero)
		ArrayList<Commit> commits =		//Array di commit
				new ArrayList<>();
		
		
		try { log = git.log().call(); } 
		catch (GitAPIException e) { FileLogger.getLogger().error("Errore nel recupero del log: " + e.getMessage()); System.exit(1);}
		
		
		Iterator<RevCommit> iterator = log.iterator();

		
		
		
		while(iterator.hasNext()) {
			

			rev = iterator.next();
		

			ArrayList<BugTicket> currTickets = new ArrayList<>();
			ArrayList<CommitFileOperation> fileTouched = new ArrayList<>();
				
			//Verifico se il commit è relativo ad un bug presente su JIRA
			for(BugTicket ticket : tickets)
				if(ticketMatch(rev.getFullMessage(), ticket.getTicketId())) {
						FileLogger.getLogger().info("Questo commit è relativo al ticket: " + ticket.getTicketId());
						currTickets.add(ticket);
				}	
			
			FileLogger.getLogger().info("Questo commit ha " + rev.getParentCount() + " parent(s)");
			
			DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
			df.setRepository(git.getRepository());
			df.setDiffComparator(RawTextComparator.DEFAULT);
			df.setContext(0);
			df.setDetectRenames(true);
			List<DiffEntry> diffEntries = new ArrayList<>();
			
			if(rev.getParentCount() == 0)
			{
				RevTree tree = null;
				try (RevWalk revWalkA = new RevWalk(git.getRepository())) {
					tree = revWalkA.parseTree(rev.getId());    
				}
				catch(Exception e) { FileLogger.getLogger().error("Errore nel recupero del walk"); System.exit(1); }

				try (ObjectReader reader = git.getRepository().newObjectReader()) {
					CanonicalTreeParser iteratorTree = new CanonicalTreeParser();
				    iteratorTree.reset(reader, tree);
				    diffEntries = df.scan(new EmptyTreeIterator(), iteratorTree);
				}
				catch(Exception e) {FileLogger.getLogger().error("Errore nella creazione del tree"); System.exit(1);}
					
			}
			else 
				diffEntries = df.scan(rev.getParent(0).getTree(), rev.getTree());
				
				
				
				
			FileLogger.getLogger().info(commitsNumber + "#Commit: "  + rev.getFullMessage().replace("\n", "\n |\t") + "\n |\tDifferenze:");
				
			for(DiffEntry diff : diffEntries)
			{
				FileLogger.getLogger().info(" |\t |\t" + diff.toString().replace("\n", "\n |\t |\t"));					
				//if(diff.getChangeType() == ChangeType.DELETE || diff.getChangeType() == ChangeType.RENAME) 
					//fileTouched.add(new CommitFileOperation(diff.getNewPath(), diff.getOldPath(), diff.getChangeType()));
				//else
					//fileTouched.add(new CommitFileOperation(diff.getNewPath(), null, diff.getChangeType()));
			}
				
			for(Version version : versions)
				if((new Date((long)rev.getCommitTime() * 1000L).toInstant().atZone(ZoneId.of("Z")).toLocalDate()).isBefore(version.getDate()))
				{
					commits.add(new Commit(rev.getShortMessage(), currTickets, new Date((long)rev.getCommitTime() * 1000L), version, fileTouched));
					break;
				}
				
				
			commitsNumber++;
				
			
			df.close();	


			
			FileLogger.getLogger().info(" |\n +----------------------------------------------------------------------------------------------------\n");			       	
			
		}

	FileLogger.getLogger().info("\n\nIl numero delle commit totali è: " + commitsNumber);

	return commits.toArray(new Commit[0]);
		
	}
	
	
	
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
	
	
	
	
	//Metodo per recuperare la data dell'ultimo commit relativo ad un ticket
	private Date retreiveLastCommitTicket(String ticket)
	{
		//Recupero del log
		
		Iterable<RevCommit> log = null;
		
		try { log = git.log().call(); } 
		catch (GitAPIException e) {FileLogger.getLogger().error("Errore nel recupero del log: " + e.getMessage()); System.exit(1);}
		
		//La data viene usata per fare i paragoni su quale sia la più recente
		Date date = new Date(0L);
		
		//Trovo la commit più recente per il ticket specificato da parametro
		for ( Iterator<RevCommit> iterator = log.iterator(); iterator.hasNext();) {
	       	
			RevCommit rev = iterator.next();
	       	String s = rev.getFullMessage();
	       	
	       	if(ticketMatch(s, ticket) && (long) rev.getCommitTime() * 1000L >= date.getTime())
	       		date = new Date( (long) rev.getCommitTime() * 1000L );
	       	
	    }
		
		//Se la data è "0" (cioè da dove si inizia a contare, 1 gennaio 1970) vuol dire che 
		// non è stata trovata una corrispondenza del ticket tra JIRA e Github
		if(date.getTime() != 0) FileLogger.getLogger().info("L'ultimo commit per il ticket [" + ticket + "] e': " + date);
		else FileLogger.getLogger().info("Non nono presenti commit su Github con il ticket [" + ticket + "].");
		
		return date;
		
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
