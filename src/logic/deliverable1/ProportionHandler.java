package logic.deliverable1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;

import entity.BugTicket;
import entity.Commit;
import entity.Version;
import util.FileLogger;


//Implemento la classe come Singleton per assicurarmi la singola istanza dell'Handler
public class ProportionHandler 
{
	
	private static ProportionHandler 
		proportionHandler = null;								//Istanza del ProportionHandler
	private float proportionColdStart;							//Valore della proportion P
	private List<Float> proportions;							//Lista di tutte le proportion P (usata per Incremental)
	private static final int MINP = 3;							//Il numero minimo di proportion calcolate per applicare incremental
	private static final String FILENAME = "coldStart_p.txt";	//Nome del file da dove leggo il proportion computato con cold start
																//(Evito di ricomputare il Proportion cold start ad ogni iterazione del programma,
																// mi salvo il valore su un file che riuso per esecuzioni successive)
	
	
	
	private ProportionHandler() {
		proportions = new ArrayList<>();
	}
	
	public static ProportionHandler getIstance() {
		if(proportionHandler == null) proportionHandler = new ProportionHandler();
		return proportionHandler;
	}
	
	//Metodo per fare il coldstart usando i progetti in projectsName
	public void coldStart(String[] projectsName, String[] projUrl)
	{
		File coldStartFile = new File(FILENAME);
		boolean fileIsRead = false;
		//Se il file esiste, e il dato scritto nella prima linea è un float, allora uso quello, sennò viene ricalcolato
		if(coldStartFile.exists()) {
			
			try(BufferedReader fileBuffer = new BufferedReader(new FileReader(FILENAME))) {
				
				String line = fileBuffer.readLine();
				
				//Se questa istruzione non lancia una eccezione, vuol dire che è un floatr e quindi può essere letto
				this.proportionColdStart = Float.parseFloat(line);
		
				fileIsRead = true;
				
			} 
			catch (NumberFormatException | IOException e) {FileLogger.getLogger().warning("Errore nella lettura del file");}
		}
		
		if(!fileIsRead)
			computeColdStart(projectsName, projUrl);
		
		FileLogger.getLogger().info("\nLa mediana di P calcolata mediante Cold Start P: " + this.proportionColdStart + "\n\n");
		
	}
	
	private void computeColdStart(String[] projectsName, String[] projUrl) {
		//Variabile per gesire i ticket e le versioni su JIRA
		JsonJiraHandler jiraHandler = new JsonJiraHandler();
		//Variabile per ottenere i commit per il calcolo di FV
		GithubHandler githubHandler = new GithubHandler();
		//Variabile delle proporzioni
		List<Float> proportion = new ArrayList<>();
				
				
		for(int i = 0; i < projectsName.length; i++) {
			try {
				
				//Variabile per memorizzare le versioni su JIRA
				Version[] versions = null;
				//Variabile per memorizzare i ticket su JIRA
				BugTicket[] tickets = null;
				//Variabile per memorizzare i commits
				Commit[] commits = null;
						
				versions = jiraHandler.retreiveVersionInfo(projectsName[i], false);
				tickets = jiraHandler.retriveJiraBugJsonFromURL(projectsName[i]);
				commits = githubHandler.getAllCommits(projUrl[i], tickets, versions);
						
						
				for(BugTicket ticket : tickets) {
					if(ticket.hasAVFV() && versions.length > 3) {
						int fv = getFV(ticket, commits);
						int iv = getIV(ticket, versions);
						int ov = getOV(ticket, versions);
						addColdStartProportion(iv, ov, fv, proportion);
								
										
					}
				}		
						
			} 
			catch (JSONException | IOException e) {
				FileLogger.getLogger().warning("Errore nel recupero delle AV/FV di " + projectsName[i] );			
			}
		}
				
		saveColdStartProportion(proportion);
	}
	
	
	//Salva e applica il valore di proportion calcolato mediante cold start
	private void saveColdStartProportion(List<Float> proportion) {
		Collections.sort(proportion);
		
		float medianProportion = proportion.get(proportion.size()/2); 
		this.proportionColdStart = medianProportion;
		
		//Scrivo sul file il valore ottenuto
		try(PrintWriter fileWriter = new PrintWriter(new BufferedWriter(new FileWriter(FILENAME)))) {
			fileWriter.println(this.proportionColdStart);
		} 
		catch (IOException e) {FileLogger.getLogger().warning("Errore nella scrittura del file");}
	}
	
	
	
	//Se i valori sono corretti e utlizzabili, aggiunge i valori alla lista delle proporzioni per fare il cold start
	private void addColdStartProportion(int iv, int ov, int fv, List<Float> proportion) {
		if(!(iv == -1 || ov == -1 || fv == -1) && !(fv < ov || ov < iv) && fv != ov)
			proportion.add(((float)(fv - iv)/(float)(fv - ov)));
				
	}
	
	
	
	
	//Metodo per ottenere la fixed version (viene prese la fixed version più vecchia)
	public int getFV(BugTicket ticket, Commit[] commits)
	{
		int versionIndex = -1;
		
		if (commits != null) {
				
			Commit lastCommit = null;

			for (Commit commit : commits) {
				if ((commit.getTicket().contains(ticket)) &&
					 (lastCommit == null || lastCommit.getDate().getTime() < commit.getDate().getTime()))
					lastCommit = commit;

					
			}

			if(lastCommit != null)
				versionIndex = lastCommit.getVersion().getIndex();
			
		} 
		
		return versionIndex;
	}
	
	
	
	public int getIV(BugTicket ticket, Version[] versions)
	{
		
		List<Integer> affectedVersionIndexes = new ArrayList<>();
		
		for(String av : ticket.getAffectedVersions())
			for(Version version : versions)
				if(version.getId().equals(av))
					affectedVersionIndexes.add(version.getIndex());
		
		if(affectedVersionIndexes.isEmpty())	
			return -1;
		
		
		
		int minAv = affectedVersionIndexes.get(0);
		
		for(int av : affectedVersionIndexes) {
			if(minAv > av) minAv = av;
		}
		
		return minAv;				
			
	}
	
	
	
	public int getOV(BugTicket ticket, Version[] versions) {
		for(Version version : versions)
			if(ticket.getDate().isBefore(version.getDate()))
				return version.getIndex();
		

		return -1;
	}
	
	
	
	
	public float getProportion() {
		
		//Se ho almeno "minP" proportion, applico incremental
		if(proportions.size() >= MINP) {
			float avg = averageP();
			FileLogger.getLogger().info("Applico il proportion incremental P = " + avg);
			return avg;
		}
		return proportionColdStart;
	}
	
	
	
	public void addProportion(float p) {
		proportions.add(p);
	}
	
	
	
	private float averageP() {
		float sum = 0;
		
		for(float p : this.proportions) sum += p;
		
		return sum/this.proportions.size();
	}

}
