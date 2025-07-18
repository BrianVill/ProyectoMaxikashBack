package com.loki.estructuraUsuarios.Controller;

import java.util.List;

/** Payload for Cloud Tasks worker request. */
public class GestoresImportRequest {
    public String objectName;
    public String mensaje;
    public List<String> to;
}
