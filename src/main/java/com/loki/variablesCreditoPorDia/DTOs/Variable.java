package com.loki.variablesCreditoPorDia.DTOs;

import lombok.Data;

@Data
public class Variable {
    private Long id;
    private String name;
    private String type;
    private Long campoCalculadoId;
}

