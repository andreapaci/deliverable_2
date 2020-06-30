package logic.deliverable1;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry.ChangeType;

import entity.BugTicket;
import entity.Commit;
import entity.CommitFileOperation;
import entity.Version;
import entity.VersionFile;
import util.FileLogger;

public class DefectHandler 
{
	
	public void calculateDefect(Commit[] commits, BugTicket[] tickets, Version[] versions)
	{
		for(BugTicket ticket : tickets) {
			FileLogger.getLogger().info("Ticket: " + ticket.getTicketId());
			calculateTicketDefectClass(commits, ticket, versions);
			FileLogger.getLogger().info(" |\n +----------------------------------------------------------------------------------------------");
		}
	}
	
	private void calculateTicketDefectClass(Commit[] commits, BugTicket ticket, Version[] versions)
	{
		
		//Innanzitutto verifico che il ticket possa essere usato per calcolare la buggyness
		//Se non ho la Fixed Version, la calcolo prendendo la prima versione dopo l'ultimo commit del ticket
		
		
		//Trovo tutti i file MODFICATI ed ELIMINATI per il ticket specificato

		ArrayList<CommitFileOperation> fileOps = new ArrayList<>();
		findAllBuggyFiles(commits, ticket, fileOps);
		
		// Ho tutte le classi che sono buggate

		// Trovo le versioni affected e la fixed version

		int fv;
		int iv = -1;
		int ov;

		fv = ProportionHandler.getIstance().getFV(ticket, commits);
		ov = ProportionHandler.getIstance().getOV(ticket, versions);

		if (ticket.getAffectedVersions() != null) {

			iv = hasAffectedVersion(ticket, versions, ov, fv);

		} //Se invece non ha affected version 
		else 
		{
			FileLogger.getLogger().info(" |\tIl ticket non ha AV, computo col proportion.");

			FileLogger.getLogger().info(" |\tIl ticket non ha IV/OV/FV validi, calcolo la defectiveness ma non uso la Proportion per il calcolo di P incremental");
			
			if (isValidFvOv(ov, fv)) {
				FileLogger.getLogger().info(" |\tIl ticket non ha FV/OV valido, scarto il ticket");
			} 
			else 
			{
				iv = calculateIv(ov, fv);
			}

		}



		// Vado ad applicare la defectiveness ai file nelle versioni [iv, fv)
		if(iv != -1)
			applyDefectiveness(fileOps, commits, versions, iv, fv);
		
		
		
	}
	
	//Controlla se il ticket è valido
	private boolean isValidTicket(int iv, int ov, int fv) {
		return !(iv == -1 || ov == -1 || fv == -1) && !(fv < ov || ov < iv) && fv != ov;
	}
	
	private boolean isValidFvOv(int ov, int fv) {
		return fv == -1 || ov == -1;
	}
	
	
	//Calcola IV a partire dalla proporzione fornita
	private int calculateIv(int ov, int fv) {
		float proportion = ProportionHandler.getIstance().getProportion();
		return (int) Math.floor(fv - (fv - ov) * proportion);
	}
	
	private int hasAffectedVersion(BugTicket ticket, Version[] versions, int ov, int fv) {
		
		int iv = ProportionHandler.getIstance().getIV(ticket, versions);

		FileLogger.getLogger().info(" |\tIl ticket ha AV/FV: " + iv + "-" + ov + "-" + fv);

		// Verifico se i valori sono validi e se possono essere usati
		if (isValidTicket(iv, ov, fv)) {

			FileLogger.getLogger().info(" |\tIl ticket ha IV/OV/FV validi");

			calculateAndSaveProportion(iv, ov, fv);

		} 
		else 
		{
			FileLogger.getLogger().info(" |\tIl ticket non ha IV/OV/FV validi, calcolo la defectiveness ma non uso la Proportion per il calcolo di P incremental");
			
			if (isValidFvOv(ov, fv)) {
				FileLogger.getLogger().info(" |\tIl ticket non ha FV/OV valido, scarto il ticket");
			} 
			else 
			{	
				if (iv >= fv) {
					

					iv = calculateIv(ov, fv);
				}
			}
		}
		
		return iv;
	}
	
	//Calcola e salva nell'apposita istanza la proporzione
	private void calculateAndSaveProportion(int iv, int ov, int fv) {
		
		float proportion = ((float) (fv - iv) / (float) (fv - ov));

		FileLogger.getLogger().info(" |\tLa proporzione calcolata è P = " + proportion);

		// Se i valori sono validi, li aggiungo anche ad una lista gestita dal
		// ProportionHandler in modo da poter applicare Incremental
		ProportionHandler.getIstance().addProportion(proportion);
		
	}
	
