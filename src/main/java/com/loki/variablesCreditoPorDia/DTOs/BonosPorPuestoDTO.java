package com.loki.variablesCreditoPorDia.DTOs;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class BonosPorPuestoDTO {

    private Long id;
    private UUID idPuesto;           // ← ahora UUID
    private Long idBono;
    private Double monto;
    private LocalDateTime fecha;

    public BonosPorPuestoDTO() {}

    public BonosPorPuestoDTO(Long id, UUID idPuesto, Long idBono,
                             Double monto, LocalDateTime fecha) {
        this.id       = id;
        this.idPuesto = idPuesto;
        this.idBono   = idBono;
        this.monto    = monto;
        this.fecha    = fecha;
    }
}
