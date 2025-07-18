package com.loki.variablesCreditoPorDia.Models;

import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor
@Entity
@Table(name = "clientes_cumple_bono",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"id_credito","id_bono","fecha"}))
public class ClientesCumpleBono {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_credito", nullable = false)
    private String idCredito;

    /* ahora sólo puesto */
    @Column(name = "id_puesto", nullable = false)
    private UUID puestoId;

    @Column(name = "id_bono", nullable = false)
    private Long idBono;

    @Column(nullable = false)
    private boolean cumple;

    @Column(nullable = false)
    private LocalDate fecha;

    public ClientesCumpleBono(String idCredito, UUID puestoId,
                              Long idBono, boolean cumple,
                              LocalDate fecha) {
        this.idCredito = idCredito;
        this.puestoId  = puestoId;
        this.idBono    = idBono;
        this.cumple    = cumple;
        this.fecha     = fecha;
    }
}
