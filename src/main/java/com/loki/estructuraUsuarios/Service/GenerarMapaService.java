package com.loki.estructuraUsuarios.Service;

import com.loki.estructuraUsuarios.Models.Credito;
import com.loki.estructuraUsuarios.Models.CreditoPuestoPorSemana;
import com.loki.estructuraUsuarios.Models.Puesto;
import com.loki.estructuraUsuarios.Models.Usuario;
import com.loki.estructuraUsuarios.Models.UsuarioPuestoPorSemana;
import com.loki.estructuraUsuarios.Repository.CreditoPuestoPorSemanaRepository;
import com.loki.estructuraUsuarios.Repository.CreditoRepository;
import com.loki.estructuraUsuarios.Repository.PuestoRepository;
import com.loki.estructuraUsuarios.Repository.UsuarioPuestoPorSemanaRepository;
import com.loki.estructuraUsuarios.Repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GenerarMapaService {

    private static final String MAPA_HTML = "/tmp/uploads/mapa_asignacion_gestores.html";

    private final CreditoRepository creditoRepo;
    private final CreditoPuestoPorSemanaRepository cppsRepo;
    private final PuestoRepository puestoRepo;
    private final UsuarioPuestoPorSemanaRepository uppsRepo;
    private final UsuarioRepository usuarioRepo;

    public GenerarMapaService(CreditoRepository creditoRepo,
                              CreditoPuestoPorSemanaRepository cppsRepo,
                              PuestoRepository puestoRepo,
                              UsuarioPuestoPorSemanaRepository uppsRepo,
                              UsuarioRepository usuarioRepo) {
        this.creditoRepo = creditoRepo;
        this.cppsRepo    = cppsRepo;
        this.puestoRepo  = puestoRepo;
        this.uppsRepo    = uppsRepo;
        this.usuarioRepo = usuarioRepo;
    }

    public void GenerarMapa() throws Exception {

        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate sunday = LocalDate.now().with(DayOfWeek.SUNDAY);

        /* 1) ASIGNACIONES DE LA SEMANA -------------------------------------- */
        List<CreditoPuestoPorSemana> asignaciones = cppsRepo
                .findByFechaInicioAndFechaFin(monday, sunday);

        Map<String, CreditoPuestoPorSemana> mapAsign = asignaciones.stream()
                .collect(Collectors.toMap(CreditoPuestoPorSemana::getCreditoId, a -> a));

        /* 2) CREDITOS implicados (un solo select) --------------------------- */
        List<Credito> creditos = creditoRepo.findAllById(mapAsign.keySet());

        /* 3) UPPs de la semana (un solo select) ----------------------------- */
        List<UsuarioPuestoPorSemana> upps = uppsRepo
                .findAllByFechaInicioAndFechaFin(monday, sunday);

        /* -- mapas auxiliares para lookup O(1) ------------------------------ */
        // puesto → primer usuario encontrado
        Map<UUID, UUID> puestoToUsuario = upps.stream()
                .filter(u -> u.getPuestoId() != null && u.getUsuarioId() != null)
                .collect(Collectors.toMap(
                        UsuarioPuestoPorSemana::getPuestoId,
                        UsuarioPuestoPorSemana::getUsuarioId,
                        (u1, u2) -> u1   // si hay más de uno, nos quedamos con el primero
                ));

        /* 4) PUESTOS en bloque --------------------------------------------- */
        List<Puesto> puestos = puestoRepo.findAllById(puestoToUsuario.keySet());
        Map<UUID, Puesto> puestoMap = puestos.stream()
                .collect(Collectors.toMap(Puesto::getId, p -> p));

        /* 5) USUARIOS en bloque -------------------------------------------- */
        List<Usuario> usuarios = usuarioRepo.findAllById(new HashSet<>(puestoToUsuario.values()));
        Map<UUID, Usuario> usuarioMap = usuarios.stream()
                .collect(Collectors.toMap(Usuario::getId, u -> u));

        /* ------------------------------------------------------------------- */
        /* 6) BUILD “GESTORES” LIST (ya sin SELECT individuales)               */
        /* ------------------------------------------------------------------- */
        List<Map<String, Object>> finalGestores = new ArrayList<>();

        for (UUID puestoId : puestoToUsuario.keySet()) {

            Puesto  p  = puestoMap.get(puestoId);
            if (p == null || p.getNivel() != 2) continue;          // filtramos nivel≠2

            Usuario u  = usuarioMap.get(puestoToUsuario.get(puestoId));

            Map<String, Object> gst = new HashMap<>();
            gst.put("id",     p.getId());
            gst.put("nombre", u != null ? u.getNombre() : "SIN_USUARIO");
            gst.put("lat",    p.getLat());
            gst.put("lon",    p.getLon());
            gst.put("color",  u != null ? u.getColor() : "#000000");
            finalGestores.add(gst);
        }

        /* ------------------------------------------------------------------- */
        /* 7) BUILD “CLIENTES” LIST (lookup en mapas, sin hits a la BD)        */
        /* ------------------------------------------------------------------- */
        List<Map<String, Object>> finalClients = new ArrayList<>();

        for (Credito c : creditos) {
            CreditoPuestoPorSemana cp = mapAsign.get(c.getId());
            UUID puestoId   = cp != null ? cp.getPuestoId() : null;
            UUID usuarioId  = puestoId != null ? puestoToUsuario.get(puestoId) : null;
            String gestorNom = (usuarioId != null && usuarioMap.containsKey(usuarioId))
                               ? usuarioMap.get(usuarioId).getNombre()
                               : "SIN_GESTOR";

            if (esMexico(c.getLat(), c.getLon())) {
                Map<String, Object> cli = new HashMap<>();
                cli.put("id",              c.getId());
                cli.put("nombre",          c.getNombre());
                cli.put("lat",             c.getLat());
                cli.put("lon",             c.getLon());
                cli.put("color",           c.getColor());
                cli.put("gestor_asignado", gestorNom);
                finalClients.add(cli);
            }
            
        }
        
        // 6) Build color dictionary
        Set<String> gestorNames = finalGestores.stream()
            .map(g -> (String)g.get("nombre"))
            .collect(Collectors.toSet());
        gestorNames.add("SIN_GESTOR");

        List<String> palette = generateDistinctColors(250);
        Map<String,String> colorDict = new HashMap<>();
        int idx = 0;
        for (String name : gestorNames) {
            colorDict.put(name, palette.get(idx++ % palette.size()));
        }
        colorDict.put("SIN_GESTOR", "#808080");

        // 7) Begin HTML builder
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
            .append("<html>\n")
            .append("<head>\n")
            .append("    <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n")
            .append("    <script>\n")
            .append("        L_NO_TOUCH = false;\n")
            .append("        L_DISABLE_3D = false;\n")
            .append("    </script>\n")
            .append("    <style>html, body {width: 100%;height: 100%;margin: 0;padding: 0;}</style>\n")
            .append("    <style>#map {position:absolute;top:0;bottom:0;right:0;left:0;}</style>\n")
            .append("    <script src=\"https://cdn.jsdelivr.net/npm/leaflet@1.9.3/dist/leaflet.js\"></script>\n")
            .append("    <script src=\"https://code.jquery.com/jquery-3.7.1.min.js\"></script>\n")
            .append("    <script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.2.2/dist/js/bootstrap.bundle.min.js\"></script>\n")
            .append("    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/Leaflet.awesome-markers/2.0.2/leaflet.awesome-markers.js\"></script>\n")
            .append("    <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/leaflet@1.9.3/dist/leaflet.css\"/>\n")
            .append("    <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.2.2/dist/css/bootstrap.min.css\"/>\n")
            .append("    <link rel=\"stylesheet\" href=\"https://netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap-glyphicons.css\"/>\n")
            .append("    <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@6.2.0/css/all.min.css\"/>\n")
            .append("    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/Leaflet.awesome-markers/2.0.2/leaflet.awesome-markers.css\"/>\n")
            .append("    <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/gh/python-visualization/folium/folium/templates/leaflet.awesome.rotate.min.css\"/>\n")
            .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\" />\n")
            .append("    <title>Mapa Asignación Gestores</title>\n")
            .append("    <style>\n")
            .append("        .leaflet-container { font-size: 1rem; }\n")
            .append("    </style>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("    <div class=\"folium-map\" id=\"map\" ></div>\n")
            .append("    <script>\n")
            .append("        var map = L.map('map', {\n")
            .append("            center: [23.6345, -102.5528],\n")
            .append("            zoom: 5,\n")
            .append("            zoomControl: true,\n")
            .append("            preferCanvas: false\n")
            .append("        });\n\n")
            .append("        var tile_layer = L.tileLayer(\n")
            .append("            'https://tile.openstreetmap.org/{z}/{x}/{y}.png',\n")
            .append("            {\n")
            .append("                \"attribution\": \"&copy; <a href=\\\"https://www.openstreetmap.org/copyright\\\">OpenStreetMap</a> contributors\",\n")
            .append("                \"detectRetina\": false,\n")
            .append("                \"maxNativeZoom\": 19,\n")
            .append("                \"maxZoom\": 19,\n")
            .append("                \"minZoom\": 0,\n")
            .append("                \"noWrap\": false,\n")
            .append("                \"opacity\": 1,\n")
            .append("                \"subdomains\": \"abc\",\n")
            .append("                \"tms\": false\n")
            .append("            }\n")
            .append("        );\n")
            .append("        tile_layer.addTo(map);\n\n");
        // 8. Crear los FeatureGroups para cada gestor
        Map<String, String> gestorToFeatureGroup = new HashMap<>();
        int counterFG = 0;
        for (String gName : gestorNames) {
            String safeName = gName.replaceAll("[^a-zA-Z0-9]+", "_");
            String fgVar = "feature_group_" + counterFG + "_" + safeName;
            gestorToFeatureGroup.put(gName, fgVar);
            html.append("        var ").append(fgVar).append(" = L.featureGroup({});\n");
            counterFG++;
        }
        html.append("\n");

        // 9. Marcadores de GESTORES
        counterFG = 0;
        for (Map<String, Object> gst : finalGestores) {
            String gname = gst.get("nombre") != null ? gst.get("nombre").toString() : "SIN_NOMBRE";
            String fgVar = gestorToFeatureGroup.getOrDefault(gname, gestorToFeatureGroup.get("SIN_GESTOR"));
            Object latObj = gst.get("lat");
            Object lonObj = gst.get("lon");
            if (latObj == null || lonObj == null) {
                System.out.println("[WARNING] Gestor sin coordenadas: " + gname);
                continue;
            }
            double lat = Double.parseDouble(latObj.toString());
            double lon = Double.parseDouble(lonObj.toString());
            String col = colorDict.getOrDefault(gname, "#000000");
            String colorCompat = gst.get("color") != null ? gst.get("color").toString() : "desconocido";
            String circleMarkerVar = "circle_marker_g_" + counterFG;
            counterFG++;
            html.append("        var ").append(circleMarkerVar).append(" = L.circleMarker(\n")
                .append("            [").append(lat).append(", ").append(lon).append("],\n")
                .append("            {\n")
                .append("                \"bubblingMouseEvents\": true,\n")
                .append("                \"color\": \"").append(col).append("\",\n")
                .append("                \"dashArray\": null,\n")
                .append("                \"dashOffset\": null,\n")
                .append("                \"fill\": true,\n")
                .append("                \"fillColor\": \"#000000\",  // replicate Folium's black fill for gestor\n")
                .append("                \"fillOpacity\": 1.0,\n")
                .append("                \"fillRule\": \"evenodd\",\n")
                .append("                \"lineCap\": \"round\",\n")
                .append("                \"lineJoin\": \"round\",\n")
                .append("                \"opacity\": 1.0,\n")
                .append("                \"radius\": 10,\n")
                .append("                \"stroke\": true,\n")
                .append("                \"weight\": 3\n")
                .append("            }\n")
                .append("        ).addTo(").append(fgVar).append(");\n\n")
                .append("        ").append(circleMarkerVar).append(".bindTooltip(\n")
                .append("            `<div>\n")
                .append("                 Gestor: ").append(gname).append("<br>Color: ").append(colorCompat).append("\n")
                .append("             </div>`,\n")
                .append("            {\"sticky\": true}\n")
                .append("        );\n\n");
        }

        // 10. Marcadores de CLIENTES
        counterFG = 0;
        for (Map<String, Object> cli : finalClients) {
            String cname = cli.get("nombre") != null ? cli.get("nombre").toString() : "Cliente?";
            String gestorAsignado = cli.get("gestor_asignado") != null ? cli.get("gestor_asignado").toString() : "SIN_GESTOR";
            String fgVar = gestorToFeatureGroup.getOrDefault(gestorAsignado, gestorToFeatureGroup.get("SIN_GESTOR"));
            Object latObj = cli.get("lat");
            Object lonObj = cli.get("lon");
            if (latObj == null || lonObj == null) {
                System.out.println("[WARNING] Cliente sin coordenadas: " + cname);
                continue;
            }
            double lat = Double.parseDouble(latObj.toString());
            double lon = Double.parseDouble(lonObj.toString());
            String colorCli = cli.get("color") != null ? cli.get("color").toString() : "desconocido";
            String col = colorDict.getOrDefault(gestorAsignado, "#808080");
            String circleMarkerVar = "circle_marker_c_" + counterFG;
            counterFG++;
            html.append("        var ").append(circleMarkerVar).append(" = L.circleMarker(\n")
                .append("            [").append(lat).append(", ").append(lon).append("],\n")
                .append("            {\n")
                .append("                \"bubblingMouseEvents\": true,\n")
                .append("                \"color\": \"").append(col).append("\",\n")
                .append("                \"dashArray\": null,\n")
                .append("                \"dashOffset\": null,\n")
                .append("                \"fill\": true,\n")
                .append("                \"fillColor\": \"").append(col).append("\",\n")
                .append("                \"fillOpacity\": 0.7,\n")
                .append("                \"fillRule\": \"evenodd\",\n")
                .append("                \"lineCap\": \"round\",\n")
                .append("                \"lineJoin\": \"round\",\n")
                .append("                \"opacity\": 1.0,\n")
                .append("                \"radius\": 6,\n")
                .append("                \"stroke\": true,\n")
                .append("                \"weight\": 3\n")
                .append("            }\n")
                .append("        ).addTo(").append(fgVar).append(");\n\n")
                .append("        ").append(circleMarkerVar).append(".bindTooltip(\n")
                .append("            `<div>\n")
                .append("                 Cliente: ").append(cname)
                .append("<br>Gestor: ").append(gestorAsignado)
                .append("<br>Color: ").append(colorCli).append("\n")
                .append("             </div>`,\n")
                .append("            {\"sticky\": true}\n")
                .append("        );\n\n");
        }

        // 11. Añadir cada featureGroup al mapa
        html.append("        // Añadir cada feature_group al mapa\n");
        for (String fgVar : gestorToFeatureGroup.values()) {
            html.append("        ").append(fgVar).append(".addTo(map);\n");
        }

        // 12. Control de capas
        html.append("\n        var layer_control_config = {\n")
            .append("            base_layers : {\n")
            .append("                \"openstreetmap\" : tile_layer\n")
            .append("            },\n")
            .append("            overlays : {\n");
        int c = 0, total = gestorToFeatureGroup.size();
        for (Map.Entry<String, String> e : gestorToFeatureGroup.entrySet()) {
            html.append("                \"").append(e.getKey()).append("\" : ").append(e.getValue());
            if (c < total - 1) html.append(",");
            html.append("\n");
            c++;
        }
        html.append("            }\n")
            .append("        };\n\n")
            .append("        let layer_control_")
            .append(UUID.randomUUID().toString().replace("-", ""))
            .append(" = L.control.layers(\n")
            .append("            layer_control_config.base_layers,\n")
            .append("            layer_control_config.overlays,\n")
            .append("            {\n")
            .append("                \"autoZIndex\": true,\n")
            .append("                \"collapsed\": true,\n")
            .append("                \"position\": \"topright\"\n")
            .append("            }\n")
            .append("        ).addTo(map);\n\n");

        // 13. Leyenda (sin cambios en estructura original)
        StringBuilder legendItems = new StringBuilder();
        for (Map.Entry<String, String> entry : colorDict.entrySet()) {
            legendItems.append("<i style=\"background:")
                       .append(entry.getValue())
                       .append(";width:12px;height:12px;display:inline-block;margin-right:8px;\"></i>")
                       .append(entry.getKey()).append("<br>");
        }

        // 14. Cerrar tags y guardar
        html.append("    </script>\n")
            .append("</body>\n")
            .append("</html>\n");

        File mapaFile = new File(MAPA_HTML);
        mapaFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(mapaFile)) {
            writer.write(html.toString());
        }

        System.out.println("[OK] Mapa generado con la asignación corregida: " + MAPA_HTML);
        System.out.println("\n=== [Fin Paso 4: Corrección + Mapa] ===");
    }

    private boolean esMexico(double lat, double lon) {
        return lat >= 14.0 && lat <= 33.0
            && lon >= -119.0 && lon <= -85.0;
    }    

    private List<String> generateDistinctColors(int n) {
        List<String> colors = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            float hue = (float) i / n;
            int rgb = Color.HSBtoRGB(hue, 0.65f, 0.95f);
            String hex = String.format("#%06X", (rgb & 0x00FFFFFF));
            colors.add(hex);
        }
        return colors;
    }
}

