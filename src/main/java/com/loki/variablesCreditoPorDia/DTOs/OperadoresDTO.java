package com.loki.variablesCreditoPorDia.DTOs;

import lombok.Data;

@Data
public class OperadoresDTO {
    private Long id;
    private String name;
    private String clase;
    private String tipo;

    public OperadoresDTO() {}

    public OperadoresDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
