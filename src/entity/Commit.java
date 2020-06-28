package entity;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;


//Classe che tiene conto dell'ID del ticket e la data del commit
public class Commit 
{
	private String message;					//Testo del commit
	private List<BugTicket> tickets;		//Ticket del bug collegato (null se commit normale)
	private Date date;						//Data del commit
	private Version version;				//Versione a cui si riferisce il commit
	private List<CommitFileOperation>  		//Indica su quali file è stata effettuata una modifica e cosa è stato fatto
			fileTouched;
	
	
	
	//Costruttore per commit
	public Commit(String message, List<BugTicket> tickets, Date date, Version version, List<CommitFileOperation> fileTouched)
	{
		this.message = message;
		this.version = version;
		this.fileTouched = fileTouched;
		this.tickets = tickets;
		this.date = new Date(date.getTime());

	}

	
	//Serve per estrapolare il mese dalla data (numero da 1 a 12)
	public int getMonth() {
		LocalDate localDate = this.date.toInstant().atZone(ZoneId.of("Z")).toLocalDate();
		return localDate.getMonthValue();
	}
	
	//Serve per estrapolare l'anno della data (formato yyyy)
	public int getYear() {
		LocalDate localDate = this.date.toInstant().atZone(ZoneId.of("Z")).toLocalDate();
		return localDate.getYear();
	}
	
	public List<BugTicket> getTicket() {
		return tickets;
	}


	public Date getDate() {
		return date;
	}
	

	public Version getVersion() {
		return version;
	}
	

	public List<CommitFileOperation> getFileTouched() {
		return fileTouched;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public String printCommitValues() {
		String print = "Commit: " + this.date.toString() + "\n" + this.message + "\n";
		StringBuilder sb = new StringBuilder(print);

		sb.append("Versione: " + version.getVersionName() + "\n");
		sb.append(" |\tTicket relativi: \n");
		if(this.tickets != null)
			for(BugTicket ticket : this.tickets)
				sb.append(" |\t |\tTicket: " + ticket.getTicketId() + "\n");
		else
			sb.append(" |\t |\tNessun ticket relativo\n");
		sb.append(" +-----------------------------------------------------------------------------\n\n");
		
		return sb.toString();
	}



}
