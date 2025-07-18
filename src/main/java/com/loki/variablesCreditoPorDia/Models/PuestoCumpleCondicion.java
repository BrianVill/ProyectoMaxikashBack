package com.loki.variablesCreditoPorDia.Models;

import java.time.LocalDate;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "puesto_cumple_condicion", uniqueConstraints = @UniqueConstraint(columnNames = { "id_puesto",
        "id_condicion", "fecha" }))
public class PuestoCumpleCondicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_condicion", nullable = false)
    private Long idCondicion;

    @Column(name = "id_puesto", nullable = false)
    private UUID puestoId;

    @Column(nullable = false)
    private boolean cumple;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    @Column(nullable = false)
    private LocalDate fecha;
}
