package com.opcua_arrow.config;

import java.util.Map;

public class FilterConfig {

    final private String name;
    final private Map<String, Object> parameters;
    final private boolean isNumeric;

    public FilterConfig(String name, Map<String, Object> parameters, boolean isNumeric) {
        this.name = name;
        this.parameters = parameters;
        this.isNumeric = isNumeric;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public boolean isNumeric() {
        return isNumeric;
    }

}
