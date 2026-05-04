package com.burpgraphql.schema.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TypeDef {
    public String kind;
    public String name;
    public String description;
    public List<FieldDef> fields;
    public List<ArgDef> inputFields;
}
