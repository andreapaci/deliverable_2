package logic.deliverable2;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import entity.ModelMetrics;
import entity.Walk;
import util.FileLogger;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.unsupervised.attribute.Normalize;

public class ControllerDeliverable2 {
	
	private static ControllerDeliverable2 istance = null;
	private String projName;
	private String outputFolder = "output/deliverable_2/";
	private List<ModelMetrics> modelMetrics;	//Risusltati dei modelli
		
	private ControllerDeliverable2() {}
	
	
	
	public void run (String projName) {
		
		List<Walk> walks = new ArrayList<>();
		modelMetrics = new ArrayList<>();
		this.projName = projName;
		
		try 
		{
			//Caricamento e preprocessamento del dataset
			Instances dataset = loadFile();
			dataset = preprocess(dataset);
			int lastWalk = (int) dataset.get(dataset.size()-1).value(0);
			
			//Instanziazione dei vari walk
			for (int i = 1; i < lastWalk; i++)
				walks.add(new Walk(i, dataset));
			
			
		}
		catch (Exception e) {FileLogger.getLogger().warning("Errore nel caricamento ed esportazione del file dataset");}
		
		for(Walk walk : walks) {
			evaluateWalk(walk);
		}
		
		FileLogger.getLogger().info("\n\nGenerazione del file CSV\n");
		
		generateCSV(modelMetrics);
		

		FileLogger.getLogger().info("\n\nFile CSV generato\n");
		
	}
	

	
	private Instances loadFile() {
		
		Instances dataset = null;
		CSVLoader loader;
		ArffSaver saver; 
		
		//Load del file CSV
	    loader = new CSVLoader();
	    try { 
	    	loader.setSource(new File("output/deliverable_1/" + projName + "_metrics.csv")); 
		    dataset = loader.getDataSet();
		} 
	    catch (IOException e) { FileLogger.getLogger().error("Errore nel caricamento del file CSV"); System.exit(1); }
	    

	    //Esportazione del file in formato ARFF
	    saver = new ArffSaver();
	    saver.setInstances(dataset);
	    
	    try {
	    	saver.setFile(new File(outputFolder + projName + "_metrics.arff"));
	    	saver.writeBatch(); 
	    } 
	    catch (IOException e) { FileLogger.getLogger().warning("Errore nella scrittura del file .arff"); }
	    
		
		return dataset;
	}
	
	
	private void evaluateWalk(Walk walk) {
		
		
		//Scelta del classificatore
		String[] classifierNames = {"Random Forest", "IBk", "Naive Bayes"};
		
		
		AbstractClassifier classifier = null;
		
		for(int classifierIndex = 0; classifierIndex < classifierNames.length; classifierIndex++) {
			
			switch(classifierIndex) {
				
				case 0: //Random Forest
					classifier = new RandomForest();
				break;
				
				case 1: //IBk
					classifier = new IBk();
				break;
				
				case 2: //Naive Bayes
					classifier = new NaiveBayes();
				break;
				
				default:
					FileLogger.getLogger().error("Errore nella selezione del classifier");
					System.exit(1);
				break;
				
			}
			
			
			chooseFeatureSelection(classifier, 
					classifierNames[classifierIndex], walk);
			
			
		}
		
		
	}
	
