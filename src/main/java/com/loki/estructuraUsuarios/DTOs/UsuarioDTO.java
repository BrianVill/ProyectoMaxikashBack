package com.loki.estructuraUsuarios.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor          // ← ya tenías constructor vacío implícito
@AllArgsConstructor         // ← genera el constructor con TODOS los campos
public class UsuarioDTO {

    private UUID   id;
    private String nombre;
    private Double sueldo;
    private Double sueldoFinal;
    private String color;
}
