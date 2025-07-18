package com.loki.estructuraUsuarios.Service;

import com.loki.estructuraUsuarios.Models.*;
import com.loki.estructuraUsuarios.Repository.*;
import com.github.pjfanning.xlsx.StreamingReader;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import com.loki.tasks.StorageService;
import java.io.IOException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Importa jerarquías de gestores y crea/actualiza:
 * • Puestos
 * • Usuarios
 * • Vinculación Usuario-Puesto por Semana
 * Además actualiza la tabla de estados y envía e-mail con el resumen.
 */
@Service
public class GestoresImportService {

    /* ───────── repos ───────── */
    private final NivelRepository nivelRepository;
    private final PuestoRepository puestoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioPuestoPorSemanaRepository uppsRepository;

    /* ───────── extras ───────── */
    private final EstadosAutomatizacionesService estadosSvc;
    private final JavaMailSender mailSender;
    private final JdbcTemplate jdbc;
    private final StorageService storageSvc; // por si lo necesitas luego

    @Value("${app.mail.to}")
    private String MAIL_TO;
    @Value("${spring.mail.username}")
    private String MAIL_FROM;

    @PersistenceContext
    private EntityManager em;

    private static final String ESTADO_NOMBRE = "Gestor";

    public GestoresImportService(NivelRepository nivelRepository,
            PuestoRepository puestoRepository,
            UsuarioRepository usuarioRepository,
            UsuarioPuestoPorSemanaRepository uppsRepository,
            EstadosAutomatizacionesService estadosSvc,
            JavaMailSender mailSender,
            JdbcTemplate jdbc,
            StorageService storageSvc) {
        this.nivelRepository = nivelRepository;
        this.puestoRepository = puestoRepository;
        this.usuarioRepository = usuarioRepository;
        this.uppsRepository = uppsRepository;
        this.estadosSvc = estadosSvc;
        this.mailSender = mailSender;
        this.jdbc = jdbc;
        this.storageSvc = storageSvc;
    }

    /*
     * ═══════════════════════════════════════
     * 0-A wrapper original (sin params extra)
     * ═══════════════════════════════════════
     */
    @Async
    public void processExcelHierarchyBulkAsync(byte[] hierarchyBytes, byte[] mx1Bytes) {
        processExcelHierarchyBulkAsync(hierarchyBytes, mx1Bytes, null, null);
    }

    /*
     * ═══════════════════════════════════════
     * 0-B wrapper nuevo (mensaje + destinos)
     * ═══════════════════════════════════════
     */
    @Async
    public void processExcelHierarchyBulkAsync(byte[] hierarchyBytes, byte[] mx1Bytes,
            String cuerpoEmail, List<String> destinos) {
        try (InputStream hierarchyIs = new ByteArrayInputStream(hierarchyBytes);
                InputStream mx1Is = new ByteArrayInputStream(mx1Bytes)) {

            Resumen res = processExcelHierarchyBulk(hierarchyIs, mx1Is);

            String resumenStr = String.format(
                    "puestos=%d  usuarios=%d  vinculos=%d  warnLatLon=%d",
                    res.puestos, res.usuarios, res.vinculos, res.warnLatLon);

            estadosSvc.actualizar(ESTADO_NOMBRE, new EstadosAutomatizaciones(
                    null, ESTADO_NOMBRE, "Completado", resumenStr, LocalDate.now()));

            String cuerpo = (cuerpoEmail == null || cuerpoEmail.isBlank())
                    ? "Importación de gestores finalizada.\n[" + resumenStr + "]"
                    : cuerpoEmail + "\n\n[" + resumenStr + "]";

            enviarCorreo("Importación Gestores – Completada", cuerpo, destinos);

        } catch (Exception e) {
            estadosSvc.actualizar(ESTADO_NOMBRE, new EstadosAutomatizaciones(
                    null, ESTADO_NOMBRE, "Error", e.getMessage(), LocalDate.now()));

            enviarCorreo("Importación Gestores – ERROR",
                    "El proceso terminó con error:\n\n" + e.getMessage(), destinos);
        }
    }

