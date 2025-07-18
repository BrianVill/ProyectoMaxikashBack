package com.loki.estructuraUsuarios.DTOs;

public class CreditoDTO {

    private String id;
    private String nombre;
    private String color;
    private Double lat;
    private Double lon;

    public CreditoDTO(String id2, String nombre2, String color2, Double lat2, Double lon2) {
        //TODO Auto-generated constructor stub
    }
    public CreditoDTO() {
        //TODO Auto-generated constructor stub
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
