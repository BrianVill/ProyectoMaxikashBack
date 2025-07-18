package com.loki.variablesCreditoPorDia.DTOs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import lombok.Data;

@Data
public class PuestoDTO {

    private UUID id;
    private String nombre;

    private Double lat;
    private Double lon;
    private Long sueldo;

    private UUID idPadreDirecto;
    private int nivel;
    private Double sueldoFinal;

    private List<CreditoDTO> creditos = new ArrayList<>();

    public PuestoDTO() {}

    public PuestoDTO(UUID id, String nombre, Double lat, Double lon, Long sueldo, UUID idPadreDirecto, int nivel,
                      Double sueldoFinal, List<CreditoDTO> creditos) {
        this.id = id;
        this.nombre = nombre != null ? nombre : "Sin dato";
        this.lat = lat;
        this.lon = lon;
        this.sueldo = sueldo;
        this.idPadreDirecto = idPadreDirecto;
        this.nivel = nivel;
        this.sueldoFinal = sueldoFinal;
        this.creditos = creditos != null ? creditos : new ArrayList<>();
    }
}