    /**
     * Same as {@link #processExcelHierarchyBulkAsync(byte[], byte[], String, List)}
     * but executes
     * synchronously. Useful for Cloud Tasks/Jobs where the request itself should
     * perform the work.
     */
    public void processExcelHierarchyBulkSync(byte[] hierarchyBytes, byte[] mx1Bytes,
            String cuerpoEmail, List<String> destinos) {
        try {
            processExcelHierarchyBulkSyncOnce(hierarchyBytes, mx1Bytes, cuerpoEmail, destinos);
        } catch (Exception e) {
            estadosSvc.actualizar(ESTADO_NOMBRE, new EstadosAutomatizaciones(
                    null, ESTADO_NOMBRE, "Error", e.getMessage(), LocalDate.now()));

            enviarCorreo("Importación Gestores – ERROR",
                    "El proceso terminó con error:\n\n" + e.getMessage(), destinos);
        }
    }

    private void processExcelHierarchyBulkSyncOnce(byte[] hierarchyBytes, byte[] mx1Bytes,
            String cuerpoEmail, List<String> destinos) throws Exception {

        try (InputStream hierarchyIs = new ByteArrayInputStream(hierarchyBytes);
                InputStream mx1Is = new ByteArrayInputStream(mx1Bytes)) {

            Resumen res = processExcelHierarchyBulk(hierarchyIs, mx1Is);

            String resumenStr = String.format(
                    "puestos=%d  usuarios=%d  vinculos=%d  warnLatLon=%d",
                    res.puestos, res.usuarios, res.vinculos, res.warnLatLon);

            estadosSvc.actualizar(ESTADO_NOMBRE, new EstadosAutomatizaciones(
                    null, ESTADO_NOMBRE, "Completado", resumenStr, LocalDate.now()));

            String cuerpo = (cuerpoEmail == null || cuerpoEmail.isBlank())
                    ? "Importación de gestores finalizada.\n[" + resumenStr + "]"
                    : cuerpoEmail + "\n\n[" + resumenStr + "]";

            enviarCorreo("Importación Gestores – Completada", cuerpo, destinos);
        }
    }

    /**
     * Reads the hierarchy Excel from Cloud Storage with retries and processes it.
     * Errors are reported only after all attempts fail.
     *
     * @return {@code null} on success or the last thrown exception on failure.
     */
    public Exception processFromStorageWithRetry(String objectName,
                                                 String cuerpoEmail,
                                                 List<String> destinos) {
        byte[] mx1Bytes;
        try {
            mx1Bytes = StreamUtils.copyToByteArray(
                    new ClassPathResource("uploads/MX1.xlsx").getInputStream());
        } catch (IOException ioe) {
            estadosSvc.actualizar(ESTADO_NOMBRE, new EstadosAutomatizaciones(
                    null, ESTADO_NOMBRE, "Error", ioe.getMessage(), LocalDate.now()));
            enviarCorreo("Importación Gestores – ERROR",
                         "El proceso terminó con error:\n\n" + ioe.getMessage(), destinos);
            return ioe;
        }

        Exception last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                byte[] hierarchyBytes = storageSvc.read(objectName);
                processExcelHierarchyBulkSyncOnce(hierarchyBytes, mx1Bytes, cuerpoEmail, destinos);
                return null; // success
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

        estadosSvc.actualizar(ESTADO_NOMBRE, new EstadosAutomatizaciones(
                null, ESTADO_NOMBRE, "Error", last.getMessage(), LocalDate.now()));
        enviarCorreo("Importación Gestores – ERROR",
                     "El proceso terminó con error:\n\n" + last.getMessage(), destinos);
        return last;
    }


