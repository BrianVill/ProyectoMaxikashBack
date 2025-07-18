package com.loki.bonos.DTOs;

import com.loki.bonos.Models.Operadores;

import lombok.Data;

@Data
public class OperadoresDTO {
    private Long id;
    private String name;
    private String clase;
    private String tipo; // Nuevo campo tipo en el DTO

    public OperadoresDTO() {}

    public OperadoresDTO(Operadores operador) {
        this.id = operador.getId();
        this.name = operador.getName();
        this.clase = operador.getClase();
        this.tipo = operador.getTipo(); // Asignación del campo tipo
    }
}