	private void chooseFeatureSelection(AbstractClassifier classifier, 
			String classifierName, Walk walk) {
		
		
		//Scelta delle features
		String[] featureNames = {"No selection", "CFS con Best First", "Wrapper con Best First"};
				
		
		FilterHandler filterHandler = new FilterHandler();
		
		for(int featureIndex = 0; featureIndex < featureNames.length; featureIndex++) {
			
			Filter filter = null;
			Instances filteredTrain = null;
			Instances filteredTest = null;
			
			switch(featureIndex) {
					
				case 0: //No selection
				break;
					
				case 1: //CFS con Best First
					filter = filterHandler.cfsEvaluator(walk.getTrainSet());
				break;
					
				case 2: //Wrapper con Best First
					filter = filterHandler.wrapperEvaluator(walk.getTrainSet());
				break;
				
				default:
					FileLogger.getLogger().error("Errore nella selezione della feature selection");
					System.exit(1);
				break;
				
					
			}
			
			if(filter != null) {
				try 
				{
					filteredTrain = Filter.useFilter(walk.getTrainSet(), filter);
					filteredTest = Filter.useFilter(walk.getTestSet(), filter);
				} 
				catch (Exception e) { FileLogger.getLogger().error("Errore nell'applicazione del filtro al Train/Test set"); System.exit(1); }
				filteredTrain.setClassIndex(filteredTrain.numAttributes() - 1);
				filteredTest.setClassIndex(filteredTest.numAttributes() - 1);
			}
			
			Instances trainSet;
			Instances testSet;
			if(filteredTrain == null) trainSet = walk.getTrainSet();
			else trainSet = filteredTrain;
			
			if(filteredTest == null) testSet = walk.getTestSet();
			else testSet = filteredTest;
			
			
			chooseSamplingSelection(classifier, 
					classifierName, featureNames[featureIndex], walk, trainSet, testSet);
			
		}
		
	}
	
	private void chooseSamplingSelection(AbstractClassifier classifier, 
			String classifierName, String featureName, Walk walk, Instances trainSet, Instances testSet) {
		
		//Scelta del sampling
		String[] balancingNames = {"No sampling", "Oversampling", "Undersampling", "SMOTE"};

		SamplingHandler samplingHandler = new SamplingHandler();
		
		for(int balancingIndex = 0; balancingIndex < balancingNames.length; balancingIndex++) {
			
			if(checkDatasetValidity(trainSet)) break;
			
			FilteredClassifier filteredClassifier = null;
			
			switch(balancingIndex) {
				
				case 1: //Oversampling
					Resample resample = samplingHandler.oversampling(trainSet);
					
					filteredClassifier = new FilteredClassifier();
					filteredClassifier.setClassifier(classifier);
					filteredClassifier.setFilter(resample);
					
					try { trainSet = Filter.useFilter(trainSet, resample);} 
					catch (Exception e1) { FileLogger.getLogger().error("Errore nell'applicazione di Oversampling");}
					
				break;
					
				case 2: //Undersampling
					
					SpreadSubsample spreadSubsample = samplingHandler.undersampling(trainSet);
					
					filteredClassifier = new FilteredClassifier();
					filteredClassifier.setClassifier(classifier);
					filteredClassifier.setFilter(spreadSubsample);
					
					try { trainSet = Filter.useFilter(trainSet, spreadSubsample);} 
					catch (Exception e1) { FileLogger.getLogger().error("Errore nell'applicazione di Undersampling");}
					
				break;
					
				case 3: //SMOTE
					SMOTE smote = samplingHandler.smote(trainSet);
					
					filteredClassifier = new FilteredClassifier();
					filteredClassifier.setClassifier(classifier);
					filteredClassifier.setFilter(smote);
					
					try { trainSet = Filter.useFilter(trainSet, smote);} 
					catch (Exception e1) { FileLogger.getLogger().error("Errore nell'applicazione di SMOTE");}
				break;
				
				default: //No sampling
				break;
				
				
			}
			
			Instances[] dataSet = {trainSet, testSet};
			
			evaluate(classifier, filteredClassifier,
					classifierName, featureName, balancingNames[balancingIndex], walk, dataSet);
			
		}
	}
	
	
	
	
	private void evaluate(AbstractClassifier classifier, FilteredClassifier filteredClassifier,
			String classifierName, String featureName, String balancingName, Walk walk, Instances[] dataSet) {
		
		Instances trainSet = dataSet[0];
		Instances testSet = dataSet[1];
		
		Evaluation evaluation = null; 
		try { evaluation = new Evaluation(testSet); }
		catch (Exception e) { FileLogger.getLogger().error("Errore nella inizializzazione dell' Evaluator"); System.exit(1);}
		
		int classIndex = getDefectiveClassIndex(trainSet);
		
		if(filteredClassifier != null)
		{
			
			try 
			{ 

				filteredClassifier.buildClassifier(trainSet); 
				evaluation.evaluateModel(filteredClassifier, testSet);
				
				
				
				printEvaluation(trainSet, testSet, walk, evaluation, classifierName, balancingName, featureName);
				
				
			}
			catch (Exception e) { FileLogger.getLogger().error("Errore nel build del classificatore con filtro"); System.exit(1);}
		}
		else 
		{
			
			try 
			{ 
				classifier.buildClassifier(trainSet); 
				
				evaluation.evaluateModel(classifier, testSet);
				
				printEvaluation(null, null, walk, evaluation, classifierName, balancingName, featureName);
				
			}
			catch (Exception e) { FileLogger.getLogger().error("Errore nel build del classificatore senza filtro"); System.exit(1);}
		
		}
		
		if(!Double.isNaN(evaluation.precision(classIndex)) && !Double.isNaN(evaluation.recall(classIndex)) && !Double.isNaN(evaluation.areaUnderROC(classIndex)) &&
				!Double.isNaN(evaluation.kappa())) {
			FileLogger.getLogger().info("Walk valido");
			
			String[] attributes = {classifierName, balancingName, featureName};
			double[] percents = {walk.getPercentTrainingDataset(), walk.getPercentDefectiveTrain(), walk.getPercentDefectiveTest()};
			int[] trueFalsePositiveNegative = {(int) evaluation.numTruePositives(classIndex), (int) evaluation.numTrueNegatives(classIndex), 
					(int) evaluation.numFalsePositives(classIndex), (int) evaluation.numFalseNegatives(classIndex)};
			double[] metrics = {evaluation.precision(classIndex), evaluation.recall(classIndex), evaluation.areaUnderROC(classIndex), evaluation.kappa()};
			
			modelMetrics.add(new ModelMetrics(projName, attributes, walk.getTrainIndex(),percents , trueFalsePositiveNegative, metrics));
		
		}
		else FileLogger.getLogger().info("Walk non valido");
		
		FileLogger.getLogger().info("\n\n");
	}
	
	
	
