package boundary;

import logic.deliverable1.ControllerDeliverable1;
import logic.deliverable2.ControllerDeliverable2;

//Punto di accesso dell'applicazione
public class Main {
	
	
	public static void main(String[] args) 
	{
		ControllerDeliverable1 controllerDel1 = ControllerDeliverable1.getIstance();
		ControllerDeliverable2 controllerDel2 = ControllerDeliverable2.getIstance();
		
		//Calcolo delle metriche e della defectiveness come specificato dalla prima deliverable
		controllerDel1.run("BOOKKEEPER", "https://github.com/apache/bookkeeper");
		controllerDel1.run("SYNCOPE", "https://github.com/apache/syncope");
		
		//Applicazione di modelli di ML come specificato nella seconda deliverable
		controllerDel2.run("BOOKKEEPER");
		controllerDel2.run("SYNCOPE");
		
		
		
		
		
	}
	
	
	

}