package logic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;

import entity.BugTicket;
import entity.Version;

public class ProportionHandler 
{
	
	//Metodo per fare il coldstart usando i progetti in projectsName
	public float coldStart(String[] projectsName)
	{
		//Variabile per gesire i ticket e le versioni su JIRA
		JsonJiraHandler jiraHandler = new JsonJiraHandler();
		//Variabile per memorizzare le versioni su JIRA
		Version[] versions= null;
		//Variabile per memorizzare i ticket su JIRA
		BugTicket[] tickets = null;
		//Variabile delle proporzioni
		List<Float> proportion = new ArrayList<>();
		
		
		for(String projectName : projectsName) {
			try {
				versions = jiraHandler.retreiveVersionInfo(projectName, false);
				tickets = jiraHandler.retriveJiraBugJsonFromURL(projectName);
				
				for(BugTicket ticket : tickets) {
					if(ticket.hasAV() && versions.length > 3) {
						int FV = getFV(ticket, versions);
						int IV = getIV(ticket, versions);
						int OV = getOV(ticket, versions);
						if(!(IV == -1 || OV == -1 || FV == -1) && !(FV < OV || OV < IV) && FV != OV)
							proportion.add((float) ((float)(FV - IV)/(float)(FV - OV)));
						
						
								
					}
				}
				
				
				
			} catch (JSONException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Collections.sort(proportion);
		
		for(float p : proportion)
			System.out.println(p);
		
		float medianProportion = proportion.get((int) proportion.size()/2); 
		
		System.out.println("Median P: " + medianProportion);
		
		return medianProportion;
	}
	
	
	//Metodo per ottenere la fixed version (viene prese la fixed version più vecchia)
	private int getFV(BugTicket ticket, Version[] versions)
	{
		int versionIndex = Integer.MAX_VALUE;
		
		
		for(String fixedVersion : ticket.getFixedVersion())
			for(Version version : versions)
				if(version.getId().equals(fixedVersion))
					if(versionIndex > version.getIndex())
						versionIndex = version.getIndex();
				
			
		if(versionIndex == Integer.MAX_VALUE) return -1;
		return versionIndex;
	}
	
	private int getIV(BugTicket ticket, Version[] versions)
	{
		
		List<Integer> affectedVersionIndexes = new ArrayList<>();
		
		for(String av : ticket.getAffectedVersions())
			for(Version version : versions)
				if(version.getId().equals(av))
					affectedVersionIndexes.add(version.getIndex());
		
		if(affectedVersionIndexes.size() == 0)	
			return -1;
		
		
		
		int minAv = affectedVersionIndexes.get(0);
		int maxAv = affectedVersionIndexes.get(0);
		
		for(int av : affectedVersionIndexes) {
			if(minAv > av) minAv = av;
			if(maxAv < av) maxAv = av;
		}
		
		return minAv;
		
		
					
			
	}
	
	private int getOV(BugTicket ticket, Version[] versions) {
		for(Version version : versions)
			if(ticket.getDate().isBefore(version.getDate()))
				return version.getIndex();
		

		return -1;
	}

}
