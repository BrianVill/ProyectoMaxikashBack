package com.loki.estructuraUsuarios.Models;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
    private String role;  // agrega este campo
}


