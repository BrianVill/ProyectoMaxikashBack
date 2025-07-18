package com.loki.estructuraUsuarios.Models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Asignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true)
    private int id;

    @Column(length = 255)
    private String estadoAsignacion;

    // Constructor vacío
    public Asignacion() {}

    // Constructor con parámetros
    public Asignacion(int id, String estadoAsignacion) {
        if (id < 1 || id > 3) {
            throw new IllegalArgumentException("El ID de la asignación debe ser entre 1 y 3.");
        }
        this.id = id;
        this.estadoAsignacion = estadoAsignacion;
    }

    // Método toString
    @Override
    public String toString() {
        return "Asignacion{" +
                "id=" + id +
                ", estadoAsignacion='" + estadoAsignacion + '\'' +
                '}';
    }
}
