package com.loki.bonos.DTOs;

import com.loki.bonos.Models.Variables;

import lombok.Data;

@Data
public class VariablesDTO {

    private Long id;
    private String name;
    private String type;
    private Long campoCalculadoId;  // Campo opcional

    public VariablesDTO() {
    }

    public VariablesDTO(Variables variables) {
        this.id = variables.getId();
        this.name = variables.getName();
        this.type = variables.getType();
    }
}
