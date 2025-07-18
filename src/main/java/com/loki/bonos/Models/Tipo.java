package com.loki.bonos.Models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Tipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Campo para almacenar el nombre o descripción del tipo.
    @Column(nullable = false)
    private String tipo;
    
    // Constructor vacío (requerido por JPA)
    public Tipo() {}

    // Constructor completo (opcional)
    public Tipo(String tipo) {
        this.tipo = tipo;
    }
}
