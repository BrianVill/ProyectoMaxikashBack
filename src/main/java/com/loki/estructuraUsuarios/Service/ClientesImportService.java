package com.loki.estructuraUsuarios.Service;

import com.loki.estructuraUsuarios.Models.Credito;
import com.loki.estructuraUsuarios.Models.CreditoPuestoPorSemana;
import com.loki.estructuraUsuarios.Models.EstadosAutomatizaciones;
import com.loki.estructuraUsuarios.Repository.CreditoPuestoPorSemanaRepository;
import com.loki.estructuraUsuarios.Repository.CreditoRepository;
import com.loki.tasks.StorageService;

import org.apache.poi.ss.usermodel.*;
import com.github.pjfanning.xlsx.StreamingReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Importa clientes de nivel 1 desde un Excel, asigna coordenadas mediante
 * geocodificación y crea la relación Crédito-Puesto-Semana.
 * Al finalizar actualiza la tabla <i>estados_automatizaciones</i> y envía
 * un correo con un resumen.
 */
@Service
public class ClientesImportService {

    /* ─────────────── constantes ─────────────── */
    private static final int BATCH_SIZE = 500;
    private static final int GEOCODE_THREAD_COUNT = 10;
    private static final String GOOGLE_API_KEY = "AIzaSyB2oudGwnMDhpyUsO6jGkiblGVlWDV5w1M";
    private static final String PROCESO = "Clientes";


    /* ─────────────── dependencias ───────────── */
    private final RestTemplate restTemplate = new RestTemplate();
    private final CreditoRepository creditoRepository;
    private final CreditoPuestoPorSemanaRepository cppsRepository;
    private final EstadosAutomatizacionesService estadosSvc;
    private final JavaMailSender mailSender;
    private final StorageService storageSvc;

    /* correo por defecto (application.properties) */
    @Value("${app.mail.to}")
    private String MAIL_TO;
    @Value("${spring.mail.username}")
    private String MAIL_FROM;

    @PersistenceContext
    private EntityManager entityManager;