	private boolean checkDatasetValidity(Instances trainSet) {
		return (trainSet.numAttributes() <= 1 || (trainSet.numAttributes() <= 2  && trainSet.attribute(0).name().equals("Version")));
	}
	
	
	
	
	
	private void printEvaluation(Instances train, Instances test, Walk walk, Evaluation eval, String classifier, String balancing, String feature) {
		
		int trueDefectiveIndex;
		if(train == null) trueDefectiveIndex = getDefectiveClassIndex(walk.getTrainSet());
		else trueDefectiveIndex = getDefectiveClassIndex(train);
		
		FileLogger.getLogger().info("Walk #" + walk.getTrainIndex());
		FileLogger.getLogger().info(" +-Defective \"true\" index: " + trueDefectiveIndex);
		FileLogger.getLogger().info(" +-Number of features: " + walk.getTrainSet().numAttributes());
		if(train != null)
		{
			FileLogger.getLogger().info(" +-Number of filtered features: " + train.numAttributes());
			String newFeatures = "";
			StringBuilder sb = new StringBuilder(newFeatures);
		
			for(int i = 0; i < train.numAttributes(); i++)
				sb.append(train.attribute(i).name() + "; ");
			
			FileLogger.getLogger().info(" +----Features: " + sb.toString());	
			FileLogger.getLogger().info(" +-Filtered Class Index: " + test.classAttribute().name() + " [" + test.classIndex() + "]");
			
		}
		FileLogger.getLogger().info(" +-Class Index: " + walk.getTestSet().classAttribute().name() + " [" + walk.getTestSet().classIndex() + "]");
		FileLogger.getLogger().info(" +-Classifier: " + classifier);
		FileLogger.getLogger().info(" +-Balancing: " + balancing);
		FileLogger.getLogger().info(" +-Feature: " + feature);
		FileLogger.getLogger().info(" +-Evaluation results:");
		FileLogger.getLogger().info(" |");
		FileLogger.getLogger().info(" +----Precision: " + eval.precision(trueDefectiveIndex));
		FileLogger.getLogger().info(" |");
		FileLogger.getLogger().info(" +----TP: " + eval.numTruePositives(trueDefectiveIndex) + " FP: " + eval.numFalsePositives(trueDefectiveIndex));
		FileLogger.getLogger().info(" |");
		FileLogger.getLogger().info(" +----TN: " + eval.numTrueNegatives(trueDefectiveIndex) + " FN: " + eval.numFalseNegatives(trueDefectiveIndex));
		FileLogger.getLogger().info(" |");
		FileLogger.getLogger().info(" +----Area under ROC: " + eval.areaUnderROC(trueDefectiveIndex));
		FileLogger.getLogger().info(" |");
		FileLogger.getLogger().info(" +----Kappa: " + eval.kappa());
		FileLogger.getLogger().info(" |");
		FileLogger.getLogger().info(" +----Error rate: " + eval.errorRate());
		
		
		
		
		
	}
	