    /* ═════════════ envío de correo (varios destinatarios) ═════════════ */
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

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(MAIL_FROM);
        mail.setTo(all.toArray(String[]::new));
        mail.setSubject(asunto);
        mail.setText(texto);
        mailSender.send(mail);
    }

    /*
     * ═══════════════════════════════════════
     * 1. PROCESO PRINCIPAL (devuelve Resumen)
     * ═══════════════════════════════════════
     */
    @Transactional
    public Resumen processExcelHierarchyBulk(InputStream excelHierarchyStream,
            InputStream excelMx1Stream) throws Exception {

        /* 1. niveles ≠1 descendente */
        List<Nivel> niveles = nivelRepository.findAll().stream()
                .filter(n -> n.getNivel() != 1)
                .sorted(Comparator.comparingInt(Nivel::getNivel).reversed())
                .collect(Collectors.toList());

        /* 2. CP → lat/lon */
        Map<String, List<LatLon>> cpToLatLonMap = readCpLatLonFromMx1(excelMx1Stream);

        /* 3. jerarquía */
        List<RawRow> rows = readHierarchyExcel(excelHierarchyStream, niveles);
        if (rows.isEmpty()) {
            System.out.println("[INFO] Excel vacío.");
            return new Resumen();
        }

        /* 4. estructuras in-memory */
        Map<String, PuestoData> puestoMap = new LinkedHashMap<>();
        Map<String, UserData> userMap = new LinkedHashMap<>();
        Set<UppsKey> uppsSet = new HashSet<>();
        Map<String, ChildCounters> childCount = new HashMap<>();
        Map<String, List<LatLon>> latLonAcc = new HashMap<>();

        /* 5. recorrer filas */
        for (RawRow row : rows) {
            if (row.levelValues.values().stream().allMatch(this::isEmpty))
                continue;

            String parentCode = "";
            for (Nivel nv : niveles) {
                String lvlName = nv.getNombre().toLowerCase();
                String userName = row.levelValues.get(lvlName);

                String childCode = findOrCreateChildPuestoCode(
                        parentCode, nv, userName, puestoMap, childCount);

                if (!isEmpty(userName)) {
                    String userKey = buildUserKey(userName, nv.getNivel());
                    userMap.computeIfAbsent(userKey, k -> {
                        UserData ud = new UserData();
                        ud.name = userName;
                        if (nv.getNivel() == 2 && !isEmpty(row.color))
                            ud.color = row.color.trim();
                        return ud;
                    });
                    uppsSet.add(new UppsKey(userKey, childCode));

                    if (nv.getNombre().equalsIgnoreCase("gestor") && !isEmpty(row.cp)) {
                        latLonAcc.computeIfAbsent(childCode, k -> new ArrayList<>())
                                .addAll(cpToLatLonMap.getOrDefault(
                                        normalizeCP(row.cp), List.of()));
                    }
                }
                parentCode = childCode;
            }
        }

        /* 6. promedio lat/lon */
        int warnLat = 0;
        for (var e : latLonAcc.entrySet()) {
            PuestoData pd = puestoMap.get(e.getKey());
            double avgLat = e.getValue().stream().mapToDouble(l -> l.lat).average().orElse(0);
            double avgLon = e.getValue().stream().mapToDouble(l -> l.lon).average().orElse(0);
            pd.lat = avgLat;
            pd.lon = avgLon;
            if (avgLat == 0 || avgLon == 0)
                warnLat++;
        }

        /* 7. UPSERT Puestos */
        List<PuestoData> puestosSorted = new ArrayList<>(puestoMap.values());
        puestosSorted.sort(Comparator.comparingInt(p -> p.code.length()));

        for (PuestoData pd : puestosSorted) {
            String parentCode = (pd.code.length() > 4) ? pd.code.substring(0, pd.code.length() - 4) : null;
            UUID parentId = (parentCode != null && puestoMap.containsKey(parentCode))
                    ? puestoMap.get(parentCode).dbId
                    : null;

            Puesto ent = puestoRepository.findByNombre(pd.code).orElse(new Puesto());
            ent.setNombre(pd.code);
            ent.setLat(pd.lat);
            ent.setLon(pd.lon);
            ent.setNivel(pd.nivelInt);
            ent.setIdPadreDirecto(parentId);
            puestoRepository.save(ent);
            pd.dbId = ent.getId();
        }

        /* 8. UPSERT Usuarios */
        for (UserData ud : userMap.values()) {
            Usuario u = usuarioRepository.findByNombre(ud.name).orElse(new Usuario());
            u.setNombre(ud.name);
            if (!isEmpty(ud.color))
                u.setColor(ud.color);
            usuarioRepository.save(u);
            ud.dbId = u.getId();
        }

        /* 9. UPSERT Usuario-Puesto Semana */
        LocalDate now = LocalDate.now();
        LocalDate start = now.with(DayOfWeek.MONDAY);
        LocalDate end = now.with(DayOfWeek.SUNDAY);

        int vinc = 0;
        for (UppsKey uk : uppsSet) {
            UserData ud = userMap.get(uk.userKey);
            PuestoData pd = puestoMap.get(uk.puestoCode);
            if (ud == null || pd == null || ud.dbId == null || pd.dbId == null)
                continue;

            if (uppsRepository.existsByUsuarioIdAndFechaInicio(ud.dbId, start))
                continue;

            UsuarioPuestoPorSemana rel = new UsuarioPuestoPorSemana();
            rel.setUsuarioId(ud.dbId);
            rel.setPuestoId(pd.dbId);
            rel.setFechaInicio(start);
            rel.setFechaFin(end);
            uppsRepository.save(rel);
            vinc++;
        }

        System.out.printf("[OK] Import terminado – %d puestos, %d usuarios, %d vínculos.%n",
                puestoMap.size(), userMap.size(), vinc);

        return new Resumen(puestoMap.size(), userMap.size(), vinc, warnLat);
    }

    /*
     * =========================================================================
     * LECTURA DE EXCEL (jerarquía) – compatible con StreamingReader
     * =========================================================================
     */
    private List<RawRow> readHierarchyExcel(InputStream is, List<Nivel> niveles) throws Exception {
        List<RawRow> out = new ArrayList<>();

        try (Workbook wb = StreamingReader.builder()
                .rowCacheSize(100) // filas en RAM
                .bufferSize(4096) // bytes por bloque
                .open(is)) {

            Sheet sh = wb.getSheetAt(0);
            if (sh == null)
                return out;

            /* --- encabezado --- */
            Iterator<Row> it = sh.iterator();
            if (!it.hasNext())
                return out; // archivo vacío
            Row header = it.next();

            Map<String, Integer> colIdx = new HashMap<>();
            for (Cell c : header) {
                colIdx.put(c.getStringCellValue().trim().toLowerCase(), c.getColumnIndex());
            }

            Integer cpCol = colIdx.get("cp");
            Integer colorCol = colIdx.get("color");

            /* --- filas de datos --- */
            while (it.hasNext()) {
                Row row = it.next();

                RawRow rr = new RawRow();
                rr.levelValues = new HashMap<>();

                rr.cp = (cpCol != null) ? getCellString(row, cpCol) : null;
                rr.color = (colorCol != null) ? parseColorDeCredito(getCellString(row, colorCol)) : null;

                for (Nivel nv : niveles) {
                    String key = nv.getNombre().toLowerCase();
                    Integer pos = colIdx.get(key);
                    rr.levelValues.put(key, (pos != null) ? getCellString(row, pos) : null);
                }
                out.add(rr);
            }
        }
        return out;
    }

    /*
     * =========================================================================
     * MX1 (CP → lat/lon) – compatible con StreamingReader
     * =========================================================================
     */
    private Map<String, List<LatLon>> readCpLatLonFromMx1(InputStream is) throws Exception {
        Map<String, List<LatLon>> map = new HashMap<>();
        if (is == null)
            return map;

        try (Workbook wb = StreamingReader.builder()
                .rowCacheSize(100)
                .bufferSize(4096)
                .open(is)) {

            Sheet sh = wb.getSheetAt(0);
            if (sh == null)
                return map;

            Iterator<Row> it = sh.iterator();
            if (!it.hasNext())
                return map; // sin filas
            it.next(); // descarta encabezado

            while (it.hasNext()) {
                Row row = it.next();

                String cp = getCellString(row, 0);
                double lat = getDouble(row.getCell(1));
                double lon = getDouble(row.getCell(2));

                if (isEmpty(cp))
                    continue;

                map.computeIfAbsent(normalizeCP(cp), k -> new ArrayList<>())
                        .add(new LatLon(lat, lon));
            }
        }
        return map;
    }

    /* ---------- helpers originales ---------- */
    private String findOrCreateChildPuestoCode(String parentCode, Nivel nv, String userName,
            Map<String, PuestoData> pMap,
            Map<String, ChildCounters> counters) {

        String key = isEmpty(userName)
                ? nv.getNombre() + "_ANON_" + parentCode
                : nv.getNombre() + "_" + userName + "_" + parentCode;

        if (pMap.containsKey(key))
            return pMap.get(key).code;

        counters.putIfAbsent(parentCode, new ChildCounters());
        int seq = counters.get(parentCode).nextVal++;
        String code = parentCode + getLetterFromNombre(nv.getNombre()) + format3(seq);

        PuestoData pd = new PuestoData(code, nv.getNivel());
        pMap.put(code, pd);
        pMap.put(key, pd);
        return code;
    }

    private String buildUserKey(String name, int lvl) {
        return name.trim().toUpperCase() + "|" + lvl;
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String getCellString(Row row, int idx) {
        Cell c = row.getCell(idx);
        if (c == null)
            return null;
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> String.format("%05.0f", c.getNumericCellValue());
            default -> null;
        };
    }

    private String normalizeCP(String cp) {
        if (cp == null)
            return "";
        cp = cp.trim();
        while (cp.length() < 5)
            cp = "0" + cp;
        return cp;
    }

    private double getDouble(Cell c) {
        if (c == null)
            return 0;
        return switch (c.getCellType()) {
            case NUMERIC -> c.getNumericCellValue();
            case STRING -> parseDouble(c.getStringCellValue(), 0);
            default -> 0;
        };
    }

    private double parseDouble(String v, double def) {
        try {
            return Double.parseDouble(v);
        } catch (Exception e) {
            return def;
        }
    }

    private char getLetterFromNombre(String n) {
        return Character.toUpperCase(n.charAt(0));
    }

    private String format3(int n) {
        return String.format("%03d", n);
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

    /* ---------- clases internas originales ---------- */
    private static class RawRow {
        String cp;
        String color;
        Map<String, String> levelValues;
    }

    private static class LatLon {
        double lat, lon;

        LatLon(double la, double lo) {
            lat = la;
            lon = lo;
        }
    }

    private static class ChildCounters {
        int nextVal = 1;
    }

    private static class PuestoData {
        String code;
        int nivelInt;
        double lat, lon;
        UUID dbId;

        PuestoData(String c, int n) {
            code = c;
            nivelInt = n;
        }
    }

    private static class UserData {
        String name;
        String color;
        UUID dbId;
    }

    private static class UppsKey {
        String userKey, puestoCode;

        UppsKey(String u, String p) {
            userKey = u;
            puestoCode = p;
        }

        public boolean equals(Object o) {
            return (o instanceof UppsKey k)
                    && userKey.equals(k.userKey) && puestoCode.equals(k.puestoCode);
        }

        public int hashCode() {
            return Objects.hash(userKey, puestoCode);
        }
    }

    /* ---------- resumen interno ---------- */
    private record Resumen(int puestos, int usuarios, int vinculos, int warnLatLon) {
        Resumen() {
            this(0, 0, 0, 0);
        }
    }
}
