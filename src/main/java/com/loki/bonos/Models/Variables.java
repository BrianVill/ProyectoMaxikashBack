package com.loki.bonos.Models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Variables {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // ID autogenerado

    private String name;
    private String type;
    

    // Constructor vacío (requerido por JPA)
    public Variables() {
    }

    // Constructor con parámetros
    public Variables(Long id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    // Método toString
    @Override
    public String toString() {
        return "Variables{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
