package com.loki.estructuraUsuarios.Models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "credito_puesto_por_semana",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"id_credito", "fecha_inicio", "fecha_fin"}))
public class CreditoPuestoPorSemana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* FK — text PK from Credito */
    @Column(name = "id_credito", nullable = false, length = 64)
    private String creditoId;

    /* FK — UUID PK from Puesto (gestor) */
    @Column(name = "id_puesto", length = 36, columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID puestoId;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    public CreditoPuestoPorSemana() {}

    /* getters & setters */
    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getCreditoId()               { return creditoId; }
    public void setCreditoId(String creditoId) { this.creditoId = creditoId; }

    public UUID getPuestoId()                  { return puestoId; }
    public void setPuestoId(UUID puestoId)     { this.puestoId = puestoId; }

    public LocalDate getFechaInicio()          { return fechaInicio; }
    public void setFechaInicio(LocalDate fi)   { this.fechaInicio = fi; }

    public LocalDate getFechaFin()             { return fechaFin; }
    public void setFechaFin(LocalDate ff)      { this.fechaFin = ff; }
}
