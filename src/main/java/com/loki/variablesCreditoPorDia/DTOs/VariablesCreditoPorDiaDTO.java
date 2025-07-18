package com.loki.variablesCreditoPorDia.DTOs;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class VariablesCreditoPorDiaDTO {

    private Long   id;
    private String idCredito;
    private UUID   puestoId;        // ← nuevo
    private Long   idVariable;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fecha;

    private String valor;

    public VariablesCreditoPorDiaDTO() {}

    public VariablesCreditoPorDiaDTO(Long id, String idCredito,
                                     UUID puestoId, Long idVariable,
                                     LocalDate fecha, String valor) {
        this.id         = id;
        this.idCredito  = idCredito;
        this.puestoId   = puestoId;
        this.idVariable = idVariable;
        this.fecha      = fecha;
        this.valor      = valor;
    }
}
