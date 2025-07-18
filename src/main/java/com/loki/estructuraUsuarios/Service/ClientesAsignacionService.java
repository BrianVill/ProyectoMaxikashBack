package com.loki.estructuraUsuarios.Service;

import com.loki.estructuraUsuarios.DTOs.UsuarioPorPuestoDTO;
import com.loki.estructuraUsuarios.Models.*;
import com.loki.estructuraUsuarios.Repository.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service @Slf4j
public class ClientesAsignacionService {

    /* ---------- repos ---------- */
    private final CreditoRepository creditoRepo;
    private final PuestoRepository  puestoRepo;
    private final CreditoPuestoPorSemanaRepository cppsRepo;
    private final UsuarioPuestoPorSemanaRepository uppsRepo;
    private final GenerarMapaService               generarMapaService;
    private final ReposicionarPuestosService       reposicionarPuestosService;
    private final EstadosAutomatizacionesService   estadosSvc;

    /* ---------- mail ---------- */
    private final JavaMailSender mailSender;
    @Value("${app.mail.to}")          private String MAIL_TO;
    @Value("${spring.mail.username}") private String MAIL_FROM;

    /* archivo que descarga el endpoint /clientes/asignacion/archivo */
    private static final String XLS_RESULT_PATH = "/tmp/uploads/Clientes.xlsx";

    public ClientesAsignacionService(
            CreditoRepository creditoRepo,
            PuestoRepository  puestoRepo,
            CreditoPuestoPorSemanaRepository cppsRepo,
            UsuarioPuestoPorSemanaRepository uppsRepo,
            GenerarMapaService generarMapaService,
            ReposicionarPuestosService reposicionarPuestosService,
            EstadosAutomatizacionesService estadosSvc,
            JavaMailSender mailSender) {

        this.creditoRepo        = creditoRepo;
        this.puestoRepo         = puestoRepo;
        this.cppsRepo           = cppsRepo;
        this.uppsRepo           = uppsRepo;
        this.generarMapaService = generarMapaService;
        this.reposicionarPuestosService = reposicionarPuestosService;
        this.estadosSvc         = estadosSvc;
        this.mailSender         = mailSender;
    }

    /* ------------------------------------------------------------------ */
    /* ---------------------------- WRAPPERS ---------------------------- */
    /* ------------------------------------------------------------------ */

    @Async
    public void asignarClientesAsync(double th, int cap, double maxTh) {
        asignarClientesAsync(th, cap, maxTh, null, null);
    }

    @Async
    public void asignarClientesAsync(double th, int cap, double maxTh,
                                     String cuerpo, List<String> to) {

        final String PROCESO = "Asignacion";
        try {
            ResumenAsignacion res = runAsignacion(th, cap, maxTh);

            String resumenStr = "total=%d  asignados=%d  sinAsignar=%d"
                               .formatted(res.total, res.asignados, res.sinAsignar);

            estadosSvc.actualizar(PROCESO,
                    new EstadosAutomatizaciones(null, PROCESO, "Completado",
                                                 resumenStr, LocalDate.now()));

            String cuerpoMail = (cuerpo == null || cuerpo.isBlank())
                              ? "Asignación completada.\n[" + resumenStr + "]"
                              : cuerpo + "\n\n[" + resumenStr + "]";
            enviarCorreo("Asignación Clientes – Completada", cuerpoMail, to);

        } catch (Exception e) {
            estadosSvc.actualizar(PROCESO,
                    new EstadosAutomatizaciones(null, PROCESO, "Error",
                                                 e.getMessage(), LocalDate.now()));
            enviarCorreo("Asignación Clientes – ERROR",
                         "El proceso terminó con error:\n\n" + e.getMessage(), to);
        }
    }

    public void asignarClientesSync(double th, int cap, double maxTh,
                                    String cuerpo, List<String> to) {

        final String PROCESO = "Asignacion";
        try {
            ResumenAsignacion res = runAsignacion(th, cap, maxTh);
            String resumenStr = "total=%d  asignados=%d  sinAsignar=%d"
                               .formatted(res.total, res.asignados, res.sinAsignar);

            estadosSvc.actualizar(PROCESO,
                    new EstadosAutomatizaciones(null, PROCESO, "Completado",
                                                 resumenStr, LocalDate.now()));

            String cuerpoMail = (cuerpo == null || cuerpo.isBlank())
                              ? "Asignación completada.\n[" + resumenStr + "]"
                              : cuerpo + "\n\n[" + resumenStr + "]";
            enviarCorreo("Asignación Clientes – Completada", cuerpoMail, to);

        } catch (Exception e) {
            estadosSvc.actualizar(PROCESO,
                    new EstadosAutomatizaciones(null, PROCESO, "Error",
                                                 e.getMessage(), LocalDate.now()));
            enviarCorreo("Asignación Clientes – ERROR",
                         "El proceso terminó con error:\n\n" + e.getMessage(), to);
        }
    }

