package com.loki.estructuraUsuarios.Controller;

import java.util.List;

/** Payload for Cloud Tasks worker request for clientes correccion. */

public class ClientesCorreccionRequest {
    public String objectName;
    public String mensaje;
    public List<String> to;
}
