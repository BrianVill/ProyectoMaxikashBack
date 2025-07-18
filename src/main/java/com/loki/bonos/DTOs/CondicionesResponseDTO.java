package com.loki.bonos.DTOs;

import lombok.Data;

@Data
public class CondicionesResponseDTO {
    private Long id;
    private Long bonoId;  
    private VariablesDTO variable;  
    private OperadoresDTO operador;  
    private String valor;
    
    // Nuevos campos para Tipo:
    private Long tipoId;
    private String tipo;

    public CondicionesResponseDTO() {}

    public CondicionesResponseDTO(Long id, Long bonoId, VariablesDTO variable, OperadoresDTO operador, String valor, Long tipoId, String tipo) {
        this.id = id;
        this.bonoId = bonoId;
        this.tipoId = tipoId;
        this.tipo = tipo;
        this.variable = variable;
        this.operador = operador;
        this.valor = valor;
    }
}