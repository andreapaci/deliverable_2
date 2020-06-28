package logic.deliverable2;

import util.FileLogger;
import weka.core.Instances;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;


//Handler per il sampling
public class SamplingHandler {
	
	
	public SpreadSubsample undersampling(Instances dataset) {
		
		SpreadSubsample spreadSubsample = null;
		
		try {
			
			spreadSubsample = new SpreadSubsample();
			spreadSubsample.setInputFormat(dataset);
			
			
			String[] opts = new String[]{"-M", "1.0"};
			spreadSubsample.setOptions(opts);
			
		} 
		catch (Exception e) { FileLogger.getLogger().error("Errore nell'instanziazione dell'undersample"); System.exit(1); }
		
		return spreadSubsample;
		
	}
	public Resample oversampling(Instances dataset) {
		
		
		Resample resample = null;
		
		try {
		
			resample = new Resample();
			resample.setInputFormat(dataset);
			
			resample.setNoReplacement(false);
			resample.setBiasToUniformClass(1.0f);
			double percentMinority = getMinorityPercentage(dataset.size(), getDefectsNumber(dataset));
			resample.setSampleSizePercent((100 - percentMinority)*2);
			
			
		} 
		catch (Exception e) { FileLogger.getLogger().error("Errore nell'instanziazione dell'oversample"); System.exit(1); }
		
		return resample;
		
	}
	public SMOTE smote(Instances dataset) {
		
		SMOTE smote = null;
		
		try {
		
			smote = new SMOTE();
			smote.setInputFormat(dataset);
			
		} 
		catch (Exception e) { FileLogger.getLogger().error("Errore nell'instanziazione dello SMOTE"); System.exit(1); }
		
		return smote;
		
	}
	
	
	private double getMinorityPercentage(int size, int defects) {
		int minority = defects;
		if (size - defects < defects)
			minority = size-defects;
		
		return (double) minority/size * 100;
	}
	
	private int getDefectsNumber(Instances dataset) {
		
		int defects = 0;
		int defectTrueIndex = ControllerDeliverable2.getDefectiveClassIndex(dataset);
		
		for(int i = 0; i!= dataset.size(); i++) 
			if(dataset.get(i).value(dataset.classIndex()) == defectTrueIndex) 
				defects++;
		
	
		return defects;
	}
	
	
		

}
