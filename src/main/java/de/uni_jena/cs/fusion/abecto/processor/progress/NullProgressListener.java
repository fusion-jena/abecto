package de.uni_jena.cs.fusion.abecto.processor.progress;

public class NullProgressListener implements ProgressListener {

	private final static NullProgressListener NULL_PROGRESS_LISTENER = new NullProgressListener();

	private NullProgressListener() {
	}

	public static NullProgressListener get() {
		return NullProgressListener.NULL_PROGRESS_LISTENER;
	}

	@Override
	public void onProgress(long current, long total) {
		// do nothing
	}

	@Override
	public void onFailure(Throwable throwable) {
		// do nothing
	}

	@Override
	public void onSuccess() {
		// do nothing
	}

}
