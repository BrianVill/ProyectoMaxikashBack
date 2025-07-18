package com.loki.bonos.DTOs;

import lombok.Data;

@Data
public class AccionesDTO {
    private Long bonoId;
    private Long variableId;
    private Long operadorId;
    private String valor;

    public AccionesDTO() {}

    // Constructor
    public AccionesDTO(Long bonoId, Long variableId, Long operadorId, String valor) {
        this.bonoId = bonoId;
        this.variableId = variableId;
        this.operadorId = operadorId;
        this.valor = valor;
    }
    
    @Override
    public String toString() {
        return "AccionesDTO{" +
                "bonoId=" + bonoId +
                ", variableId=" + variableId +
                ", operadorId=" + operadorId +
                ", valor='" + valor + '\'' +
                '}';
    }
}
