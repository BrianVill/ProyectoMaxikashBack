package com.loki.estructuraUsuarios.DTOs;

import java.util.UUID;

public record UsuarioPorPuestoDTO(
        UUID puestoId,
        UUID usuarioId,
        String nombre,
        String color) {}