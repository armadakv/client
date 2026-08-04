/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.core.api.op;

import com.google.protobuf.ByteString;
import io.armadakv.core.api.ByteSequence;
import io.armadakv.core.api.Txn;
import mvcc.v1.Mvcc.Compare;

/**
 * Cmp target used in {@link Txn}.
 */
public abstract class CmpTarget<T> {

    /**
     * Cmp on the <i>value</i>.
     *
     * @param value the value to compare
     * @return the value compare target
     */
    public static ValueCmpTarget value(ByteSequence value) {
        return new ValueCmpTarget(ByteString.copyFrom(value.getBytes()));
    }

    private final Compare.CompareTarget target;
    private final T targetValue;

    protected CmpTarget(Compare.CompareTarget target, T targetValue) {
        this.target = target;
        this.targetValue = targetValue;
    }

    /**
     * Get the compare target used for this compare.
     *
     * @return the compare target used for this compare
     */
    public Compare.CompareTarget getTarget() {
        return target;
    }

    /**
     * Get the compare target value of this compare.
     *
     * @return the compare target value of this compare.
     */
    public T getTargetValue() {
        return targetValue;
    }


    public static final class ValueCmpTarget extends CmpTarget<ByteString> {

        ValueCmpTarget(ByteString targetValue) {
            super(Compare.CompareTarget.VALUE, targetValue);
        }
    }

}
