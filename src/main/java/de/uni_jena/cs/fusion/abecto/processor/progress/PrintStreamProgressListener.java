package de.uni_jena.cs.fusion.abecto.processor.progress;

import java.io.PrintStream;
import java.text.NumberFormat;

public class PrintStreamProgressListener implements ProgressListener {

	PrintStream out;
	NumberFormat percentage = NumberFormat.getPercentInstance();

	public PrintStreamProgressListener(PrintStream out) {
		this.out = out;
	}

	@Override
	public void onProgress(long current, long total) {
		this.out.println(percentage.format(((float)current)/total));
	}

	@Override
	public void onFailure(Throwable throwable) {
		this.out.println("FAILURE");
	}

	@Override
	public void onSuccess() {
		this.out.println("SUCCESS");
	}

}
