package com.burpgraphql.generator;

import com.burpgraphql.schema.model.ArgDef;
import com.burpgraphql.schema.model.FieldDef;
import com.burpgraphql.schema.model.TypeDef;
import com.burpgraphql.schema.model.TypeRef;

import java.util.Map;

public class QueryGenerator {

    private final Map<String, TypeDef> typeMap;

    public QueryGenerator(Map<String, TypeDef> typeMap) {
        this.typeMap = typeMap;
    }

    public String generate(FieldDef field, String operationType) {
        return switch (field.getGroup()) {
            case "Required Args" -> generateWithArgs(field, operationType);
            default              -> generateWithoutArgs(field, operationType);
        };
    }

    // No Arguments / Optional Args Only – skip all args
    private String generateWithoutArgs(FieldDef field, String operationType) {
        String op = operationType.toLowerCase();
        String sel = selectionSet(field.type, 2);
        return op + " {\n  " + field.name + sel + "\n}";
    }

    // Required Args – include all args as variables so the user can fill them in
    private String generateWithArgs(FieldDef field, String operationType) {
        StringBuilder varDecl = new StringBuilder();
        StringBuilder argPass = new StringBuilder();

        if (field.args != null) {
            for (ArgDef arg : field.args) {
                if (varDecl.length() > 0) varDecl.append(", ");
                varDecl.append("$").append(arg.name).append(": ").append(arg.getTypeString());
                if (argPass.length() > 0) argPass.append(", ");
                argPass.append(arg.name).append(": $").append(arg.name);
            }
        }

        String op = operationType.toLowerCase();
        String sel = selectionSet(field.type, 2);
        String vars = varDecl.length() > 0 ? "(" + varDecl + ")" : "";
        String args = argPass.length() > 0 ? "(" + argPass + ")" : "";
        return op + vars + " {\n  " + field.name + args + sel + "\n}";
    }

    /**
     * Builds a selection set for the given type ref.
     * - SCALAR / ENUM: no braces needed
     * - OBJECT: pick scalar/enum leaf fields one level deep; fallback to __typename
     */
    private String selectionSet(TypeRef typeRef, int indent) {
        if (typeRef == null) return "";

        TypeRef base = typeRef.getBaseType();
        if (base == null) return "";

        String k = base.kind;
        if ("SCALAR".equals(k) || "ENUM".equals(k)) return "";

        TypeDef typeDef = typeMap.get(base.name);
        if (typeDef == null || typeDef.fields == null || typeDef.fields.isEmpty()) {
            return " {\n" + " ".repeat(indent) + "__typename\n" + " ".repeat(indent - 2) + "}";
        }

        String pad = " ".repeat(indent);
        String close = " ".repeat(indent - 2);
        StringBuilder sb = new StringBuilder(" {\n");
        boolean added = false;

        for (FieldDef f : typeDef.fields) {
            if (f.type == null) continue;
            TypeRef fb = f.type.getBaseType();
            if (fb != null && ("SCALAR".equals(fb.kind) || "ENUM".equals(fb.kind))) {
                sb.append(pad).append(f.name).append("\n");
                added = true;
            }
        }

        if (!added) sb.append(pad).append("__typename\n");

        sb.append(close).append("}");
        return sb.toString();
    }
}
