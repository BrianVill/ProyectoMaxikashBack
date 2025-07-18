package com.loki.variablesCreditoPorDia.DTOs;

import lombok.Data;

@Data
public class Bono {
    private Long id;
    private String nombre;
    private String grupo;
    private String lunes;
    private String martes;
    private String miercoles;
    private String jueves;
    private String viernes;
    private String sabado;
    private String domingo;
}
