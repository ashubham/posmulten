package com.github.starnowski.posmulten.configuration.yaml.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Map;

@Accessors(chain = true)
@Getter
@Setter
@EqualsAndHashCode
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForeignKeyConfiguration {

    @JsonProperty(value = "constraint_name", required = true)
    private String constraintName;
    @JsonProperty(value = "table_name", required = true)
    private String tableName;
    @JsonProperty(value = "foreign_key_primary_key_columns_mappings", required = true)
    //TODO Not empty
    private Map<String, String> foreignKeyPrimaryKeyColumnsMappings;
}