	//Metodo che trova tutti i file toccati dalle commit del ticket
	private void findAllBuggyFiles(Commit[] commits, BugTicket ticket, ArrayList<CommitFileOperation> fileOps) {
		for (Commit commit : commits) {
			
			// Questo commit è relativo al ticket
			if (commit.getTicket().contains(ticket))
				for (CommitFileOperation fileOp : commit.getFileTouched())
					// Considero solamente i file modificati ed eliminati, quelli aggiunti, copiati
					// o rinominati, essendo aggiunte, non possono essere defective
					if (fileOp.getOpType() == ChangeType.MODIFY || fileOp.getOpType() == ChangeType.DELETE)
						fileOps.add(fileOp);
		}

		FileLogger.getLogger().info(" |\tTrovati tutti i file modificati/eliminati");

	}
	
	
	//Metodo che applica ai file riscontrati la defectiveness
	private void applyDefectiveness(ArrayList<CommitFileOperation> fileOps, Commit[] commits, Version[] versions, int iv, int fv) {
		
		// Lista di tutti i file tra [iv, fv) che sono defective
		List<String> alias = new ArrayList<>();

		for (CommitFileOperation fileOp : fileOps) {

			FileLogger.getLogger()
					.info(" |\tCalcolo presenza di file defective nelle versioni [" + iv + ", " + fv
							+ ") per la fileOP [" + fileOp.getOpType() + "]:" + fileOp.getOldPath() + ":::"
							+ fileOp.getFilePath());

			// Devo innanzitutto trovarmi tutti gli alias
			alias.addAll(findAlias(fileOp, commits, iv, fv));

		}

		for (Version version : versions) {

			FileLogger.getLogger().info(" |\tFile defective per versione[" + version.getIndex() + "]");
			if (version.getIndex() >= iv && version.getIndex() < fv) {
				for (VersionFile file : version.getFilesPath())
					// Se un file della versione è contenuto tra gli alias dei file
					// modificati/eliminati delle commit del ticket preso in analisi, allora è
					// defective
					if (alias.contains(file.getFilePath())) {

						FileLogger.getLogger().info(" |\t |\t" + file.getFilePath());
						file.setDefective(true);
					}
			}
		}
				
	}
	
	
	
	//Metodo per trovare Alias, sia andando avanti che a ritroso
	private List<String> findAlias(CommitFileOperation fileOp, Commit[] commits, int iv, int fv) {
		
		FileLogger.getLogger().info(" |\t |\tRicerca degli alias per la file operation [" + fileOp.getOpType() + "]:" + fileOp.getOldPath() + ":::" + fileOp.getFilePath());
		
		//Lista di filePath di alias a ritroso
		List<String> filesPathBackward = new ArrayList<>();
		//Lista di filePath di alias avanti
		List<String> filesPathForeward = new ArrayList<>();
				
		if(fileOp.getOpType() == ChangeType.DELETE) { filesPathBackward.add(fileOp.getOldPath()); filesPathForeward.add(fileOp.getOldPath()); }
		else { filesPathBackward.add(fileOp.getFilePath()); filesPathForeward.add(fileOp.getFilePath()); }
		
		//Prima andando a ritroso
		FileLogger.getLogger().info(" |\t |\tAndando a ritroso");
		findAliasBackward(commits, iv, fv, filesPathBackward);
				
		
		//Andando avanti (se ho un delete, non ha senso andare avanti)
		if(fileOp.getOpType() == ChangeType.MODIFY) {
			
			FileLogger.getLogger().info(" |\t |\tAndando avanti");
			findAliasForeward(commits, iv, fv, filesPathForeward);
		}
		
		FileLogger.getLogger().info(" |\tAlias trovati.");
		
		filesPathBackward.addAll(filesPathForeward);
		
		return filesPathBackward;
		
		
	}
	
	private void findAliasBackward(Commit[] commits, int iv, int fv, List<String> filesPathBackward) {
		

		for (int i = commits.length - 1; i >= 0; i--)
			if (commits[i].getVersion().getIndex() >= iv && commits[i].getVersion().getIndex() < fv)
				for (CommitFileOperation fileTouched : commits[i].getFileTouched())
					if ((fileTouched.getOpType() == ChangeType.RENAME || fileTouched.getOpType() == ChangeType.COPY) && (filesPathBackward.contains(fileTouched.getFilePath()) && !filesPathBackward.contains(fileTouched.getOldPath()))) {

						filesPathBackward.add(fileTouched.getOldPath());
						FileLogger.getLogger().info(" |\t |\t |\t" + fileTouched.getOldPath() + ":::" + fileTouched.getFilePath());

					}
	
	}
	
	private void findAliasForeward(Commit[] commits, int iv, int fv, List<String> filesPathForeward) {
		
			
		for (int i = 0; i < commits.length; i++)
			if (commits[i].getVersion().getIndex() >= iv && commits[i].getVersion().getIndex() < fv)
				for (CommitFileOperation fileTouched : commits[i].getFileTouched())
					if ((fileTouched.getOpType() == ChangeType.RENAME || fileTouched.getOpType() == ChangeType.COPY)
							&& (!filesPathForeward.contains(fileTouched.getFilePath())
									&& filesPathForeward.contains(fileTouched.getOldPath()))) {

						filesPathForeward.add(fileTouched.getFilePath());
						FileLogger.getLogger()
								.info(" |\t |\t |\t" + fileTouched.getOldPath() + ":::" + fileTouched.getFilePath());

						}
				
			
	}
	
	
	
	
	
}
