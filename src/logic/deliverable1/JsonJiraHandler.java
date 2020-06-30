package logic.deliverable1;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.BugTicket;
import entity.Version;
import util.FileLogger;


//Classe addetta al recupero delle informazioni su JIRA
public class JsonJiraHandler {
	
	String jsonArrayName = "versions";
	String jsonFieldName = "fields";

	// Ottiene informazioni sulle varie release del progetto specificato da projName
	// Aggiungo una release "extra" chiamata "Unreleased" per memorizzare tutte le informazioni sui commits/tickets che non appartengono a nessuna versione
	public Version[] retreiveVersionInfo(String projName, boolean writeCSV) throws IOException, JSONException {

		// Array contentente le date delle releases
		List<LocalDateTime> releases = new ArrayList<>();
		// HashMap contenete la coppia ...
		HashMap<LocalDateTime, String> releaseNames = new HashMap<>();
		// HashMap contenete la coppia ...
		HashMap<LocalDateTime, String> releaseID = new HashMap<>();
		//Array delle versioni
		ArrayList<Version> versionsList = new ArrayList<>();
		// Url del link su JIRA relativo al progetto
		String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
		// File JSON su cui fare il retreive delle informazioni
		JSONObject json;
		// Array JSON con le versioni delle release
		JSONArray versions;
		
		

		json = readJsonFromUrl(url);
		versions = json.getJSONArray(jsonArrayName);

		for (int i = 0; i < versions.length(); i++) {
			String name = "";
			String id = "";
			
			//La salvo solo se ha una release date
			if (versions.getJSONObject(i).has("releaseDate")) {
				
				if (versions.getJSONObject(i).has("name")) name = versions.getJSONObject(i).get("name").toString();
				if (versions.getJSONObject(i).has("id")) id = versions.getJSONObject(i).get("id").toString();
				
				//Aggiungo la release trovata
				addRelease(releases, releaseNames, releaseID, 
						versions.getJSONObject(i).get("releaseDate").toString(),name, id);
				
				
			}
		}

		// Ordino le releases dalla data (utlizzo di LAMBDA)
		Collections.sort(releases, (o1, o2) -> o1.compareTo(o2));
		

		//faccio l'export e il salvataggio dei dati
		exportAndSaveData(writeCSV, projName, releases, releaseID, releaseNames, versionsList);
				
		
		
		return versionsList.toArray(new Version[0]);
	}
	
	//Metodo che esporta i valori su un file CSV e li salva su una lista
	private void exportAndSaveData(boolean writeCSV, String projName, List<LocalDateTime> releases, HashMap<LocalDateTime, String> releaseID, 
				HashMap<LocalDateTime, String> releaseNames, ArrayList<Version> versionsList) {
		if (writeCSV) {
				
			// Scrivo il risultato in un file csv
				
			String outname = "output/deliverable_1/" + projName + "_VersionInfo.csv";
			try (FileWriter fileWriter = new FileWriter(outname)) {

				fileWriter.append("Index;Version ID;Version Name;Date");
				fileWriter.append("\n");

				for (int i = 0; i < releases.size(); i++) {
					Integer index = i + 1;
					fileWriter.append(index.toString());
					fileWriter.append(";");
					fileWriter.append(releaseID.get(releases.get(i)));
					fileWriter.append(";");
					fileWriter.append(releaseNames.get(releases.get(i)));
					fileWriter.append(";");
					fileWriter.append(releases.get(i).toLocalDate().toString());
					fileWriter.append("\n");

					// Aggiungo il risultato in una lista
					versionsList.add(new Version(index, releaseID.get(releases.get(i)),
							releaseNames.get(releases.get(i)), releases.get(i).toLocalDate()));
				}
					
					
			} 
			catch (Exception e) {
				FileLogger.getLogger().warning("Errore nella scrittura del file CSV.");
			} 
			
				
			versionsList.add(new Version(versionsList.size() + 1, "LastVersion", "Unreleased Version", LocalDate.now()));
		}
		else
		{
			for (int i = 0; i < releases.size(); i++) {
				Integer index = i + 1;

				// Aggiungo il risultato in una lista
				versionsList.add(new Version(index, releaseID.get(releases.get(i)),
						releaseNames.get(releases.get(i)), releases.get(i).toLocalDate()));

			}
		}
			
			
	}
	
	

	
	//Aggiungi la release alle liste passate come parametro
	private void addRelease(List<LocalDateTime> releases, Map<LocalDateTime, String> releaseNames,
			Map<LocalDateTime, String> releaseID, String strDate, String name, String id) {

		//Faccio il parsing della data
		LocalDate date = LocalDate.parse(strDate);
		
		LocalDateTime dateTime = date.atStartOfDay();
		if (!releases.contains(dateTime))
			releases.add(dateTime);
		
		releaseNames.put(dateTime, name);
		releaseID.put(dateTime, id);
		
	}
	
	
	
	
	
	
	

