package com.burpgraphql.schema.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ArgDef {
    public String name;
    public String description;
    public TypeRef type;
    public String defaultValue;

    public boolean isRequired() {
        return type != null && type.isRequired();
    }

    public String getTypeString() {
        return type != null ? type.toGraphQLString() : "Unknown";
    }
}
