package com.loki.estructuraUsuarios.Models;

import jakarta.persistence.*;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "usuario",
       uniqueConstraints = {
          @UniqueConstraint(columnNames = {"nombre"})
       })
public class Usuario {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name="id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "nombre", nullable = false, unique=true)
    private String nombre;

    @Column(name = "sueldo")
    private Double sueldo;

    @Column(name = "sueldo_final")
    private Double sueldoFinal;

    @Column(name = "color")
    private String color;

    // Constructors
    public Usuario() {
    }

    // Getters & Setters
    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public Double getSueldo() {
        return sueldo;
    }
    public void setSueldo(Double sueldo) {
        this.sueldo = sueldo;
    }
    public Double getSueldoFinal() {
        return sueldoFinal;
    }
    public void setSueldoFinal(Double sueldoFinal) {
        this.sueldoFinal = sueldoFinal;
    }
    public String getColor() {
        return color;
    }
    public void setColor(String color) {
        this.color = color;
    }

    public Usuario(UUID id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }
}
