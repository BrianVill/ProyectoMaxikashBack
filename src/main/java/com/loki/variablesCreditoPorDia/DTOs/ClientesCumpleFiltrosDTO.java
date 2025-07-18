package com.loki.variablesCreditoPorDia.DTOs;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class ClientesCumpleFiltrosDTO {

    private Long id;
    private String idCredito;
    private Long idCondicion;
    private boolean cumple;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fecha;

    public ClientesCumpleFiltrosDTO() {}

    public ClientesCumpleFiltrosDTO(Long id, String idCredito, Long idCondicion, boolean cumple, LocalDate fecha) {
        this.id = id;
        this.idCredito = idCredito;
        this.idCondicion = idCondicion;
        this.cumple = cumple;
        this.fecha = fecha;
    }
}
