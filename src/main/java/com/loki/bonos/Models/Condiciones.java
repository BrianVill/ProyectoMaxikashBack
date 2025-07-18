package com.loki.bonos.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Condiciones {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idBono", referencedColumnName = "id")
    @JsonIgnore
    private Bono bono;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo", referencedColumnName = "id")
    private Tipo tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idVariable", referencedColumnName = "id")
    @JsonIgnore
    private Variables variable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idOperador", referencedColumnName = "id")
    @JsonIgnore
    private Operadores operador;

    private String valor;

    // Constructor
    public Condiciones() {
    }

    public Condiciones(Bono bono, Variables variable, Operadores operador, String valor, Tipo tipo) {
        this.bono = bono;
        this.variable = variable;
        this.operador = operador;
        this.valor = valor;
        this.tipo = tipo;
    }

    @Override
    public String toString() {
        return "Condiciones{" +
                "id=" + id +
                ", bono=" + (bono != null ? bono.getId() : "N/A") +
                ", variable=" + (variable != null ? variable.getId() : "N/A") +
                ", operador=" + (operador != null ? operador.getId() : "N/A") +
                ", valor='" + valor + '\'' +
                ", tipo=" + (tipo != null ? tipo.getId() : "N/A") +
                '}';
    }
}