    public ClientesImportService(CreditoRepository creditoRepository,
            CreditoPuestoPorSemanaRepository cppsRepository,
            EstadosAutomatizacionesService estadosSvc,
            JavaMailSender mailSender,
            StorageService storageSvc
            ) {
        this.creditoRepository = creditoRepository;
        this.cppsRepository = cppsRepository;
        this.estadosSvc = estadosSvc;
        this.mailSender = mailSender;
        this.storageSvc = storageSvc;
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * 0-A Wrapper original (para código legado)
     * ══════════════════════════════════════════════════════════════
     */
    @Async
    public void importarClientes(byte[] fileBytes) {
        importarClientes(fileBytes, null, null);
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * 0-B Wrapper NUEVO (permite cuerpo y destinatarios)
     * ══════════════════════════════════════════════════════════════
     */
    @Async
    public void importarClientes(byte[] fileBytes,
            String cuerpoEmail,
            List<String> destinos) {

        try (InputStream is = new ByteArrayInputStream(fileBytes)) {

            /* ——— core ——— */
            Resumen res = importarClientesInterno(is);

            String resumenStr = String.format(
                    "creados=%d  actualizados=%d  cppsNuevos=%d  cppsSaltados=%d",
                    res.creados, res.actualizados, res.cppsCreados, res.cppsSaltados);

            estadosSvc.actualizar(PROCESO, new EstadosAutomatizaciones(
                    null, PROCESO, "Completado", resumenStr, LocalDate.now()));

            String cuerpo = (cuerpoEmail == null || cuerpoEmail.isBlank())
                    ? "Importación de clientes finalizada.\n[" + resumenStr + "]"
                    : cuerpoEmail + "\n\n[" + resumenStr + "]";

            enviarCorreo("Importación Clientes – Completada", cuerpo, destinos);

        } catch (Exception e) {

            estadosSvc.actualizar(PROCESO, new EstadosAutomatizaciones(
                    null, PROCESO, "Error", e.getMessage(), LocalDate.now()));

            enviarCorreo("Importación Clientes – ERROR",
                    "El proceso terminó con error:\n\n" + e.getMessage(), destinos);

            System.err.printf("[ERROR] Importación de clientes: %s%n", e.getMessage());
        }
    }

    /**
     * Same as {@link #importarClientes(byte[], String, List)} but executes
     * synchronously. Useful for Cloud Tasks/Jobs where the request itself
     * performs the work.
     */
    public void importarClientesSync(byte[] fileBytes,
            String cuerpoEmail,
            List<String> destinos) {

        final String PROCESO = "Clientes";

        try (InputStream is = new ByteArrayInputStream(fileBytes)) {

            Resumen res = importarClientesInterno(is);

            String resumenStr = String.format(
                    "creados=%d  actualizados=%d  cppsNuevos=%d  cppsSaltados=%d",
                    res.creados, res.actualizados, res.cppsCreados, res.cppsSaltados);

            estadosSvc.actualizar(PROCESO, new EstadosAutomatizaciones(
                    null, PROCESO, "Completado", resumenStr, LocalDate.now()));

            String cuerpo = (cuerpoEmail == null || cuerpoEmail.isBlank())
                    ? "Importación de clientes finalizada.\n[" + resumenStr + "]"
                    : cuerpoEmail + "\n\n[" + resumenStr + "]";

            enviarCorreo("Importación Clientes – Completada", cuerpo, destinos);

        } catch (Exception e) {

            estadosSvc.actualizar(PROCESO, new EstadosAutomatizaciones(
                    null, PROCESO, "Error", e.getMessage(), LocalDate.now()));

            enviarCorreo("Importación Clientes – ERROR",
                    "El proceso terminó con error:\n\n" + e.getMessage(), destinos);

            System.err.printf("[ERROR] Importación de clientes: %s%n", e.getMessage());
        }
    }

    /* ═════════════════ envío de correo ═════════════════ */
    private void enviarCorreo(String asunto, String texto, List<String> destinosList) {

        List<String> base = Arrays.stream(MAIL_TO.split("[,;]"))
                .map(String::trim).filter(s -> !s.isBlank()).toList();

        List<String> extra = (destinosList == null) ? List.of()
                : destinosList.stream()
                        .flatMap(s -> Arrays.stream(s.split("[,;]")))
                        .map(String::trim).filter(s -> !s.isBlank()).toList();

        Set<String> all = new LinkedHashSet<>();
        all.addAll(base);
        all.addAll(extra);
        if (all.isEmpty())
            return;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(MAIL_FROM);
        msg.setTo(all.toArray(String[]::new));
        msg.setSubject(asunto);
        msg.setText(texto);
        mailSender.send(msg);
    }

    /*
     * ══════════════════════════════════════════════════════
     * 1. CORE – devuelve Resumen
     * ══════════════════════════════════════════════════════
     */
    private Resumen importarClientesInterno(InputStream excelStream) throws Exception {

        /* 1) Leer Excel y deduplicar */
        List<RawRow> rawRows = leerExcelClientes(excelStream);
        if (rawRows.isEmpty()) {
            System.out.println("[INFO] Excel vacío.");
            return new Resumen();
        }
        Map<String, RawRow> uniqueMap = new LinkedHashMap<>();
        for (RawRow r : rawRows)
            uniqueMap.putIfAbsent(r.idCredito, r);

        /* 2) Precargar créditos */
        Map<String, Credito> existing = creditoRepository.findAll().stream()
                .collect(Collectors.toMap(Credito::getId, Function.identity()));

        /* 3) Direcciones a geocodificar */
        Set<String> toGeocode = uniqueMap.values().stream()
                .filter(row -> {
                    Credito c = existing.get(row.idCredito);
                    if (c == null)
                        return true;
                    boolean lat0 = c.getLat() == null || c.getLat() == 0.0;
                    boolean lon0 = c.getLon() == null || c.getLon() == 0.0;
                    return lat0 && lon0;
                })
                .map(this::buildFullAddress)
                .collect(Collectors.toSet());

        /* 4) Geocodificar en paralelo */
        ConcurrentMap<String, double[]> cache = new ConcurrentHashMap<>();
        if (!toGeocode.isEmpty()) {
            ExecutorService pool = Executors.newFixedThreadPool(GEOCODE_THREAD_COUNT);
            List<Future<?>> futs = new ArrayList<>();
            for (String addr : toGeocode)
                futs.add(pool.submit(() -> cache.put(addr, geocodeAddress(addr))));
            for (Future<?> f : futs)
                f.get();
            pool.shutdown();
        }

        /* 5) Semana actual */
        LocalDate now = LocalDate.now();
        LocalDate start = now.with(DayOfWeek.MONDAY);
        LocalDate end = now.with(DayOfWeek.SUNDAY);

        /* 6) CPPS existentes de la semana */
        Set<String> cppsExist = cppsRepository
                .findByFechaInicioAndFechaFin(start, end)
                .stream().map(CreditoPuestoPorSemana::getCreditoId)
                .collect(Collectors.toSet());

        /* 7) Buffers y contadores */
        List<Credito> credBuf = new ArrayList<>(BATCH_SIZE);
        List<CreditoPuestoPorSemana> cppsBuf = new ArrayList<>(BATCH_SIZE);
        int nCre = 0, nUpd = 0, cppsNew = 0, cppsSkip = 0;

        /* 8) Recorrer filas */
        for (RawRow row : uniqueMap.values()) {

            String id = row.idCredito.trim();
            String address = buildFullAddress(row);

            /* —— coordenadas —— */
            double lat, lon;
            Credito ex = existing.get(id);
            if (ex == null ||
                    (ex.getLat() == null || ex.getLat() == 0) && (ex.getLon() == null || ex.getLon() == 0)) {
                double[] c = cache.getOrDefault(address, new double[] { 0, 0 });
                lat = c[0];
                lon = c[1];
            } else {
                lat = ex.getLat();
                lon = ex.getLon();
            }

            /* —— upsert crédito —— */
            Credito cr;
            if (ex != null) {
                cr = ex;
                if (row.nombre != null)
                    cr.setNombre(row.nombre);
                cr.setColor(row.colorCredito);
                if ((cr.getLat() == null || cr.getLat() == 0) && (cr.getLon() == null || cr.getLon() == 0)) {
                    cr.setLat(lat);
                    cr.setLon(lon);
                }
                nUpd++;
            } else {
                cr = new Credito();
                cr.setId(id);
                cr.setNombre(row.nombre != null ? row.nombre : id);
                cr.setColor(row.colorCredito);
                cr.setLat(lat);
                cr.setLon(lon);
                existing.put(id, cr);
                nCre++;
            }
            credBuf.add(cr);
            if (credBuf.size() >= BATCH_SIZE)
                flushCreditos(credBuf);

            /* —— upsert CPPS —— */
            if (cppsExist.contains(id)) {
                cppsSkip++;
            } else {
                CreditoPuestoPorSemana cpps = new CreditoPuestoPorSemana();
                cpps.setCreditoId(id);
                cpps.setPuestoId(null);
                cpps.setFechaInicio(start);
                cpps.setFechaFin(end);
                cppsBuf.add(cpps);
                cppsExist.add(id);
                cppsNew++;
            }
            if (cppsBuf.size() >= BATCH_SIZE)
                flushCpps(cppsBuf);
        }

        /* 9) flush final */
        flushCreditos(credBuf);
        flushCpps(cppsBuf);

        System.out.println("[OK] Import terminado:");
        System.out.printf("   Créditos creados:   %d%n", nCre);
        System.out.printf("   Créditos actualiz.: %d%n", nUpd);
        System.out.printf("   CPPS creados:       %d%n", cppsNew);
        System.out.printf("   CPPS saltados:      %d%n", cppsSkip);

        return new Resumen(nCre, nUpd, cppsNew, cppsSkip);
    }

    /* ═════════════ flush helpers ═════════════ */
    private void flushCreditos(List<Credito> batch) {
        if (batch.isEmpty())
            return;
        creditoRepository.saveAll(batch);
        entityManager.flush();
        entityManager.clear();
        batch.clear();
    }

    private void flushCpps(List<CreditoPuestoPorSemana> batch) {
        if (batch.isEmpty())
            return;
        cppsRepository.saveAll(batch);
        entityManager.flush();
        entityManager.clear();
        batch.clear();
    }

    /* ═════════════ Excel → RawRow ═════════════ */
    private List<RawRow> leerExcelClientes(InputStream excelStream) throws Exception {
        List<RawRow> list = new ArrayList<>();
        try (Workbook wb = StreamingReader.builder()
                .rowCacheSize(100)
                .bufferSize(4096)
                .open(excelStream)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null)
                return list;
            Iterator<Row> it = sheet.iterator();
            if (!it.hasNext())
                return list;
            Row header = it.next();

            Map<String, Integer> idx = new HashMap<>();
            for (Cell c : header)
                idx.put(c.getStringCellValue().trim().toLowerCase(), c.getColumnIndex());

            while (it.hasNext()) {
                Row row = it.next();
                if (row.getRowNum() == 0)
                    continue;

                RawRow rr = new RawRow();
                rr.idCredito = getString(row, idx.get("u_id_credito"));
                rr.nombre = getString(row, idx.get("u_nombre"));
                rr.colorCredito = parseColorDeCredito(getString(row, idx.get("v_tipo_de_crédito")));
                rr.calle = getString(row, idx.get("d_calle"));
                rr.colonia = getString(row, idx.get("d_colonia"));
                rr.municipio = getString(row, idx.get("d_municipio"));
                rr.estado = getString(row, idx.get("d_estado"));
                rr.cp = getString(row, idx.get("d_cp"));

                if (rr.idCredito != null && !rr.idCredito.isBlank())
                    list.add(rr);
            }
        }
        return list;
    }

    /* ═════════════ helpers generales ═════════════ */
    private String buildFullAddress(RawRow row) {
        List<String> parts = new ArrayList<>();
        if (row.calle != null && !row.calle.isBlank())
            parts.add(row.calle.trim());
        if (row.colonia != null && !row.colonia.isBlank())
            parts.add(row.colonia.trim());
        if (row.municipio != null && !row.municipio.isBlank())
            parts.add(row.municipio.trim());
        if (row.estado != null && !row.estado.isBlank())
            parts.add(row.estado.trim());
        if (row.cp != null && !row.cp.isBlank())
            parts.add(row.cp.trim());
        return String.join(", ", parts);
    }

    private double[] geocodeAddress(String address) {
        if (address == null || address.isBlank())
            return new double[] { 0.0, 0.0 };

        String url = String.format(
                "https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=%s",
                address.replace(" ", "+"), GOOGLE_API_KEY);

        try {
            Map<?, ?> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null || !"OK".equals(resp.get("status")))
                return new double[] { 0.0, 0.0 };

            Map<?, ?> geom = (Map<?, ?>) ((Map<?, ?>) ((List<?>) resp.get("results"))
                    .get(0)).get("geometry");
            Map<?, ?> loc = (Map<?, ?>) geom.get("location");
            return new double[] {
                    ((Number) loc.get("lat")).doubleValue(),
                    ((Number) loc.get("lng")).doubleValue()
            };
        } catch (Exception e) {
            return new double[] { 0.0, 0.0 };
        }
    }

