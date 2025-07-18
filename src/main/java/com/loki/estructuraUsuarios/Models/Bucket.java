package com.loki.estructuraUsuarios.Models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Bucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true)
    private int id;

    @Column(length = 255)
    private String estadoBucket;

    // Constructor vacío
    public Bucket() {}

    // Constructor con parámetros
    public Bucket(int id, String estadoBucket) {
        if (id < 1 || id > 9) {
            throw new IllegalArgumentException("El ID del bucket debe estar entre 1 y 9.");
        }
        this.id = id;
        this.estadoBucket = estadoBucket;
    }

    @Override
    public String toString() {
        return "Bucket{" +
                "id=" + id +
                ", estadoBucket='" + estadoBucket + '\'' +
                '}';
    }

}