	//Fase di preprocessamento
	private Instances preprocess(Instances dataset) {
		
		//Elimino l'attributo relativo al filename poichè non fornisce informazioni aggiuntive 
		// ma può addirittura condizionare i risultati
		dataset.deleteAttributeAt(1);
		
		
		//Normalizzo il dataset per far si che tutti gli attributi hanno lo stesso peso sul train del modello
		Normalize norm = new Normalize();
		
		try {
			
			//E' necessario impostare il classindex a 0 per evitare di normalizzare anche il numero di versione
			dataset.setClassIndex(0);
			norm.setInputFormat(dataset);
			
			dataset = Filter.useFilter(dataset, norm);
			
			dataset.setClassIndex(dataset.numAttributes() - 1);
			
			return dataset;
		}
		catch (Exception e) {
			FileLogger.getLogger().error("Errore nella normalizzazione del dataset"); 
			System.exit(1);
		}
		
		return null;
		
	}
	
	
	//Metodo che ritorna il valore di indice per la Defective impostata su "true"
	public static int getDefectiveClassIndex(Instances dataset) {
		return dataset.attribute(dataset.numAttributes() - 1).indexOfValue("true");
	}
	
	
	
	private void generateCSV(List<ModelMetrics> modelMetrics) 
	{

		// Elimino il file se già esistente
		try {
			Files.delete(Paths.get(outputFolder + projName + "_model.csv"));
		} catch (IOException e1) {

			FileLogger.getLogger().warning("Errore nell'eliminazione del file .csv");
		}

		try (BufferedWriter br = new BufferedWriter(
				new FileWriter(outputFolder + projName + "_model.csv"))) {
			StringBuilder sb = new StringBuilder();

			sb.append("Dataset,Training_Release,%Training,%Defective_training,%Defective_testing,Classifier,Balancing,Feature_Selection,TP,FP,TN,FN,Precision,Recall,ROC_Area,Kappa\n");

			for(ModelMetrics modelMetric : modelMetrics) {
				sb.append(modelMetric.getDataset());
				sb.append(",");
				sb.append(modelMetric.getTrainingReleaseNumber());
				sb.append(",");
				sb.append(modelMetric.getTrainingPercent());
				sb.append(",");
				sb.append(modelMetric.getDefectTrainingPercent());
				sb.append(",");
				sb.append(modelMetric.getDefectTestPercent());
				sb.append(",");
				sb.append(modelMetric.getClassifier());
				sb.append(",");
				sb.append(modelMetric.getBalancing());
				sb.append(",");
				sb.append(modelMetric.getFeatureSelection());
				sb.append(",");
				sb.append(modelMetric.getTp());
				sb.append(",");
				sb.append(modelMetric.getFp());
				sb.append(",");
				sb.append(modelMetric.getTn());
				sb.append(",");
				sb.append(modelMetric.getFn());
				sb.append(",");
				sb.append(modelMetric.getPrecision());
				sb.append(",");
				sb.append(modelMetric.getRecall());
				sb.append(",");
				sb.append(modelMetric.getRocArea());
				sb.append(",");
				sb.append(modelMetric.getKappa());
				sb.append("\n");
				
				
			}

			br.write(sb.toString());
		} catch (Exception e) {

			FileLogger.getLogger().warning("Errore nella scrittura del file .csv");
		}

	}
	
	
	public static ControllerDeliverable2 getIstance() {
		if(istance == null) istance = new ControllerDeliverable2();
		return istance;
	}
	
}
