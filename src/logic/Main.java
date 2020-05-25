package logic;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONException;

import entity.BugTicket;
import entity.Commit;
import entity.Version;
import entity.VersionFile;

public class Main {

	public static void main(String[] args) 
	{
		//Nome del progetto
		String projName = "BOOKKEEPER";
		//Url del progetto su Github
		String projUrl = "https://github.com/apache/bookkeeper";
		//Nomi dei progetti con cui fare il Cold Start
		String[] projColdStart = {"AVRO", "CHUKWA", "FALCON", "GIRAPH", "IVY", "OPENJPA", "PROTON", "SSHD", "STORM", "THRIFT", "WHIRR", "ZEPPELIN", "ZOOKEEPER", "CONNECTORS", "CRUNCH", "TAJO", "TEZ", "TOMEE", "LUCENE", "STRATOS" };
		//Memorizza tutte le versioni del progetto
		Version[] versions = null;
		//Memorizza informazioni su tutti i commit
		Commit[] commits = null;
		//Memorizza tutti i ticket con i relativi AV e FV
		BugTicket[] bugTickets = null;
		//Handler per il recupero di informazioni su JIRA
		JsonJiraHandler jsonHandler = new JsonJiraHandler();
		//Handler per calcolare il proportion
		ProportionHandler proportionHandler = new ProportionHandler();
		//Handler per il recupero di informazioni su Github
		GithubHandler githubHandler = new GithubHandler();
		//Handler che calcola le metriche
		MetricHandler metricHandler = new MetricHandler();
		
		FileLogger.getLogger().info("Progetto: " + projName + "\n\n");
		FileLogger.getLogger().info("Recupero delle versioni di " + projName);
		
		
		/*
		 * -----------------------------------------------------------------
		 * Recupero delle versioni del progetto specificato
		 * -----------------------------------------------------------------
		 */
		
		try { versions = jsonHandler.retreiveVersionInfo(projName, true); } 
		catch (IOException | JSONException e) {
			FileLogger.getLogger().error("Errore nel recupero delle versioni:" + e.getStackTrace().toString());
			System.exit(1);
		}
		
		FileLogger.getLogger().info("Versioni recuperate:");
		
		for(Version version : versions)
			FileLogger.getLogger().info("\tVersion index: " + version.getIndex() + " - ID: " + 
					version.getId() + " - Name: " + version.getVersionName() + " - Released: " + version.getDate().toString());
			
		FileLogger.getLogger().info("Fine versioni recuperate.\n\n\n");
		
		/*
		 * -----------------------------------------------------------------
		 * Applico Proportion Cold Start
		 * -----------------------------------------------------------------
		 */
		
		
		proportionHandler.coldStart(projColdStart);
		
		
		
		
		
		
		/*
		 * -----------------------------------------------------------------
		 * Recupero dei Bug su JIRA con AV e FV
		 * -----------------------------------------------------------------
		 */
		
		FileLogger.getLogger().info("Recupero dei bug su JIRA con relative AV e FV (se presenti)");
		
		try {
			bugTickets = jsonHandler.retriveJiraBugJsonFromURL(projName);
		} catch (JSONException | IOException e) {
			FileLogger.getLogger().error("Errore nel parsing dei bug. " + e.getStackTrace());
			System.exit(1);
		}
		
		FileLogger.getLogger().info("Bug recuperati.\n\n");
		
		
		
		//Stampa di prova per verificare che tutti i ticket sono stati effettivamente riportati correttamente
		
		FileLogger.getLogger().info("I ticket recuperati sono in totale: " + bugTickets.length + "\n");
		
		/*for(BugTicket bugTicket : bugTickets)
		{
			FileLogger.getLogger().info("\tTicket: " + bugTicket.getTicketId());
			
			FileLogger.getLogger().info("\t |\tOpening Version: " + bugTicket.getOpeningVersion());
			
			FileLogger.getLogger().info("\t |\tAffected Version:");
			if(bugTicket.getAffectedVersions() != null)
				for(String affectedVersion : bugTicket.getAffectedVersions())
					FileLogger.getLogger().info("\t |\t\t" + affectedVersion);
			
			FileLogger.getLogger().info("\t |\tFixed Version:");
			if(bugTicket.getFixedVersion() != null)
				for(String fixedVersion : bugTicket.getFixedVersion())
					FileLogger.getLogger().info("\t |\t\t" + fixedVersion);
			
			FileLogger.getLogger().info("\t +------------------------------------------------");
		
		}
		*/
		FileLogger.getLogger().info("\n\n");
		
		
		
		/*
		 * -----------------------------------------------------------------
		 * Recupero dei file di ogni versione
		 * -----------------------------------------------------------------
		 */
		
		commits = githubHandler.retreiveFileVersion(projUrl, bugTickets, versions);
		
		
		FileLogger.getLogger().info("\n\nCalcolo delle metriche\n");
		metricHandler.calculateMetrics(commits, versions);
		
		
		//Elimino il file se già esistente
				try {
					Files.delete(Paths.get("metrics.csv"));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				try (BufferedWriter br = new BufferedWriter(new FileWriter("metrics.csv"))) {
					StringBuilder sb = new StringBuilder();
					
					
					
					
					
					sb.append("Project name");
					sb.append(";");
					sb.append(projName);
					sb.append("\n");
					
			
				
					
				
					
					sb.append("Version;FileName;Size;Age;Number of revision;Number of Authors;Loc Touched;Loc Added;Max Loc Added;Avg Loc Added;Change Set size;Max ChgSetSize;Avg ChgSetSize;\n");

				
					for (Version version : versions) 
					{
						for(VersionFile file : version.getFilesPath())
						{
							sb.append(version.getIndex());
							sb.append(";");
							sb.append(file.getFilePath());
							sb.append(";");
							sb.append(file.getSize());
							sb.append(";");
							sb.append(file.getAge());
							sb.append(";");
							sb.append(file.getRevisionNumber());
							sb.append(";");
							sb.append(file.getNumberAuthor());
							sb.append(";");
							sb.append(file.getLocTouched());
							sb.append(";");
							sb.append(file.getLocAdded());
							sb.append(";");
							sb.append(file.getLocMax());
							sb.append(";");
							sb.append(file.getLocAvg());
							sb.append(";");
							sb.append(file.getChgSize());
							sb.append(";");
							sb.append(file.getMaxChg());
							sb.append(";");
							sb.append(file.getAvgChg());
							sb.append(";");
							sb.append("\n");
						}
					}

					br.write(sb.toString());
				}
				catch(Exception e) {  }

		
	}
	

}