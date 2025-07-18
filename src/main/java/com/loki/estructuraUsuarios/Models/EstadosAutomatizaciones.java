package com.loki.estructuraUsuarios.Models;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "estados_automatizaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadosAutomatizaciones {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true) // . Eliminado unique = true
    private String nombre;

    @Column(nullable = false)
    private String estado;

    @Lob
    @Column(nullable = true, columnDefinition = "TEXT")
    private String mensaje; // ✅ Nuevo campo agregado

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    @Column(nullable = false, updatable = false)
    private LocalDate fecha; // ✅ Nuevo campo con fecha automática

    @PrePersist
    protected void onCreate() {
        this.fecha = LocalDate.now(); // ✅ Fecha se genera automáticamente al hacer POST
    }
}