    private String parseColorDeCredito(String tipo) {
        if (tipo == null)
            return null;
        String l = tipo.toLowerCase();
        if (l.contains("rojo"))
            return "rojo";
        if (l.contains("verde"))
            return "verde";
        if (l.contains("amarillo"))
            return "amarillo";
        return null;
    }

    private String getString(Row row, Integer idx) {
        if (idx == null)
            return null;
        Cell c = row.getCell(idx);
        if (c == null)
            return null;
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) c.getNumericCellValue());
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            case FORMULA -> c.getCellFormula();
            default -> null;
        };
    }

    /* ═════════════ clases internas ═════════════ */
    private static class RawRow {
        String idCredito, nombre, colorCredito,
                calle, colonia, municipio, estado, cp;
    }

    private record Resumen(int creados, int actualizados, int cppsCreados, int cppsSaltados) {
        Resumen() {
            this(0, 0, 0, 0);
        }
    }

    // ClientesImportService.java
    @Transactional
    public Exception processFromStorageWithRetry(String objectName,
            String cuerpoEmail,
            List<String> destinos) {
        Exception last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                byte[] excelBytes = storageSvc.read(objectName);
                importarClientesSync(excelBytes, cuerpoEmail, destinos); // ← ya existe
                return null; // ok ✅
            } catch (Exception e) {
                last = e;
                if (attempt < 3) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return ie;
                    }
                }
            }
        }
        // actualizar estado + mail de error
        estadosSvc.actualizar("Clientes",
                new EstadosAutomatizaciones(null, "Clientes", "Error",
                        last.getMessage(), LocalDate.now()));
        enviarCorreo("Importación Clientes – ERROR",
                "El proceso terminó con error:\n\n" + last.getMessage(), destinos);
        return last;
    }

}
