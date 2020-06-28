package entity;

import logic.deliverable2.ControllerDeliverable2;
import weka.core.Instances;

//L'entià che modella il singolo walk nel Walk Forward
public class Walk {
	
	private int trainIndex;					//Indice del walk che indica quale è l'ultimo subset del dataset che viene usato per training
	private Instances trainSet;				//Set su cui fare training
	private Instances testSet;				//Set su cui fare testing
	private double percentTrainingDataset;	//Indica la percentuale su cui si fa training rispetto a tutto il dataset (#train_set/#dataset)
	private double percentDefectiveTrain;	//Percentuale di file defective sul training
	private double percentDefectiveTest;	//Percentuale di file defective sul testing
	
	public Walk(int trainIndex, Instances dataset) {
		this.trainIndex = trainIndex;
		parseDataset(dataset);
		
		this.percentDefectiveTrain = ((double) getDefectsNumber(this.trainSet))/((double) trainSet.numInstances());
		this.percentDefectiveTest = ((double) getDefectsNumber(this.testSet))/((double) testSet.numInstances());
				
	}
	
	
	//Metodo che si occupa di fare parsing del dataset per dividere tra Train e Test
	private void parseDataset(Instances dataset) {
		
		int endTrainIndex = 0;
		
		for(int i = 0; i < dataset.size(); i++) {
			if(dataset.get(i).value(0) > trainIndex) {
				endTrainIndex = i;
				trainSet = new Instances(dataset, 0, endTrainIndex);
				break;
			}
			
		}
		
		int testSetElements = 0;
		for(int i = endTrainIndex; i < dataset.size(); i++) {
			if(dataset.get(i).value(0) > trainIndex + 1) {
				break;
			}
			testSetElements++;
		}
		testSet = new Instances(dataset, endTrainIndex, testSetElements);
		
		this.percentTrainingDataset = (double) trainSet.size()/ (double) dataset.size();
		
		
	}
	
	private int getDefectsNumber(Instances dataset) {
		
		int defects = 0;
		int defectTrueIndex = ControllerDeliverable2.getDefectiveClassIndex(dataset);
		
		for(int i = 0; i!= dataset.size(); i++) 
			if(dataset.get(i).value(dataset.classIndex()) == defectTrueIndex) 
				defects++;
		
	
		return defects;
	}
	


	public int getTrainIndex() {
		return trainIndex;
	}


	public Instances getTrainSet() {
		return trainSet;
	}


	public Instances getTestSet() {
		return testSet;
	}


	public double getPercentTrainingDataset() {
		return percentTrainingDataset;
	}


	public double getPercentDefectiveTrain() {
		return percentDefectiveTrain;
	}


	public double getPercentDefectiveTest() {
		return percentDefectiveTest;
	}
	
	
	
	

}
