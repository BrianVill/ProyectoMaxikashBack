package com.loki.estructuraUsuarios.Models;

import java.util.List;

public class AsignacionRequest {

    /* ───── parámetros que ya existían ───── */
    private double threshold;
    private int    capacity;
    private double maxThreshold;

    /* ───── NUEVOS opcionales ───── */
    private String       mensaje;   // texto del e-mail
    private List<String> to;        // correos destino

    /* ───────── getters & setters ───────── */
    public double getThreshold()    { return threshold; }
    public void   setThreshold(double t) { this.threshold = t; }

    public int    getCapacity()     { return capacity; }
    public void   setCapacity(int c){ this.capacity = c; }

    public double getMaxThreshold() { return maxThreshold; }
    public void   setMaxThreshold(double m){ this.maxThreshold = m; }

    /* ---- NUEVOS ---- */
    public String getMensaje()            { return mensaje; }
    public void   setMensaje(String msg)  { this.mensaje = msg; }

    public List<String> getTo()           { return to; }
    public void   setTo(List<String> to)  { this.to = to; }
}
