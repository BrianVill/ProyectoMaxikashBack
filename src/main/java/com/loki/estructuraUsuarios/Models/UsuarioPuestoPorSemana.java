package com.loki.estructuraUsuarios.Models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "usuario_puesto_por_semana",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"id_usuario", "fecha_inicio", "fecha_fin"}))
public class UsuarioPuestoPorSemana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;          // surrogate PK

    @Column(name = "id_usuario", columnDefinition = "CHAR(36)", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID usuarioId;

    @Column(name = "id_puesto", columnDefinition = "CHAR(36)", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID puestoId;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    public UsuarioPuestoPorSemana() {}

    /* getters & setters */
    public Long getId()                       { return id; }
    public void setId(Long id)                { this.id = id; }

    public UUID getUsuarioId()                { return usuarioId; }
    public void setUsuarioId(UUID uid)        { this.usuarioId = uid; }

    public UUID getPuestoId()                 { return puestoId; }
    public void setPuestoId(UUID pid)         { this.puestoId = pid; }

    public LocalDate getFechaInicio()         { return fechaInicio; }
    public void setFechaInicio(LocalDate fi)  { this.fechaInicio = fi; }

    public LocalDate getFechaFin()            { return fechaFin; }
    public void setFechaFin(LocalDate ff)     { this.fechaFin = ff; }
}
