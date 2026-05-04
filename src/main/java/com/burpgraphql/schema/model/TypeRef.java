package com.burpgraphql.schema.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TypeRef {
    public String kind;
    public String name;
    public TypeRef ofType;

    /** Unwraps NON_NULL/LIST wrappers to get the named type at the core. */
    public TypeRef getBaseType() {
        if (ofType == null) return this;
        return ofType.getBaseType();
    }

    /** Renders the full GraphQL type string, e.g. [User!]! */
    public String toGraphQLString() {
        if ("NON_NULL".equals(kind)) {
            return (ofType != null ? ofType.toGraphQLString() : "Unknown") + "!";
        }
        if ("LIST".equals(kind)) {
            return "[" + (ofType != null ? ofType.toGraphQLString() : "Unknown") + "]";
        }
        return name != null ? name : "Unknown";
    }

    public boolean isRequired() {
        return "NON_NULL".equals(kind);
    }
}
