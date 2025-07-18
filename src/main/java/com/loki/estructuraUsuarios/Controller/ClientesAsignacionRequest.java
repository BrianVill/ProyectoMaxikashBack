package com.loki.estructuraUsuarios.Controller;

import java.util.List;

/** Payload for Cloud Tasks worker request for clientes asignacion. */
public class ClientesAsignacionRequest {
    public double threshold;
    public int capacity;
    public double maxThreshold;
    public String mensaje;
    public List<String> to;
}
