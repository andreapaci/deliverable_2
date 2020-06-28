package entity;

import java.time.LocalDate;

//Classe che memorizza informazioni sui ticket, incluso di AV e FV
public class BugTicket
{
	private String ticketId;
	private LocalDate date;
	private String openingVersion;
	private String[] affectedVersions;
	private String[] fixedVersions;
	
	
	public BugTicket(String ticketId, String[] affectedVersions, String[] fixedVersion, LocalDate date) {
		this.ticketId = ticketId;
		this.affectedVersions = affectedVersions;
		this.fixedVersions = fixedVersion;
		this.date = date;
		this.openingVersion = null;
	}

	public boolean hasAVFV() {
		return (affectedVersions != null && fixedVersions != null);
	}
	
	public String getTicketId() {
		return ticketId;
	}
	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}
	public String getOpeningVersion() {
		return openingVersion;
	}
	public void setOpeningVersion(String openingVersion) {
		this.openingVersion = openingVersion;
	}
	public String[] getAffectedVersions() {
		return affectedVersions;
	}
	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public void setAffectedVersions(String[] affectedVersions) {
		this.affectedVersions = affectedVersions;
	}
	public String[] getFixedVersion() {
		return fixedVersions;
	}
	public void setFixedVersion(String[] fixedVersion) {
		this.fixedVersions = fixedVersion;
	}


	
	

}
