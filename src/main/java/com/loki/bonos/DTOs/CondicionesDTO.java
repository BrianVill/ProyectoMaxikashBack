package com.loki.bonos.DTOs;

import lombok.Data;

@Data
public class CondicionesDTO {
    private Long bonoId;
    private Long variableId;
    private Long operadorId;
    private String valor;
    // Nuevo campo para relacionar con Tipo:
    private Long tipoId;

    public CondicionesDTO() {}

    public CondicionesDTO(Long bonoId, Long variableId, Long operadorId, String valor, Long tipoId) {
        this.bonoId = bonoId;
        this.variableId = variableId;
        this.operadorId = operadorId;
        this.valor = valor;
        this.tipoId = tipoId;
    }

    @Override
    public String toString() {
        return "CondicionesDTO{" +
                "bonoId=" + bonoId +
                ", variableId=" + variableId +
                ", operadorId=" + operadorId +
                ", valor='" + valor + '\'' +
                ", tipoId=" + tipoId +
                '}';
    }
}

