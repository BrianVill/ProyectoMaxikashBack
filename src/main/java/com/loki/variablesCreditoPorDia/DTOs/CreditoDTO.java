package com.loki.variablesCreditoPorDia.DTOs;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class CreditoDTO {

    private String id; 
    private String idCredito;
    private UUID idGestor; // UUID del gestor

    private Integer estadoBucketId; // ID del estadoBucket
    private String estadoBucket;    // Descripción del estadoBucket

    private Integer estadoAsignacionId; // ID del estadoAsignacion
    private String estadoAsignacion;    // Descripción del estadoAsignacion

    private String resolucion;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fecha;

    // Nuevo campo para mostrar el ID del puesto asociado al crédito
    private UUID puestoId;

    public CreditoDTO() {}

    public CreditoDTO(String id, String idCredito, UUID idGestor, 
                                     Integer estadoBucketId, String estadoBucket, 
                                     Integer estadoAsignacionId, String estadoAsignacion, 
                                     String resolucion, LocalDate fecha, UUID puestoId) {
        this.id = id;
        this.idCredito = idCredito;
        this.idGestor = idGestor;
        this.estadoBucketId = estadoBucketId;
        this.estadoBucket = estadoBucket;
        this.estadoAsignacionId = estadoAsignacionId;
        this.estadoAsignacion = estadoAsignacion;
        this.resolucion = resolucion;
        this.fecha = fecha;
        this.puestoId = puestoId;
    }
}
