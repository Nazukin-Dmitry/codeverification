package com.codeverification.interpretator;

import com.codeverification.compiler.DataType;

/**
 * @author Dmitrii Nazukin
 */
public class StringValue implements Value {

    private String value;

    public StringValue() {
    }

    public StringValue(String value) {
        this.value = value;
    }

    @Override
    public DataType getType() {
        return DataType.STRING;
    }

    @Override
    public String getRaw() {
        return null;
    }

    @Override
    public int compareTo(Value value) {
        return 0;
    }

    @Override
    public String asString() {
        return value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
