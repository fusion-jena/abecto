package de.uni_jena.cs.fusion.abecto.progress;

public interface ProgressListener {
	
	public void onProgress(long current, long total);
	
	public void onFailure(Throwable throwable);
	
	public void onSuccess();

}
