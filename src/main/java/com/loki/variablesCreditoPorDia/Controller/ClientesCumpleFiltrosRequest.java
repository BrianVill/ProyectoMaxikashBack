package com.loki.variablesCreditoPorDia.Controller;

import java.util.List;

/** Payload for Cloud Tasks worker request of clientes filtros evaluacion. */
public class ClientesCumpleFiltrosRequest {
    public String mensaje;
    public List<String> to;
}
