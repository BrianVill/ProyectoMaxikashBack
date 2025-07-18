package com.loki.estructuraUsuarios.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Cacheable;
import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Data
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Nivel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private int nivel; // Representa la jerarquía (1 = supervisor, 2 = gestor, etc.)

    @Column(nullable = false, unique = true)
    private String nombre; // Nombre asociado al nivel (ejemplo: gestor, supervisor, etc.)

    @Column(nullable = true) // Columna opcional para almacenar un color en formato HEX
    private String color;

    public Nivel() {}

    public Nivel(int nivel, String nombre, String color) {
        this.nivel = nivel;
        this.nombre = nombre;
        this.color = color;
    }
}
