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
@Table(name = "bonos_por_puestos", uniqueConstraints = @UniqueConstraint(columnNames = { "id_puesto", "id_bono" }))
public class BonosPorPuestos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_puesto", nullable = false)
    private UUID puestoId;

    @Column(name = "id_bono", nullable = false)
    private Long idBono;

    @Column(nullable = false)
    private Double monto;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    @Column(nullable = false)
    private LocalDate fecha;
}
