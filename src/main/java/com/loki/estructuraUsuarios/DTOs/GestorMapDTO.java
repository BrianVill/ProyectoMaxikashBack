package com.loki.estructuraUsuarios.DTOs;

import java.util.UUID;

public record GestorMapDTO(UUID id, String nombre, double lat, double lon, String color, String firstUsuario) 
{
    
}