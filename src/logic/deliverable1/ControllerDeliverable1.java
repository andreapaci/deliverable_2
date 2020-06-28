package logic.deliverable1;

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
import util.FileLogger;


//Controller per la prima deliverable, creata come singleton
public class ControllerDeliverable1 
{
	
	private static ControllerDeliverable1 istance = null;
	
	private String[] javaExtensions = {".java"}; //, ".cpp", ".h", ".am", ".ac", ".Doxyfile", ".xml", ".conf", ".sh", ".properties", ".xml", ".html", ".js", ".py"
	

	
	private ControllerDeliverable1() {}
	
	
	public void run(String projName, String projUrl) {
		
		// Nomi dei progetti con cui fare il Cold Start
		String[] projColdStart = { "AVRO", "CHUKWA", "FALCON", "GIRAPH", "IVY", "OPENJPA", "PROTON", "SSHD", "STORM",
				"THRIFT", "ZOOKEEPER", "TAJO", "LUCENE" };
		// URL dei progetti su Github su cui fare il Cold Start
		String[] projUrlColdStart = { "https://github.com/apache/avro", "https://github.com/apache/chukwa",
				"https://github.com/apache/falcon", "https://github.com/apache/giraph",
				"https://github.com/apache/ant-ivy", "https://github.com/apache/openjpa",
				"https://github.com/apache/qpid-proton", "https://github.com/apache/mina-sshd",
				"https://github.com/apache/storm", "https://github.com/apache/thrift",
				"https://github.com/apache/zookeeper", "https://github.com/apache/tajo",
				"https://github.com/apache/lucene-solr/" };
		// Memorizza tutte le versioni del progetto
		Version[] versions = null;
		// Memorizza tutti i ticket con i relativi AV e FV
		BugTicket[] bugTickets = null;
		// Memorizza informazioni su tutti i commit
		Commit[] commits = null;
		// Handler per il recupero di informazioni su JIRA
		JsonJiraHandler jsonHandler = new JsonJiraHandler();
		// Handler per il recupero di informazioni su Github
		GithubHandler githubHandler = new GithubHandler();
		// Handler che calcola le metriche
		MetricHandler metricHandler = new MetricHandler();
		// Handler che calcola la Defectivness
		DefectHandler defectHandler = new DefectHandler();

		FileLogger.getLogger().info("Progetto: " + projName + "\n\n");
		FileLogger.getLogger().info("Recupero delle versioni di " + projName);

		/*
		 * ----------------------------------------------------------------- 
		 * Recupero delle versioni del progetto specificato
		 * -----------------------------------------------------------------
		 */

		try {
			versions = jsonHandler.retreiveVersionInfo(projName, true);
		} catch (IOException | JSONException e) {
			FileLogger.getLogger().error("Errore nel recupero delle versioni.");
			System.exit(1);
		}

		FileLogger.getLogger().info("Versioni recuperate:");

		for (Version version : versions)
			FileLogger.getLogger().info("\tVersion index: " + version.getIndex() + " - ID: " + version.getId()
					+ " - Name: " + version.getVersionName() + " - Released: " + version.getDate().toString());

		FileLogger.getLogger().info("Fine versioni recuperate.\n\n\n");

		/*
		 * ----------------------------------------------------------------- 
		 * Calcolo Proportion con Cold Start
		 * -----------------------------------------------------------------
		 */

		FileLogger.getLogger().info("\n\nCalcolo proportion mediande Cold Start.\n\n");

		ProportionHandler.getIstance().coldStart(projColdStart, projUrlColdStart);

		FileLogger.getLogger().info("\n\nProportion calcolata.\n\n");
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

		/*
		 * ----------------------------------------------------------------- 
		 * Recupero  dei file di ogni versione
		 * -----------------------------------------------------------------
		 */

		commits = githubHandler.retreiveFileVersion(projUrl, bugTickets, versions);

		/*
		 * ----------------------------------------------------------------- 
		 * Calcolo delle metriche
		 * -----------------------------------------------------------------
		 */

		FileLogger.getLogger().info("\n\nCalcolo delle metriche\n");

		metricHandler.calculateMetrics(commits, versions);

		FileLogger.getLogger().info("\n\nMetriche calcolate.\n");

		/*
		 * ----------------------------------------------------------------- 
		 * Calcolo delle classi defective
		 * -----------------------------------------------------------------
		 */

		FileLogger.getLogger().info("\n\nCalcolo classi defective\n");

		defectHandler.calculateDefect(commits, bugTickets, versions);

		FileLogger.getLogger().info("\n\nDefectiveness calcolata\n");

		/*
		 * ----------------------------------------------------------------- 
		 * Generazione del CSV 
		 * -----------------------------------------------------------------
		 */

		generateCSV(projName, versions);

	}

	private void generateCSV(String projName, Version[] versions) 
	{

		// Elimino il file se già esistente
		try {
			Files.delete(Paths.get("output/deliverable_1/" + projName + "_metrics.csv"));
		} catch (IOException e1) {

			FileLogger.getLogger().warning("Errore nell'eliminazione del file .csv");
		}

		try (BufferedWriter br = new BufferedWriter(
				new FileWriter("output/deliverable_1/" + projName + "_metrics.csv"))) {
			StringBuilder sb = new StringBuilder();

			sb.append("Version,FileName,Size,Age,Number_revision,Number_Authors,Loc_Touched,Loc_Added,Max_Loc_Added,Avg_Loc_Added,Change_Set_size,Max_ChgSetSize,Avg_ChgSetSize,Churn,Max_Churn,Avg_Churn,Defective\n");

			for (int i = 0; i < versions.length / 2; i++) {
				for (VersionFile file : versions[i].getFilesPath()) {
					if (checkExtension(file.getFilePath())) {
						sb.append(versions[i].getIndex());
						sb.append(",");
						sb.append(file.getFilePath());
						sb.append(",");
						sb.append(file.getSize());
						sb.append(",");
						sb.append(file.getAge());
						sb.append(",");
						sb.append(file.getRevisionNumber());
						sb.append(",");
						sb.append(file.getNumberAuthor());
						sb.append(",");
						sb.append(file.getLocTouched());
						sb.append(",");
						sb.append(file.getLocAdded());
						sb.append(",");
						sb.append(file.getLocMax());
						sb.append(",");
						sb.append(file.getLocAvg());
						sb.append(",");
						sb.append(file.getChgSize());
						sb.append(",");
						sb.append(file.getMaxChg());
						sb.append(",");
						sb.append(file.getAvgChg());
						sb.append(",");
						sb.append(file.getChurn());
						sb.append(",");
						sb.append(file.getMaxChurn());
						sb.append(",");
						sb.append(file.getAvgChurn());
						sb.append(",");
						sb.append(file.isDefective());
						sb.append("\n");
					}
				}
			}

			br.write(sb.toString());
		} catch (Exception e) {

			FileLogger.getLogger().warning("Errore nella scrittura del file .csv");
		}

	}

	// Metodo che controlla se il file contiene l'estensione desiderata, in tal caso
	// torna true
	private boolean checkExtension(String file) {
		for (String extension : javaExtensions)
			if (file.contains(extension))
				return true;

		return false;
	}
	
	
	
	public static ControllerDeliverable1 getIstance() {
		if(istance == null) istance = new ControllerDeliverable1();
		return istance;
	}
}
