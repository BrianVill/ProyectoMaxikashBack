package com.loki.bonos.Models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Operadores {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String clase;
    private String tipo; // Nueva columna tipo

    // Constructor vacío
    public Operadores() {
    }

    // Constructor con parámetros
    public Operadores(Long id, String name, String clase, String tipo) {
        this.id = id;
        this.name = name;
        this.clase = clase;
        this.tipo = tipo; // Asignación del nuevo campo
    }

    // Método toString
    @Override
    public String toString() {
        return "Operadores{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", clase='" + clase + '\'' +
                ", tipo='" + tipo + '\'' + // Nuevo campo en toString
                '}';
    }
}
