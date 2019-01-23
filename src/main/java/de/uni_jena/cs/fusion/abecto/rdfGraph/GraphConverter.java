package de.uni_jena.cs.fusion.abecto.rdfGraph;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.HDTGraph;

@Converter
public class GraphConverter implements AttributeConverter<HDTGraph, byte[]> {

	@Override
	public byte[] convertToDatabaseColumn(HDTGraph graph) {
		try (ByteArrayOutputStream compressedOut = new ByteArrayOutputStream()) {
			try (OutputStream out = new GZIPOutputStream(compressedOut)) {
				((HDTGraph) graph).getHDT().saveToHDT(out, null);
			}
			return compressedOut.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("Failed to write HDTGraph of provided Model into OutputStream.", e);
		}
	}

	@Override
	public HDTGraph convertToEntityAttribute(byte[] bytes) {
		try (InputStream in = new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)))) {
			HDT hdt = HDTManager.loadHDT(in);
			HDTGraph hdtGraph = new HDTGraph(hdt);
			return hdtGraph;
		} catch (IOException e) {
			throw new RuntimeException("Failed to read HDT from provided InputStream.", e);
		}
	}

}
