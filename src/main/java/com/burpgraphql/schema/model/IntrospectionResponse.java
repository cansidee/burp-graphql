package com.burpgraphql.schema.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IntrospectionResponse {

    public DataWrapper data;
    public List<ErrorEntry> errors;

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public String getFirstError() {
        return hasErrors() ? errors.get(0).message : null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataWrapper {
        @JsonProperty("__schema")
        public SchemaData schema;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SchemaData {
        public TypeName queryType;
        public TypeName mutationType;
        public TypeName subscriptionType;
        public List<TypeDef> types;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TypeName {
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorEntry {
        public String message;
    }
}
