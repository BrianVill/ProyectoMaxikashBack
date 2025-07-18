package com.loki.bonos.Models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Bono {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    // Se eliminó la propiedad "tipo"
    private String grupo;

    @Column(nullable = true)
    private String lunes;

    @Column(nullable = true)
    private String martes;

    @Column(nullable = true)
    private String miercoles;

    @Column(nullable = true)
    private String jueves;

    @Column(nullable = true)
    private String viernes;

    @Column(nullable = true)
    private String sabado;

    @Column(nullable = true)
    private String domingo;

    // Se eliminó también la columna "porcentual" en la versión anterior (si ya la eliminaste)

    @OneToMany(mappedBy = "bono", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Condiciones> condiciones;

    public Bono() {
    }

    // Constructor actualizado (sin "tipo")
    public Bono(Long id, String nombre, String grupo, String lunes, String martes, String miercoles,
                String jueves, String viernes, String sabado, String domingo) {
        this.id = id;
        this.nombre = nombre;
        this.grupo = grupo;
        this.lunes = lunes;
        this.martes = martes;
        this.miercoles = miercoles;
        this.jueves = jueves;
        this.viernes = viernes;
        this.sabado = sabado;
        this.domingo = domingo;
    }

    @Override
    public String toString() {
        return "Bono{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", grupo='" + grupo + '\'' +
                ", lunes='" + lunes + '\'' +
                ", martes='" + martes + '\'' +
                ", miercoles='" + miercoles + '\'' +
                ", jueves='" + jueves + '\'' +
                ", viernes='" + viernes + '\'' +
                ", sabado='" + sabado + '\'' +
                ", domingo='" + domingo + '\'' +
                '}';
    }
}


