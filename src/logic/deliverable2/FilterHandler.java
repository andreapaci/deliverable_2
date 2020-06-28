package logic.deliverable2;

import util.FileLogger;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.WrapperSubsetEval;
import weka.core.Instances;
import weka.filters.Filter;

public class FilterHandler {
	
	//Evaluator Correlation based FS (Filter Method)
	public Filter cfsEvaluator(Instances dataset) {
		
		AttributeSelection filter = new AttributeSelection();
		
		CfsSubsetEval eval = new CfsSubsetEval();
		try 
		{ 
			eval.buildEvaluator(dataset);
			
		    BestFirst search = new BestFirst();
		    
			filter.setEvaluator(eval);
			filter.setSearch(search);
			
			filter.setInputFormat(dataset); 
		} 
		catch (Exception e) { FileLogger.getLogger().error("Errore nell'instanziazione della feature selection CFS con Best First"); }
		
		
		return filter;
	}
	
	//Evaluator Wrapper FS
	public Filter wrapperEvaluator(Instances dataset) {
			
		AttributeSelection filter = new AttributeSelection();
		
		try {
			
			WrapperSubsetEval eval = new WrapperSubsetEval();
			
			String[] wrapperEvaluatorOpt = {"-B", "weka.classifiers.bayes.NaiveBayes","-F","5","-T","0.01","-R","1"};
			eval.setOptions(wrapperEvaluatorOpt);
			
			eval.buildEvaluator(dataset);
		    
			BestFirst search = new BestFirst();
			String [] bfSearchOpt = {"-D", "2",  "-N", "5"};
			search.setOptions(bfSearchOpt);
			
			filter.setEvaluator(eval);
			filter.setSearch(search);
				
			filter.setInputFormat(dataset); 
		} 
		catch (Exception e) { FileLogger.getLogger().error("Errore nell'instanziazione della feature selection Wrapper con Best First"); }
			
			
		return filter;
	}
	
	

}
