package entity;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry.ChangeType;

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
	
	public String printCommitValues(boolean printFiles) {
		String print;
		print = "Commit: " + this.date.toString() + "\n" + this.message + "\n";
		print += "Versione: " + version.getVersionName() + "\n";
		print += " |\tTicket relativi: \n";
		if(this.tickets != null)
			for(BugTicket ticket : this.tickets)
				print += " |\t |\tTicket: " + ticket.getTicketId() + "\n";
		else
			print += " |\t |\tNessun ticket relativo\n";
		if(printFiles) {
			
			print += " |\tFile toccati: \n";
		
			if(this.fileTouched != null)
				for(CommitFileOperation fileTouched : this.fileTouched){	
					if(fileTouched.getOpType() == ChangeType.DELETE)
						print += " |\t |\tFile: [" + fileTouched.getOpType().toString() + "]:" + fileTouched.getOldPath() + "\n";
					else if(fileTouched.getOpType() == ChangeType.RENAME)
						print += " |\t |\tFile: [" + fileTouched.getOpType().toString() + "]:" + fileTouched.getFilePath() + "\n |\t |\t\t From: " + fileTouched.getOldPath() + "\n";
					else
						print += " |\t |\tFile: [" + fileTouched.getOpType().toString() + "]:" + fileTouched.getFilePath() + "\n";
					
					print += " |\t |\tAuthor: " + fileTouched.getAuthor() + "\n";
					print += " |\t |\tLoc Touched: " + fileTouched.getLocTouched() + "\n";
					print += " |\t |\tLoc Added: " + fileTouched.getLocAdded() + "\n";
				}
			else
				print += " |\t |\tNessun file relativo\n";
		
		}
		print += " +-----------------------------------------------------------------------------\n\n";
		
		return print;
	}



}
