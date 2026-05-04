package com.burpgraphql.schema.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldDef {
    public String name;
    public String description;
    public List<ArgDef> args;
    public TypeRef type;

    @JsonProperty("isDeprecated")
    public boolean isDeprecated;
    public String deprecationReason;

    public boolean hasArgs() {
        return args != null && !args.isEmpty();
    }

    public boolean hasRequiredArgs() {
        if (args == null) return false;
        return args.stream().anyMatch(ArgDef::isRequired);
    }

    /**
     * Returns the grouping bucket for this operation:
     *   "No Arguments"      – zero args
     *   "Optional Args Only" – all args nullable
     *   "Required Args"     – at least one NON_NULL arg
     */
    public String getGroup() {
        if (!hasArgs()) return "No Arguments";
        if (hasRequiredArgs()) return "Required Args";
        return "Optional Args Only";
    }
}
