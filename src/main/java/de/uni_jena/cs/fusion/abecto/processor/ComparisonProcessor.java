package de.uni_jena.cs.fusion.abecto.processor;

import com.google.common.collect.Streams;
import de.uni_jena.cs.fusion.abecto.Aspect;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.util.Set;
import java.util.stream.Stream;

public abstract class ComparisonProcessor<P extends Processor<P>> extends Processor<P> {

    /**
     * Returns a {@link Set} of all resource keys of a given aspect in a given
     * dataset.
     *
     * @param aspect        the {@link Aspect} the returned {@link Resource
     *                      Resources} belong to
     * @param dataset       the IRI of the source dataset of the {@link Resource
     *                      Resources}
     * @param datasetModels the (union of) {@link Model Model(s)} of the dataset
     *                      containing the {@link Resource Resources}
     * @return all resource keys of the given aspect in the given dataset
     *
     * @throws NullPointerException if no pattern is defined for the given dataset
     */
    public Stream<Resource> getResourceKeys(Aspect aspect, Resource dataset, Model datasetModels)
            throws NullPointerException {
        Query aspectQuery = aspect.getPattern(dataset);
        // copy query without result vars
        Query query = new Query();
        query.setQuerySelectType();
        query.setPrefixMapping(aspectQuery.getPrefixMapping());
        query.setBase(aspectQuery.getBase());
        query.setQueryPattern(aspectQuery.getQueryPattern());
        if (aspectQuery.getValuesData() != null) {
            query.setValuesDataBlock(aspectQuery.getValuesVariables(), aspectQuery.getValuesData());
        }
        query.setLimit(aspectQuery.getLimit());
        query.setOffset(aspectQuery.getOffset());
        if (aspectQuery.getOrderBy() != null) {
            aspectQuery.getOrderBy()
                    .forEach(sortCondition -> query.addOrderBy(sortCondition.expression, sortCondition.direction));
        }
        aspectQuery.getGroupBy().forEachVarExpr(query::addGroupBy);
        aspectQuery.getHavingExprs().forEach(query::addHavingCondition);
        // set var
        query.addResultVar(aspect.getKeyVariable());
        query.setDistinct(true);

        ResultSet results = QueryExecutionFactory.create(query, datasetModels).execSelect();

        String keyVariableName = aspect.getKeyVariableName();

        return Streams.stream(results).map(querySolution -> querySolution.getResource(keyVariableName));
    }
}
