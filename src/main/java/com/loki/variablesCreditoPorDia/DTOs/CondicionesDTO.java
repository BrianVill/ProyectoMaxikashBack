package com.loki.variablesCreditoPorDia.DTOs;

import lombok.Data;

@Data
public class CondicionesDTO {
    private Long id;
    private Long bonoId;
    private Variable variable;  
    private OperadoresDTO operador;
    private String valor;
    private Long tipoId;
    private String tipo;

    public CondicionesDTO() {}

    public CondicionesDTO(Long id, Long bonoId, Variable variable, OperadoresDTO operador, String valor, Long tipoId, String tipo) {
        this.id = id;
        this.bonoId = bonoId;
        this.variable = variable;
        this.operador = operador;
        this.valor = valor;
        this.tipoId = tipoId;
        this.tipo = tipo;
    }
}

