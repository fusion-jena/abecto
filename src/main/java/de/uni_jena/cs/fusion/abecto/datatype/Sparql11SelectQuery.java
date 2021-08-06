package de.uni_jena.cs.fusion.abecto.datatype;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.slf4j.LoggerFactory;

import de.uni_jena.cs.fusion.abecto.Abecto;

public class Sparql11SelectQuery extends BaseDatatype {

	/**
	 * @param uri the URI label to use for this datatype
	 */
	public Sparql11SelectQuery(String uri) {
		super(uri);
	}

	@Override
	public String unparse(Object value) {
		if (value instanceof Query) {
			return ((Query) value).toString();
		}
		throw new IllegalArgumentException(String.format("Value is not a %s.", Query.class.getCanonicalName()));
	}

	@Override
	public Query parse(String lexicalForm) throws DatatypeFormatException {
		try {
			Query query = QueryFactory.create(lexicalForm, Syntax.syntaxSPARQL_11);
			if (!query.isSelectType()) {
				throw new DatatypeFormatException("Not a SPARQL 1.1 Select query.");
			}
			return query;
		} catch (QueryException e) {
			// TODO add cause, remove log if https://github.com/apache/jena/pull/1044 done
			LoggerFactory.getLogger(Abecto.class).error("Not a valid SPARQL 1.1 query.", e);
			throw new DatatypeFormatException("Not a valid SPARQL 1.1 query.");
		}
	}

	@Override
	public boolean isValidLiteral(LiteralLabel lit) {
		return equals(lit.getDatatype()) && isValidValue(lit.getValue());
	}

	@Override
	public boolean isValidValue(Object valueForm) {
		return valueForm instanceof Query && ((Query) valueForm).isSelectType();
	}

	@Override
	public Class<?> getJavaClass() {
		return Query.class;
	}

}
