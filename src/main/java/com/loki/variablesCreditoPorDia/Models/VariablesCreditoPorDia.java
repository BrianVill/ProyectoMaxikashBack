package com.loki.variablesCreditoPorDia.Models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "variables_creditos_por_dia",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"id_credito","id_variable","fecha"}))
public class VariablesCreditoPorDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_credito", nullable = false)
    private String idCredito;

    /* ► ahora guarda el puesto */
    @Column(name = "id_puesto", nullable = false)
    private UUID puestoId;

    @Column(name = "id_variable", nullable = false)
    private Long idVariable;

    @Column(nullable = false, updatable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private String valor;

    public VariablesCreditoPorDia() {}

    public VariablesCreditoPorDia(String idCredito, UUID puestoId,
                                  Long idVariable, String valor) {
        this.idCredito  = idCredito;
        this.puestoId   = puestoId;
        this.idVariable = idVariable;
        this.valor      = valor;
        this.fecha      = LocalDate.now();
    }
}
