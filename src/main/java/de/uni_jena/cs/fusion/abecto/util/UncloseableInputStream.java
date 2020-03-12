package de.uni_jena.cs.fusion.abecto.util;

import java.io.FilterInputStream;
import java.io.InputStream;

public class UncloseableInputStream extends FilterInputStream {

	public UncloseableInputStream(InputStream in) {
		super(in);
	}

	@Override
	public void close() {
	}
}
