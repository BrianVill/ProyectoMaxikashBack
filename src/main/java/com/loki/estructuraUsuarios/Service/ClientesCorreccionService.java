package com.loki.estructuraUsuarios.Service;

import com.github.pjfanning.xlsx.StreamingReader;
import com.loki.estructuraUsuarios.Models.*;
import com.loki.estructuraUsuarios.Repository.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientesCorreccionService {

    /* ----------------------- repos / helpers ----------------------- */
    private final CreditoRepository creditoRepo;
    private final PuestoRepository puestoRepo;
    private final UsuarioRepository usuarioRepo;
    private final UsuarioPuestoPorSemanaRepository uppsRepo;
    private final CreditoPuestoPorSemanaRepository cppsRepo;
    private final GenerarMapaService generarMapaService;
    private final EstadosAutomatizacionesService estadosSvc;
    private final ExcelClientesHelper excelHelper;

    /* ---------- mail ---------- */
    private final JavaMailSender mailSender;
    @Value("${app.mail.to}")
    private String MAIL_TO;
    @Value("${spring.mail.username}")
    private String MAIL_FROM;

    private static final String ESTADO_NOMBRE = "Correccion";
    private static final String XLS_RESULT_PATH = "/tmp/uploads/Clientes.xlsx";

    @PersistenceContext
    private EntityManager entityManager;

    /*
     * =====================================================================
     * WRAPPERS ASYNC
     * =====================================================================
     */
    @Async
    @Transactional
    public void corregirAsignacionesAsync(byte[] fileBytes) {
        corregirAsignacionesAsync(fileBytes, null, null);
    }

    @Async
    @Transactional
    public void corregirAsignacionesAsync(byte[] fileBytes,
            String cuerpoEmail,
            List<String> destinos) {

        try (var bis = new ByteArrayInputStream(fileBytes)) {

            ResumenCorreccion res = runCorreccion(bis);

            String resumenStr = "procesados=%d  actualizados=%d  sinCambio=%d"
                    .formatted(res.total, res.actualizados, res.sinCambio);

            estadosSvc.actualizar(ESTADO_NOMBRE,
                    new EstadosAutomatizaciones(null, ESTADO_NOMBRE,
                            "Completado", resumenStr, LocalDate.now()));

            enviarCorreoOk(resumenStr, cuerpoEmail, destinos);

        } catch (Exception e) {
            manejarError(e, cuerpoEmail, destinos);
        }
    }

    /*
     * =====================================================================
     * WRAPPER SYNC (Cloud Tasks)
     * =====================================================================
     */
    @Transactional
    public void corregirAsignacionesSync(byte[] fileBytes,
            String cuerpoEmail,
            List<String> destinos) {

        try (var bis = new ByteArrayInputStream(fileBytes)) {

            ResumenCorreccion res = runCorreccion(bis);

            String resumenStr = "procesados=%d  actualizados=%d  sinCambio=%d"
                    .formatted(res.total, res.actualizados, res.sinCambio);

            estadosSvc.actualizar(ESTADO_NOMBRE,
                    new EstadosAutomatizaciones(null, ESTADO_NOMBRE,
                            "Completado", resumenStr, LocalDate.now()));

            enviarCorreoOk(resumenStr, cuerpoEmail, destinos);

        } catch (Exception e) {
            manejarError(e, cuerpoEmail, destinos);
            throw new RuntimeException(e); // para que Cloud Tasks registre el fallo
        }
    }

    /*
     * =====================================================================
     * 1) LÓGICA CENTRAL (una sola transacción)
     * =====================================================================
     */
    @Transactional
    private ResumenCorreccion runCorreccion(InputStream excelStream) throws Exception {

        /* 1. Semana actual */
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate sunday = LocalDate.now().with(DayOfWeek.SUNDAY);

        /* 2. Cargar CPPS de la semana */
        Map<String, CreditoPuestoPorSemana> cppsByCred = cppsRepo
                .findAllByFechaInicioAndFechaFin(monday, sunday).stream()
                .collect(Collectors.toMap(CreditoPuestoPorSemana::getCreditoId, c -> c));

        /* 3. Precargar créditos */
        Map<String, Credito> creditoById = creditoRepo
                .findAllById(cppsByCred.keySet()).stream()
                .collect(Collectors.toMap(Credito::getId, c -> c));

        /* 4. Gestor → puesto */
        Map<String, UUID> gestorToPuesto = buildNameToPuestoMap(monday, sunday);

        /* 5. Leer Excel en streaming (muy poca RAM) */
        List<RowData> rows = readCorreccionStreaming(excelStream);
        int totalRows = rows.size();

        /* 6. Detectar cambios */
        List<CreditoPuestoPorSemana> toSave = new ArrayList<>();

        for (RowData rd : rows) {

            String credId = rd.creditoId().trim();
            if (!creditoById.containsKey(credId))
                continue;

            UUID nuevoPuesto = Optional.ofNullable(normalize(rd.gestorName()))
                    .filter(s -> !s.isEmpty())
                    .map(gestorToPuesto::get)
                    .orElse(null);

            CreditoPuestoPorSemana cpps = cppsByCred.computeIfAbsent(
                    credId, k -> createNewCpps(k, monday, sunday));

            if (Objects.equals(cpps.getPuestoId(), nuevoPuesto))
                continue; // sin cambio

            cpps.setPuestoId(nuevoPuesto);
            toSave.add(cpps);
        }

        /* 7. Persistir sólo modificados */
        int updated = toSave.size();
        if (updated > 0) {
            toSave.forEach(c -> cppsRepo.updatePuesto(
                    c.getCreditoId(), c.getPuestoId(),
                    c.getFechaInicio(), c.getFechaFin()));

            entityManager.flush();
            entityManager.clear();

            refrescarGestorEnExcel(monday, sunday); // mantiene Excel maestro al día
        }

        generarMapaService.GenerarMapa();

        return new ResumenCorreccion(totalRows, updated, totalRows - updated);
    }

    /*
     * =====================================================================
     * Lectura streaming del Excel de corrección (sin for-each)
     * =====================================================================
     */
    private List<RowData> readCorreccionStreaming(InputStream is) throws IOException {

        List<RowData> list = new ArrayList<>();

        try (Workbook wb = StreamingReader.builder()
                .rowCacheSize(100)
                .bufferSize(4096)
                .open(is)) {

            Sheet sh = wb.getSheetAt(0);
            if (sh == null)
                return list;

            Iterator<Row> it = sh.iterator();
            if (!it.hasNext())
                return list; // archivo vacío
            Row header = it.next();

            int colIdCred = findColumn(header, "u_ID_Credito");
            int colGestor = findColumn(header, "Gestor");
            if (colIdCred < 0 || colGestor < 0)
                throw new IllegalStateException(
                        "El Excel debe contener columnas 'u_ID_Credito' y 'Gestor'.");

            /* iterador explícito para evitar la excepción */
            while (it.hasNext()) {
                Row row = it.next();
                String cred = getString(row.getCell(colIdCred));
                String ges = getString(row.getCell(colGestor));
                if (cred == null || cred.isBlank())
                    continue;
                list.add(new RowData(cred.trim(), ges == null ? "" : ges.trim()));
            }
        }
        return list;
    }


    /*
     * =====================================================================
     * Helpers correo
     * =====================================================================
     */
    private void enviarCorreoOk(String resumen, String cuerpo, List<String> to) {
        String msg = (cuerpo == null || cuerpo.isBlank())
                ? "Corrección completada.\n[" + resumen + "]"
                : cuerpo + "\n\n[" + resumen + "]";
        enviarCorreo("Corrección Clientes – Completada", msg, to);
    }

    private void manejarError(Exception e, String cuerpo, List<String> to) {
        estadosSvc.actualizar(ESTADO_NOMBRE,
                new EstadosAutomatizaciones(null, ESTADO_NOMBRE,
                        "Error", e.getMessage(), LocalDate.now()));
        enviarCorreo("Corrección Clientes – ERROR",
                "El proceso terminó con error:\n\n" + e.getMessage(), to);
    }

    private void enviarCorreo(String asunto, String texto, List<String> dest) {
        List<String> base = Arrays.stream(MAIL_TO.split("[,;]"))
                .map(String::trim).filter(s -> !s.isBlank()).toList();
        List<String> extra = (dest == null) ? List.of()
                : dest.stream()
                        .flatMap(s -> Arrays.stream(s.split("[,;]")))
                        .map(String::trim).filter(s -> !s.isBlank()).toList();
        Set<String> all = new LinkedHashSet<>();
        all.addAll(base);
        all.addAll(extra);
        if (all.isEmpty())
            return;

        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(MAIL_FROM);
        m.setTo(all.toArray(String[]::new));
        m.setSubject(asunto);
        m.setText(texto);
        mailSender.send(m);
    }

    /*
     * =====================================================================
     * Otros helpers
     * =====================================================================
     */
    private CreditoPuestoPorSemana createNewCpps(
            String credId, LocalDate mon, LocalDate sun) {

        CreditoPuestoPorSemana x = new CreditoPuestoPorSemana();
        x.setCreditoId(credId);
        x.setFechaInicio(mon);
        x.setFechaFin(sun);
        return x;
    }

    /*
     * =====================================================================
     * Mapa “gestor normalizado” → puestoId (sólo de la semana actual)
     * =====================================================================
     */
    private Map<String, UUID> buildNameToPuestoMap(LocalDate mon, LocalDate sun) {

        // UPPS vigentes en la semana --------------------------------------
        List<UsuarioPuestoPorSemana> semanaUpps = uppsRepo.findAllByFechaInicioAndFechaFin(mon, sun);

        // Sólo puestos de nivel 2 (gestores) ------------------------------
        Set<UUID> puestosNivel2 = puestoRepo.findByNivel(2).stream()
                .map(Puesto::getId)
                .collect(Collectors.toSet());

        // Cache de usuarios para no ir a la BD en cada ciclo --------------
        Map<UUID, Usuario> usuarios = usuarioRepo.findAllById(
                semanaUpps.stream()
                        .map(UsuarioPuestoPorSemana::getUsuarioId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Usuario::getId, u -> u));

        // Construir el mapa final -----------------------------------------
        Map<String, UUID> map = new HashMap<>();
        for (UsuarioPuestoPorSemana up : semanaUpps) {
            if (!puestosNivel2.contains(up.getPuestoId()))
                continue;
            Usuario u = usuarios.get(up.getUsuarioId());
            if (u == null)
                continue;
            map.putIfAbsent(normalize(u.getNombre()), up.getPuestoId());
        }
        return map;
    }

    /*
     * =====================================================================
     * Refresca la columna “Gestor” en el Excel maestro
     * =====================================================================
     */
    private void refrescarGestorEnExcel(LocalDate mon, LocalDate sun) throws IOException {

        /* gestorId → nombreGestor */
        Map<UUID, String> nombreGestor = uppsRepo
                .findAllByFechaInicioAndFechaFin(mon, sun).stream()
                .filter(up -> up.getPuestoId() != null)
                .map(up -> Map.entry(
                        up.getPuestoId(),
                        usuarioRepo.findById(up.getUsuarioId())
                                .map(Usuario::getNombre).orElse(null)))
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a));

        /* creditoId → puestoId */
        Map<String, UUID> puestoByCred = cppsRepo
                .findAllByFechaInicioAndFechaFin(mon, sun).stream()
                .filter(c -> c.getPuestoId() != null)
                .collect(Collectors.toMap(CreditoPuestoPorSemana::getCreditoId,
                        CreditoPuestoPorSemana::getPuestoId,
                        (a, b) -> a));

        excelHelper.actualizarGestorColumn(puestoByCred, nombreGestor);
    }

    /*
     * =====================================================================
     * Utilidades varias
     * =====================================================================
     */
    /** Normaliza un nombre: quita paréntesis y pasa a minúsculas. */
    private static String normalize(String raw) {
        if (raw == null)
            return "";
        return raw.replaceFirst("\\(.*$", "") // quita "(…)" al final
                .trim()
                .toLowerCase();
    }

    private int findColumn(Row header, String name) {
        for (Iterator<Cell> it = header.cellIterator(); it.hasNext();) {
            Cell c = it.next();
            if (name.equalsIgnoreCase(c.getStringCellValue().trim()))
                return c.getColumnIndex();
        }
        return -1;
    }

    private String getString(Cell c) {
        if (c == null)
            return null;
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue();
            case NUMERIC -> String.valueOf((long) c.getNumericCellValue());
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            default -> null;
        };
    }

    /*
     * =====================================================================
     * Tipos internos (records)
     * =====================================================================
     */
    /** Representa una fila del Excel de corrección. */
    private record RowData(String creditoId, String gestorName) {
    }

    /** Resumen final del proceso. */
    private record ResumenCorreccion(int total, int actualizados, int sinCambio) {
    }

} 
