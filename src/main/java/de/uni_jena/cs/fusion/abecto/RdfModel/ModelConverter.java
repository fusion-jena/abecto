package de.uni_jena.cs.fusion.abecto.RdfModel;

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

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.HDTGraph;

@Converter
public class ModelConverter implements AttributeConverter<Model, byte[]> {

	@Override
	public byte[] convertToDatabaseColumn(Model model) {
		Graph graph = model.getGraph();
		if (graph instanceof HDTGraph) {
			try (ByteArrayOutputStream compressedOut = new ByteArrayOutputStream()) {
				try (OutputStream out = new GZIPOutputStream(compressedOut)) {
					((HDTGraph) graph).getHDT().saveToHDT(out, null);
				}
				return compressedOut.toByteArray();
			} catch (IOException e) {
				throw new RuntimeException("Failed to write HDTGraph of provided Model into OutputStream.", e);
			}
		} else {
			throw new IllegalArgumentException("Model is not presenting a HDTGraph.");
		}
	}

	@Override
	public Model convertToEntityAttribute(byte[] bytes) {
		try (InputStream in = new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)))) {
			HDT hdt = HDTManager.loadIndexedHDT(in);
			HDTGraph hdtGraph = new HDTGraph(hdt);
			return ModelFactory.createModelForGraph(hdtGraph);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read HDT from provided InputStream.", e);
		}
	}

}
