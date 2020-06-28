package entity;

//Entità usata per memorizzare i risultati delle varie evaluation su di un modello di ML
public class ModelMetrics {
	
	private String dataset;
	private String classifier;
	private String balancing;
	private String featureSelection;
	private int trainingReleaseNumber;
	private double trainingPercent;
	private double defectTrainingPercent;
	private double defectTestPercent;
	private int tp; 
	private int tn;
	private int fp; 
	private int fn;
	private double precision;
	private double recall;
	private double rocArea;
	private double kappa;

	//Il costruttore è stato fatto usando array come parametri in input per rispettare il vincolo sul numero di parametri in ingresso di una funzione (<= 7)
	public ModelMetrics(String dataset, String[] classBalaFeatu, int trainingReleaseNumber, double[] trainPercDefTrainDefTest, int[] tpTnFpFn, double[] precRecRocKap) {
		
		this.dataset = dataset;
		this.classifier = classBalaFeatu[0];
		this.balancing = classBalaFeatu[1];
		this.featureSelection = classBalaFeatu[2];
		this.trainingReleaseNumber = trainingReleaseNumber;
		this.trainingPercent = trainPercDefTrainDefTest[0];
		this.defectTrainingPercent = trainPercDefTrainDefTest[1];
		this.defectTestPercent = trainPercDefTrainDefTest[2];
		this.tp = tpTnFpFn[0];
		this.tn = tpTnFpFn[1];
		this.fp = tpTnFpFn[2];
		this.fn = tpTnFpFn[3];
		this.precision = precRecRocKap[0];
		this.recall = precRecRocKap[1];
		this.rocArea = precRecRocKap[2];
		this.kappa = precRecRocKap[3];
	}

	public String getDataset() {
		return dataset;
	}

	public String getClassifier() {
		return classifier;
	}

	public String getBalancing() {
		return balancing;
	}

	public String getFeatureSelection() {
		return featureSelection;
	}

	public int getTrainingReleaseNumber() {
		return trainingReleaseNumber;
	}

	public double getTrainingPercent() {
		return trainingPercent;
	}

	public double getDefectTrainingPercent() {
		return defectTrainingPercent;
	}

	public double getDefectTestPercent() {
		return defectTestPercent;
	}

	public int getTp() {
		return tp;
	}

	public int getTn() {
		return tn;
	}

	public int getFp() {
		return fp;
	}

	public int getFn() {
		return fn;
	}

	public double getPrecision() {
		return precision;
	}

	public double getRecall() {
		return recall;
	}

	public double getRocArea() {
		return rocArea;
	}

	public double getKappa() {
		return kappa;
	}
	
	
	
	
}
