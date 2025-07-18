package com.loki.estructuraUsuarios.Controller;

import java.util.List;

/** Payload for Cloud Tasks worker request for clientes import. */

public class ClientesImportRequest {
    public String objectName;
    public String mensaje;
    public List<String> to;
}
