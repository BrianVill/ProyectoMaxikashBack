package com.loki.bonos.DTOs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder({ 
    "id", "nombre", "grupo", "lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo", 
    "condiciones", "acciones"
})
public class BonoResponseDTO {

    private Long id;
    private String nombre;
    private String grupo;
    private String lunes;
    private String martes;
    private String miercoles;
    private String jueves;
    private String viernes;
    private String sabado;
    private String domingo;
    private List<CondicionesResponseDTO> condiciones;
    private List<AccionesResponseDTO> acciones;

    public BonoResponseDTO() {
    }

    public BonoResponseDTO(Long id, String nombre, String grupo, String lunes, String martes,
                           String miercoles, String jueves, String viernes, String sabado, String domingo,
                           List<CondicionesResponseDTO> condiciones, List<AccionesResponseDTO> acciones) {
        this.id = id;
        this.nombre = nombre;
        this.grupo = grupo;
        this.lunes = lunes;
        this.martes = martes;
        this.miercoles = miercoles;
        this.jueves = jueves;
        this.viernes = viernes;
        this.sabado = sabado;
        this.domingo = domingo;
        this.condiciones = condiciones;
        this.acciones = acciones;
    }
}