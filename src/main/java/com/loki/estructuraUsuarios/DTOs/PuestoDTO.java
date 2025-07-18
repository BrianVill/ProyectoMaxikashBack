package com.loki.estructuraUsuarios.DTOs;

import lombok.Data;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Data
@JsonPropertyOrder({
    "id", "nombre", "lat", "lon", "idPadreDirecto", "nivel"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PuestoDTO {

    private UUID id;
    private String nombre;

    
    private Double lat;
    private Double lon;

    private UUID idPadreDirecto;
    private Integer nivel;
    



    public PuestoDTO() {}

    public PuestoDTO(UUID id, String nombre, Double lat, Double lon, UUID idPadreDirecto, int nivel) {
        this.id = id;
        this.nombre = nombre;
        this.lat = lat;
        this.lon = lon;
        this.idPadreDirecto = idPadreDirecto;
        this.nivel = nivel;
    }
}
