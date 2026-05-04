package com.burpgraphql.ui;

import com.burpgraphql.schema.model.FieldDef;
import com.burpgraphql.schema.model.TypeDef;

import java.util.List;
import java.util.Map;

public class OperationContext {
    public final FieldDef field;
    public final String operationType;        // "query" | "mutation" | "subscription"
    public final Map<String, TypeDef> typeMap;
    public final String endpointUrl;
    /** All contexts in the same tree group – shared reference, populated after construction. */
    public final List<OperationContext> groupSiblings;

    public OperationContext(FieldDef field,
                            String operationType,
                            Map<String, TypeDef> typeMap,
                            String endpointUrl,
                            List<OperationContext> groupSiblings) {
        this.field = field;
        this.operationType = operationType;
        this.typeMap = typeMap;
        this.endpointUrl = endpointUrl;
        this.groupSiblings = groupSiblings;
    }

    @Override
    public String toString() {
        return field.name;
    }
}