	// Metodo per il retrive del file JSON con i filtri assegnati (fixed bugs)
	public BugTicket[] retriveJiraBugJsonFromURL(String projName) throws JSONException, IOException {

		final int delta = 50; 				// Di quanto si spostano gli indici per estrapolare il file JSON
		int startAt = 0; 					// Punto iniziale da cui estrapolare il file JSON (incluso)
		int maxResults = 0; 				// Punto finale da cui estrapolare il file JSON (escluso)
		int total; 							// Numero totale di elementi presenti da estrapolare
		ArrayList<BugTicket> bugTickets = 	// Array di BugTickets di ID dei ticket
				new ArrayList<>();

		do {

			maxResults += delta;

			// Siamo interessati unicamente al ticket
			String jsonUrl = "https://issues.apache.org/jira/rest/api/2/search?jql=project%20%3D%20" + projName
					+ "%20AND%20issuetype%20%3D%20Bug%20AND%20status%20in%20"
					+ "(Resolved%2C%20Closed)%20AND%20resolution%20%3D%20Fixed%20ORDER%20BY%20updated%20DESC&fields=key,versions,fixVersions,created" + "&startAt=" + Integer.toString(startAt)
					+ "&maxResults=" + Integer.toString(maxResults);

			// Estrapolo il file JSON
			JSONObject json = readJsonFromUrl(jsonUrl);

			// Faccio il parsing dei primi elementi trovati
			JSONArray issues = json.getJSONArray("issues");
			total = json.getInt("total");

			for (; startAt < total && startAt < maxResults; startAt++) {

				addBugTicket(issues, startAt, delta, bugTickets);
			}

		} while (startAt < total);

		FileLogger.getLogger().info("Numero di ticket trovati: " + total + "\n");
		
		BugTicket[] ticketArray = bugTickets.toArray(new BugTicket[0]);

		orderTicket(ticketArray);

		return ticketArray;

	}
	
	//Ordino i ticket usando un algoritmo Bubble Sort
	private void orderTicket(BugTicket[] tickets)
	{
		for(int i = 0; i < tickets.length; i++)
			for(int j = 0; j < tickets.length - 1; j++)
				if(tickets[j].getDate().isAfter(tickets[j+1].getDate())) {
					BugTicket tempTicket = tickets[j];
					tickets[j] = tickets[j+1];
					tickets[j+1] = tempTicket;
				}
	}
	
	
	
	//Metodo per aggiungere il ticket ad una lista
	private void addBugTicket(JSONArray issues, int startAt, int delta, ArrayList<BugTicket> bugTickets) throws JSONException {
		//Ottieni id ticket
		String ticketId = issues.getJSONObject(startAt % delta).get("key").toString();
		String[] fixedVersions = null;
		String[] versions = null;
		LocalDate date = LocalDate.parse(((CharSequence) issues.getJSONObject(startAt % delta).getJSONObject(jsonFieldName).get("created")).subSequence(0,  10));
		
		
		
		FileLogger.getLogger().info("\tID chiave #" + startAt + ": " + ticketId + " [" + date.toString() + "]");
		
		
		
		//Ottienei id affected versions
		int numberAffectedVersions = issues.getJSONObject(startAt % delta).getJSONObject(jsonFieldName).getJSONArray(jsonArrayName).length();
		
		if(numberAffectedVersions > 0)
		{
			FileLogger.getLogger().info("\t |\tLe affected versions sono:");
			versions = new String[numberAffectedVersions]; 
			
			for(int i = 0; i < numberAffectedVersions; i++)
			{
				versions[i] = issues.getJSONObject(startAt % delta).getJSONObject(jsonFieldName).getJSONArray(jsonArrayName).getJSONObject(i).get("id").toString();
				FileLogger.getLogger().info("\t |\t |\tID: " + versions[i]);
			}
			
		}
		else
		{
			FileLogger.getLogger().info("\t |\tNon sono presenti affected versions.");
		}
		
		
		
		
		//Ottieni id fixed versions
		
		int numberFixedVersions = issues.getJSONObject(startAt % delta).getJSONObject(jsonFieldName).getJSONArray("fixVersions").length();
		
		if(numberFixedVersions > 0)
		{
			FileLogger.getLogger().info("\t |\tLe fixed versions sono:");
			fixedVersions = new String[numberFixedVersions]; 
			
			for(int i = 0; i < numberFixedVersions; i++)
			{
				fixedVersions[i] = issues.getJSONObject(startAt % delta).getJSONObject(jsonFieldName).getJSONArray("fixVersions").getJSONObject(i).get("id").toString();
				FileLogger.getLogger().info("\t |\t |\tID: " + fixedVersions[i]);
			}
		
		}
		else
		{
			FileLogger.getLogger().info("\t |\tNon sono presenti fixed versions.");
		}
		
		FileLogger.getLogger().info("\t +--------------------------------------------");
		
		
		bugTickets.add(new BugTicket(ticketId, versions, fixedVersions, date));
		
	}

	// Legge tutto il contenuto di un Buffer
	private String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	// Metodo per estrapolare JSON da URL
	private JSONObject readJsonFromUrl(String url) throws IOException, JSONException {

		// Apro lo stream di connessione verso l'URL

		JSONObject json = null;
		try (InputStream is = new URL(url).openStream();) {

			BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			String jsonText = readAll(rd);
			json = new JSONObject(jsonText);

		} catch (Exception e) {
			FileLogger.getLogger().error("Errore nel recupero del file JSON: " + e.getMessage());
			System.exit(1);
		}

		return json;
	}

}