    /* ------------------------------------------------------------------ */
    /* ------------------------- LÓGICA CENTRAL ------------------------- */
    /* ------------------------------------------------------------------ */

    @Transactional
    private ResumenAsignacion runAsignacion(double umbralDistInput,
                                            int    capacidadMaxInput,
                                            double maxDistInput) throws Exception {

        /* 1-a reposicionar puestos */
        reposicionarPuestosService.reposicionarConBalanceo(
                capacidadMaxInput, 20, 0, maxDistInput);

        /* 1-b semana actual */
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate sunday = LocalDate.now().with(DayOfWeek.SUNDAY);

        /* 2. registros sin gestor */
        Map<String, CreditoPuestoPorSemana> regSemana = cppsRepo
                .findAllByFechaInicioAndFechaFinAndPuestoIdIsNull(monday, sunday)
                .stream()
                .collect(Collectors.toMap(CreditoPuestoPorSemana::getCreditoId, r -> r));

        if (regSemana.isEmpty())
            return new ResumenAsignacion(new byte[0], 0, 0, 0);

       /* 3. créditos y gestores */
       List<Credito> creditos = creditoRepo.findAllById(regSemana.keySet());
       List<Puesto> gestores = puestoRepo.findByNivel(2);

       if (creditos.isEmpty() || gestores.isEmpty())
           return new ResumenAsignacion(new byte[0], 0, 0, regSemana.size());

       /* 4. gestores disponibles hoy */
       Map<UUID, UsuarioPorPuestoDTO> usuarioPorPuesto = uppsRepo
               .findDisponibles(LocalDate.now())
               .stream()
               .collect(Collectors.toMap(
                       UsuarioPorPuestoDTO::puestoId,
                       dto -> dto,
                       (a, b) -> {
                           log.warn("Duplicate usuario en puesto {} ({} -> {})", a.puestoId(), a.usuarioId(), b.usuarioId());
                           return b;
                       },
                       LinkedHashMap::new));

       gestores = gestores.stream()
               .filter(g -> usuarioPorPuesto.containsKey(g.getId()))
               .toList();
               
        /* 5. conteo actual asignados */
        Map<UUID, AtomicInteger> asignCount = new ConcurrentHashMap<>();
        gestores.forEach(g -> asignCount.put(g.getId(), new AtomicInteger(0)));

        uppsRepo.contarAsignados(monday, sunday)
                .forEach(r -> asignCount.get((UUID) r[0])
                                        .set(((Number) r[1]).intValue()));

        /* 6. algoritmo de asignación */
        record PD(double dist, Puesto puesto) {}
        double UMBRAL_DIST = umbralDistInput;
        double MAX_DIST    = maxDistInput;
        int    CAP_MAX     = capacidadMaxInput;
        double delta       = MAX_DIST / 111d;

        Map<Credito,List<PD>> cand = new HashMap<>();
        Map<String,Double>    minD = new HashMap<>();

        for (Credito c : creditos) {
            List<PD> list = new ArrayList<>();
            double   m    = MAX_DIST - UMBRAL_DIST;

            for (Puesto g : gestores) {
                if (Math.abs(g.getLat() - c.getLat()) > delta) continue;
                if (Math.abs(g.getLon() - c.getLon()) > delta) continue;

                double d = haversine(g.getLat(), g.getLon(), c.getLat(), c.getLon());
                if (d <= MAX_DIST) {
                    list.add(new PD(d, g));
                    if (d < m) m = d;
                }
            }
            cand.put(c, list);
            minD.put(c.getId(), m);
        }

        Map<Credito,List<PD>> orden = cand.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().size()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        orden.forEach((credito, lista) -> {
            if (lista.isEmpty()) return;

            int    bestScore = 0;
            double bestDist  = MAX_DIST;
            UUID   bestPuesto = null;

            double umbral = minD.get(credito.getId()) + UMBRAL_DIST;

            for (PD pd : lista) {
                UUID puestoId = pd.puesto().getId();
                if (asignCount.get(puestoId).get() >= CAP_MAX) continue;
                if (pd.dist() > umbral)                        continue;

                UsuarioPorPuestoDTO dto = usuarioPorPuesto.get(puestoId);
                if (dto == null) continue;

                int score = obtenerPuntuacion(dto.color(), credito.getColor());
                if (score < bestScore)                             continue;
                if (score == bestScore && pd.dist() >= bestDist)   continue;

                bestScore  = score;
                bestDist   = pd.dist();
                bestPuesto = puestoId;
            }

            if (bestPuesto != null) {
                regSemana.get(credito.getId()).setPuestoId(bestPuesto);
                asignCount.get(bestPuesto).incrementAndGet();
            }
        });

        /* 7. persistir */
        cppsRepo.saveAll(regSemana.values());

        /* 8. generar XLS streaming + mapa */
        byte[] bytes = generarExcelStreaming(creditos, regSemana, usuarioPorPuesto);
        generarMapaService.GenerarMapa();

        /* 9. guardar archivo para el endpoint /archivo */
        new File(XLS_RESULT_PATH).getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(XLS_RESULT_PATH)) {
            fos.write(bytes);
        }

