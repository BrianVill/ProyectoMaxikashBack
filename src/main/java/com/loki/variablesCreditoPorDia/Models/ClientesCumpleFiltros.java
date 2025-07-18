package com.loki.variablesCreditoPorDia.Models;

import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.*;

@Data @NoArgsConstructor
@Entity
@Table(name = "clientes_cumple_filtros",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"id_credito","id_condicion","fecha"}))
public class ClientesCumpleFiltros {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="id_credito", nullable=false)
    private String idCredito;

    @Column(name="id_condicion", nullable=false)
    private Long idCondicion;

    @Column(nullable=false)
    private Boolean cumple;

    @Column(nullable=false)
    private LocalDate fecha;          // se asigna explícitamente

    public ClientesCumpleFiltros(String idCredito, Long idCondicion,
                                 Boolean cumple, LocalDate fecha) {
        this.idCredito = idCredito;
        this.idCondicion = idCondicion;
        this.cumple = cumple;
        this.fecha = fecha;
    }
}
