/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.data.query;

import org.springframework.data.domain.Sort;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.CollectionUtils;

import java.util.Iterator;

public class ArmadaQueryCreator extends AbstractQueryCreator<KeyValueQuery<ArmadaOperationChain>, ArmadaOperationChain> {

    public ArmadaQueryCreator(PartTree tree) {
        super(tree);
    }

    public ArmadaQueryCreator(PartTree tree, ParameterAccessor parameters) {
        super(tree, parameters);
    }

    @Override
    protected ArmadaOperationChain create(Part part, Iterator<Object> iterator) {
        return from(part, iterator, new ArmadaOperationChain());
    }

    private ArmadaOperationChain from(Part part, Iterator<Object> iterator, ArmadaOperationChain sink) {

        switch (part.getType()) {
            case SIMPLE_PROPERTY -> sink.sismember(part.getProperty().toDotPath(), iterator.next());
            case TRUE -> sink.sismember(part.getProperty().toDotPath(), true);
            case FALSE -> sink.sismember(part.getProperty().toDotPath(), false);
            default -> {
                String message = String.format("%s is not supported for Armada query derivation", part.getType());
                throw new IllegalArgumentException(message);
            }
        }

        return sink;
    }

    @Override
    protected ArmadaOperationChain and(Part part, ArmadaOperationChain base, Iterator<Object> iterator) {
        return from(part, iterator, base);
    }

    @Override
    protected ArmadaOperationChain or(ArmadaOperationChain base, ArmadaOperationChain criteria) {
        base.orSismember(criteria.getSismember());
        return base;
    }

    @Override
    protected KeyValueQuery<ArmadaOperationChain> complete(ArmadaOperationChain criteria, Sort sort) {
        KeyValueQuery<ArmadaOperationChain> query = new KeyValueQuery<>(criteria);

        if (query.getCriteria() != null && !CollectionUtils.isEmpty(query.getCriteria().getSismember())
                && !CollectionUtils.isEmpty(query.getCriteria().getOrSismember()))
            if (query.getCriteria().getSismember().size() == 1 && query.getCriteria().getOrSismember().size() == 1) {

                query.getCriteria().getOrSismember().add(query.getCriteria().getSismember().iterator().next());
                query.getCriteria().getSismember().clear();
            }

        if (sort.isSorted()) {
            query.setSort(sort);
        }

        return query;
    }

}