        long asignados  = regSemana.values().stream()
                                   .filter(r -> r.getPuestoId() != null).count();
        long sinAsignar = regSemana.size() - asignados;

        return new ResumenAsignacion(bytes, regSemana.size(),
                                     (int) asignados, (int) sinAsignar);
    }

    /* ------------------------------------------------------------------ */
    /* ---------------------- helpers utilitarios ----------------------- */
    /* ------------------------------------------------------------------ */

    private double haversine(double lat1,double lon1,double lat2,double lon2){
        double R=6371.0;
        double dLat=Math.toRadians(lat2-lat1);
        double dLon=Math.toRadians(lon2-lon1);
        double a=Math.sin(dLat/2)*Math.sin(dLat/2)
                +Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                *Math.sin(dLon/2)*Math.sin(dLon/2);
        return 2*R*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
    }

    private boolean esMexico(double lat, double lon) {
        return lat >= 14.0 && lat <= 33.0
            && lon >= -119.0 && lon <= -85.0;
    }

    private int obtenerPuntuacion(String colG,String colC){
        if(colG==null) colG="amarillo";
        if(colC==null) colC="amarillo";
        return switch(colG.toLowerCase()+","+colC.toLowerCase()){
            case "verde,rojo","rojo,verde"        ->3;
            case "verde,amarillo","amarillo,verde"->2;
            case "amarillo,amarillo"              ->3;
            case "verde,verde","rojo,rojo"        ->1;
            case "amarillo,rojo","rojo,amarillo"  ->2;
            default                               ->0;
        };
    }

    /* ---------- Excel streaming ---------- */
    private byte[] generarExcelStreaming(List<Credito> creditos,
                                         Map<String, CreditoPuestoPorSemana> regSemana,
                                         Map<UUID, UsuarioPorPuestoDTO> usuarioPorPuesto) throws IOException {

        try (SXSSFWorkbook wb = new SXSSFWorkbook(null, 100, true, true);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet sh = wb.createSheet("Asignación");
            Row header = sh.createRow(0);
            setCell(header, 0, "u_ID_Credito");
            setCell(header, 1, "Gestor");

            int r = 1;
            for (Credito c : creditos) {
                Row row = sh.createRow(r++);
                setCell(row, 0, c.getId());

                UUID pid = regSemana.get(c.getId()).getPuestoId();
                if (pid != null) {
                    UsuarioPorPuestoDTO dto = usuarioPorPuesto.get(pid);
                    if (dto != null) setCell(row, 1, dto.nombre());
                }
            }
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private void setCell(Row row, int col, String val){
        Cell c = row.getCell(col);
        if (c == null) c = row.createCell(col);
        c.setCellValue(val);
    }

    /* ------------------------------------------------------------------ */
    /* -------------------------- envío mail ---------------------------- */
    /* ------------------------------------------------------------------ */
    private void enviarCorreo(String asunto,String texto,List<String> dest){
        List<String> base = Arrays.stream(MAIL_TO.split("[,;]"))
                                  .map(String::trim).filter(s->!s.isBlank()).toList();
        List<String> extra = (dest==null)?List.of()
                : dest.stream()
                      .flatMap(s->Arrays.stream(s.split("[,;]")))
                      .map(String::trim).filter(s->!s.isBlank()).toList();
        Set<String> all = new LinkedHashSet<>();
        all.addAll(base); all.addAll(extra);
        if(all.isEmpty()) return;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(MAIL_FROM);
        msg.setTo(all.toArray(String[]::new));
        msg.setSubject(asunto);
        msg.setText(texto);
        mailSender.send(msg);
    }

    /* resumen numérico */
    private record ResumenAsignacion(byte[] bytes,int total,int asignados,int sinAsignar){}
}
