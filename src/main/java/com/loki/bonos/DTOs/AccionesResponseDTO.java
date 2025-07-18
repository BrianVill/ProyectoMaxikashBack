package com.loki.bonos.DTOs;

import com.loki.bonos.Models.Operadores;
import com.loki.bonos.Models.Variables;

import lombok.Data;

@Data
public class AccionesResponseDTO {

    private Long id;
    private Long bonoId;  // Añadimos bonoId
    private Variables variable;
    private Operadores operador;
    private String valor;

    public AccionesResponseDTO(Long id, Long bonoId, Variables variable, Operadores operador, String valor) {
        this.id = id;
        this.bonoId = bonoId;
        this.variable = variable;
        this.operador = operador;
        this.valor = valor;
    }
}
