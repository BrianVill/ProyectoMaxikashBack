package com.loki.estructuraUsuarios.Models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Data
@Entity
@Table(name = "puesto",
       uniqueConstraints = {
          @UniqueConstraint(columnNames = {"nombre"})
       })
public class Puesto {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name="id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;


    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lon")
    private Double lon;

    @Column(name = "nivel")
    private Integer nivel;

    // Self-relationship: A puesto might have a "parent" puesto. 
    // Usually this is a ManyToOne relationship pointing to the same table.
    @Column(name = "id_padre_directo", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID idPadreDirecto;

    // Constructors
    public Puesto() {
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
    public Double getLat() {
        return lat;
    }
    public void setLat(Double lat) {
        this.lat = lat;
    }
    public Double getLon() {
        return lon;
    }
    public void setLon(Double lon) {
        this.lon = lon;
    }
    public Integer getNivel() {
        return nivel;
    }
    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }
    public UUID getIdPadreDirecto() {
        return idPadreDirecto;
    }
    public void setIdPadreDirecto(UUID idPadreDirecto) {
        this.idPadreDirecto = idPadreDirecto;
    }

    public Puesto(UUID id, String nombre, Integer nivel) {
        this.id = id;
        this.nombre = nombre;
        this.nivel = nivel;
    }
}
