package de.uni_jena.cs.fusion.abecto.processor.refinement.meta;

import java.util.Map;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.jena.rdf.model.Model;

public class CountByCategoryAndRelationProcessor extends AbstractMetaProcessor {

	@Override
	public Map<String, TypeLiteral<?>> getPropertyTypes() {
		// TODO black- or whitelist
		return null;
	}

	@Override
	protected Model computeResultModel() throws Exception {
		// TODO get relation paths per KB
		// TODO get category IRIs per KB
		// TODO construct model containing count per relation path, IRI and KB
		return null;
	}

}
