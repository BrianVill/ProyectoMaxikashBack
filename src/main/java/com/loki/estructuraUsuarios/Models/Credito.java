package com.loki.estructuraUsuarios.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "credito",
       uniqueConstraints = {
          @UniqueConstraint(columnNames = {"id"})
       })
public class Credito {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "color")
    private String color;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lon")
    private Double lon;

    // Constructors
    public Credito() { 
    }

    // Getters & Setters
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getColor() {
        return color;
    }
    public void setColor(String color) {
        this.color = color;
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
}
