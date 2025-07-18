package com.loki.variablesCreditoPorDia.Controller;

import java.util.List;

/** Payload for Cloud Tasks worker request for variables diarias import. */

public class VariablesDiariasImportRequest {
    public String objectName;
    public String fecha;
    public String mensaje;
    public List<String> to;
}
