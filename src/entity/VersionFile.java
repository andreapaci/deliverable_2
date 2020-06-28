package entity;


public class VersionFile 
{
	private String filePath;		//Path del file
	private boolean defective;		//Metrica defective 		
	private int size; 				//Metrica LOC (Size)		
	private int locTouched;			//Metrica LOC Touched		
	private int revisionNumber;		//Metrica NR
	private int numberAuthor;		//Metrica NAuth				
	private int locAdded;			//Metrica LOC_added			
	private int locMax;				//Metrica MAX_LOC_added
	private float locAvg;			//Metrica AVG_LOC_added
	private int churn;				//Metrica Churn
	private int maxChurn;			//Metrica MAX_Churn
	private float avgChurn;			//Metrica AVG_Churn		
	private int age;				//Metrica Age (in settimane)
	private int chgSize;			//Metrica ChgSetSize
	private int maxChg;				//Metrica MAX_ChgSet
	private float avgChg;			//Metrica AVG_ChgSet
	
	
	
	public VersionFile(String filePath) {

		this.filePath = filePath;
		this.defective = false;
	}
	
	
	
	
	
	public void setSize(int size) {
		this.size = size;
	}
	
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public void setLocTouched(int locTouched) {
		this.locTouched = locTouched;
	}

	public void setRevisionNumber(int revisionNumber) {
		this.revisionNumber = revisionNumber;
	}

	public void setNumberAuthor(int numberAuthor) {
		this.numberAuthor = numberAuthor;
	}

	public void setLocAdded(int locAdded) {
		this.locAdded = locAdded;
	}

	public void setLocMax(int locMax) {
		this.locMax = locMax;
	}

	public void setLocAvg(float locAvg) {
		this.locAvg = locAvg;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public void setChgSize(int chgSize) {
		this.chgSize = chgSize;
	}

	public void setMaxChg(int maxChg) {
		this.maxChg = maxChg;
	}

	public void setAvgChg(float avgChg) {
		this.avgChg = avgChg;
	}
	
	public void setDefective(boolean defective) {
		this.defective = defective;
	}

	public void setChurn(int churn) {
		this.churn = churn;
	}

	public void setMaxChurn(int maxChurn) {
		this.maxChurn = maxChurn;
	}

	public void setAvgChurn(float avgChurn) {
		this.avgChurn = avgChurn;
	}





	public String getFilePath() {
		return filePath;
	}
	
	public int getSize() {
		return size;
	}
	
	public int getLocTouched() {
		return locTouched;
	}
	
	public int getRevisionNumber() {
		return revisionNumber;
	}
	
	public int getNumberAuthor() {
		return numberAuthor;
	}
	
	public int getLocAdded() {
		return locAdded;
	}
	
	public int getLocMax() {
		return locMax;
	}
	
	public float getLocAvg() {
		return locAvg;
	}
	
	public int getAge() {
		return age;
	}
	
	public int getChgSize() {
		return chgSize;
	}
	
	public int getMaxChg() {
		return maxChg;
	}
	
	public float getAvgChg() {
		return avgChg;
	}

	public boolean isDefective() {
		return defective;
	}
	
	public int getChurn() {
		return churn;
	}
	
	public int getMaxChurn() {
		return maxChurn;
	}

	public float getAvgChurn() {
		return avgChurn;
	}







	
	

}
