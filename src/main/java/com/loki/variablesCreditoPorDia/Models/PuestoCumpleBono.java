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
@Table(name = "puesto_cumple_bono", uniqueConstraints = @UniqueConstraint(columnNames = { "id_puesto", "id_bono",
        "fecha" }))
public class PuestoCumpleBono {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_bono", nullable = false)
    private Long idBono;

    @Column(name = "id_puesto", nullable = false)
    private UUID puestoId;

    @Column(nullable = false)
    private boolean cumple;

    @Column(nullable = false)
    private String progreso; // “X/Y”

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    @Column(nullable = false)
    private LocalDate fecha;
}
