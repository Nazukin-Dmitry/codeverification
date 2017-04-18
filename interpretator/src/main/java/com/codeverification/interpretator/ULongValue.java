package com.codeverification.interpretator;

import com.codeverification.compiler.DataType;

/**
 * Created by 1 on 18.04.2017.
 */
public class ULongValue implements Value {

    private Long value;

    @Override
    public DataType getType() {
        return DataType.ULONG;
    }

    @Override
    public String getRaw() {
        return null;
    }

    @Override
    public int compareTo(Value o) {
        return 0;
    }

    @Override
    public Long asULong() {
        return value;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }
}
