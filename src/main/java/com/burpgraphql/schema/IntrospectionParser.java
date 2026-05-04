package com.burpgraphql.schema;

import com.burpgraphql.schema.model.FieldDef;
import com.burpgraphql.schema.model.IntrospectionResponse;
import com.burpgraphql.schema.model.TypeDef;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntrospectionParser {

    public static class ParsedSchema {
        public final List<FieldDef> queries;
        public final List<FieldDef> mutations;
        public final List<FieldDef> subscriptions;
        public final Map<String, TypeDef> typeMap;

        public ParsedSchema(List<FieldDef> queries,
                            List<FieldDef> mutations,
                            List<FieldDef> subscriptions,
                            Map<String, TypeDef> typeMap) {
            this.queries = queries;
            this.mutations = mutations;
            this.subscriptions = subscriptions;
            this.typeMap = typeMap;
        }
    }

    public static ParsedSchema parse(IntrospectionResponse response) {
        IntrospectionResponse.SchemaData schema = response.data.schema;
        List<TypeDef> types = schema.types != null ? schema.types : Collections.emptyList();

        Map<String, TypeDef> typeMap = new HashMap<>();
        for (TypeDef type : types) {
            if (type.name != null) {
                typeMap.put(type.name, type);
            }
        }

        String queryTypeName = schema.queryType != null ? schema.queryType.name : "Query";
        String mutationTypeName = schema.mutationType != null ? schema.mutationType.name : null;
        String subscriptionTypeName = schema.subscriptionType != null ? schema.subscriptionType.name : null;

        return new ParsedSchema(
            fieldsFor(typeMap, queryTypeName),
            fieldsFor(typeMap, mutationTypeName),
            fieldsFor(typeMap, subscriptionTypeName),
            typeMap
        );
    }

    private static List<FieldDef> fieldsFor(Map<String, TypeDef> typeMap, String typeName) {
        if (typeName == null) return Collections.emptyList();
        TypeDef type = typeMap.get(typeName);
        if (type == null || type.fields == null) return Collections.emptyList();
        return type.fields;
    }
}
