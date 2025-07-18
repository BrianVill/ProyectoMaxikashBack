package com.loki.variablesCreditoPorDia.Service;

import com.github.pjfanning.xlsx.StreamingReader;
import com.loki.estructuraUsuarios.Models.EstadosAutomatizaciones;
import com.loki.estructuraUsuarios.Repository.CreditoPuestoPorSemanaRepository;
import com.loki.estructuraUsuarios.Service.EstadosAutomatizacionesService;
import com.loki.variablesCreditoPorDia.Models.VariablesCreditoPorDia;
import com.loki.variablesCreditoPorDia.Repository.VariablesCreditoPorDiaRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.nio.ByteBuffer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VariablesDiariasService {

    /* ───────────── dependencias ───────────── */
    private final JdbcTemplate                     jdbc;
    private final VariablesCreditoPorDiaRepository varRepo;
    private final CreditoPuestoPorSemanaRepository cppsRepo;
    private final EstadosAutomatizacionesService   estadosSvc;
    private final JavaMailSender                   mailSender;

    /* mail config */
    @Value("${app.mail.to}")          private String MAIL_TO;
    @Value("${spring.mail.username}") private String MAIL_FROM;

    public VariablesDiariasService(JdbcTemplate jdbc,
                                   VariablesCreditoPorDiaRepository varRepo,
                                   CreditoPuestoPorSemanaRepository cppsRepo,
                                   EstadosAutomatizacionesService estadosSvc,
                                   JavaMailSender mailSender) {
        this.jdbc       = jdbc;
        this.varRepo    = varRepo;
        this.cppsRepo   = cppsRepo;
        this.estadosSvc = estadosSvc;
        this.mailSender = mailSender;
    }

    /* ───────────── constantes ───────────── */
    private static final DateTimeFormatter DF   = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int               BATCH= 1_000;
    private static final String            PROC = "Variables";

    /* ═════════════════════════════════════════════
       0)  WRAPPERS  (async / sync)
       ═════════════════════════════════════════════ */
    @Async
    public void importarVariablesDiariasAsync(byte[] fileBytes,
                                              String fechaStr,
                                              String cuerpoEmail,
                                              List<String> destinos) {
        try (var bis = new ByteArrayInputStream(fileBytes)) {
            Resumen res = core(bis, fechaStr);
            notificarOK(res, cuerpoEmail, destinos);
        } catch (Exception e) {
            manejarError(e, cuerpoEmail, destinos);
        }
    }

    public void importarVariablesDiariasSync(byte[] fileBytes,
                                             String fechaStr,
                                             String cuerpoEmail,
                                             List<String> destinos) {
        try (var bis = new ByteArrayInputStream(fileBytes)) {
            Resumen res = core(bis, fechaStr);
            notificarOK(res, cuerpoEmail, destinos);
        } catch (Exception e) {
            manejarError(e, cuerpoEmail, destinos);
            throw new RuntimeException(e);   // para Cloud Tasks
        }
    }

    /* ═════════════════════════════════════════════
       1)  LÓGICA CENTRAL  
       ═════════════════════════════════════════════ */

    protected Resumen core(InputStream excel, String fechaStr) throws Exception {

        /* 1. Fecha del lote */
        LocalDate fecha = (fechaStr!=null && !fechaStr.isBlank())
                          ? LocalDate.parse(fechaStr, DF)
                          : LocalDate.now();

        /* 2. Leer XLSX en streaming (poca RAM) */
        List<RowInfo> filas = leerExcelStreaming(excel);
        if (filas.isEmpty()) return new Resumen();

        /* 3. Precargas -------------------------------------------------- */
        Map<String,UUID> puestoByCred = cppsRepo.findPuestoByFecha(fecha).stream()
                .filter(a -> a[0]!=null && a[1]!=null)
                .collect(Collectors.toMap(
                        a -> (String) a[0],
                        a -> (UUID)   a[1]));

        Map<String,Long> varIdByName = jdbc.queryForList("SELECT name,id FROM variables")
                .stream()
                .collect(Collectors.toMap(
                        m -> norm((String)m.get("name")),
                        m -> ((Number)m.get("id")).longValue()));

        /* 3-B  crear variables faltantes -------------------------------- */
        Set<String> nombresXls = filas.get(0).valores.keySet().stream()
                .map(s -> norm(s.substring(2)))   // v_nombre → nombre
                .collect(Collectors.toSet());

        List<Object[]> nuevosVars = nombresXls.stream()
                .filter(n -> !varIdByName.containsKey(n))
                .map(n -> new Object[]{n,"string"})
                .toList();

        if (!nuevosVars.isEmpty()) {
            jdbc.batchUpdate("INSERT INTO variables(name,type) VALUES(?,?)", nuevosVars);
            varIdByName.clear();
            varIdByName.putAll(
                    jdbc.queryForList("SELECT name,id FROM variables").stream()
                        .collect(Collectors.toMap(
                            m -> norm((String)m.get("name")),
                            m -> ((Number)m.get("id")).longValue())));
        }

        /* 3-C  índice existente del día --------------------------------- */
        Set<String> idxExist =
            varRepo.findByFecha(fecha).stream()
                   .map(v -> clave(v.getIdCredito(), v.getIdVariable()))
                   .collect(Collectors.toSet());

         /* 4. Procesar filas --------------------------------------------- */
         /* 4. Procesar filas --------------------------------------------- */
         List<VariablesCreditoPorDia> buf = new ArrayList<>(BATCH);
         int nuevos=0, updates=0, sinPuesto=0;
 
         for (RowInfo row : filas) {
 
             UUID puestoId = puestoByCred.computeIfAbsent(
                     row.idCredito,
                     c -> cppsRepo.findSemana(c, fecha).map(cpp -> cpp.getPuestoId()).orElse(null));
 
             if (puestoId == null) { sinPuesto++; continue; }
 
             for (Map.Entry<String,String> celda : row.valores.entrySet()) {
 
                 String val = celda.getValue();
                 if (val==null || val.isBlank()) continue;
 
                 Long idVar = varIdByName.get(norm(celda.getKey().substring(2)));
                 if (idVar == null) continue;
 
                 String k   = clave(row.idCredito, idVar);
                 if (idxExist.contains(k)) {
                     updates++;
                 } else {
                     idxExist.add(k);
                     nuevos++;
                 }
 
                 VariablesCreditoPorDia ent = new VariablesCreditoPorDia(
                         row.idCredito, puestoId, idVar, val);
                 ent.setFecha(fecha);
                 buf.add(ent);
 
                 if (buf.size()>=BATCH) { flushBatch(buf); }
             }
         }
         if (!buf.isEmpty()) flushBatch(buf);
 
         // Clear remaining references
         return new Resumen(nuevos, updates, sinPuesto);
     }
 

    /* ═════════════════════════════════════════════
       2)  STREAMING READER
       ═════════════════════════════════════════════ */
       private List<RowInfo> leerExcelStreaming(InputStream is) throws Exception {

        List<RowInfo> list = new ArrayList<>();

        try (Workbook wb = StreamingReader.builder()
                                          .rowCacheSize(200)
                                          .bufferSize(4096)
                                          .open(is)) {

            Sheet sh = wb.getSheetAt(0);
            if (sh == null) return list;

            Iterator<Row> rowIt = sh.iterator();
            if (!rowIt.hasNext()) return list;
            Row header = rowIt.next();

            Map<String,Integer> col = new HashMap<>();
            for (Iterator<Cell> cIt = header.cellIterator(); cIt.hasNext();) {
                Cell c = cIt.next();
                col.put(c.getStringCellValue().trim().toLowerCase(), c.getColumnIndex());
            }

            if (!col.containsKey("u_id_credito"))
                throw new IllegalStateException("Falta la columna u_id_credito");

            /* columnas de variables (v_…) */
            List<String> vCols = col.keySet().stream()
                                    .filter(s -> s.startsWith("v_"))
                                    .toList();

            while (rowIt.hasNext()) {
                Row row = rowIt.next();
                if (row.getRowNum()==0) continue;      // salta encabezado
                String id = getStr(row.getCell(col.get("u_id_credito")));
                if (id==null || id.isBlank()) continue;

                RowInfo info = new RowInfo(id.trim());
                for (String v : vCols)
                    info.valores.put(v, getStr(row.getCell(col.get(v))));
                list.add(info);
            }
        }
        return list;
    }


    /* ═════════════════ helpers ═════════════════ */
    private void flushBatch(List<VariablesCreditoPorDia> batch){
        if(batch.isEmpty()) return;
        jdbc.batchUpdate("""
            INSERT INTO variables_creditos_por_dia
                (fecha, id_credito, id_variable, id_puesto, valor)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                id_puesto = VALUES(id_puesto),
                valor     = VALUES(valor)
            """, batch, batch.size(), (ps, v) -> {
                ps.setDate(1, java.sql.Date.valueOf(v.getFecha()));
                ps.setString(2, v.getIdCredito());
                ps.setLong(3, v.getIdVariable());
                ps.setBytes(4, uuidToBytes(v.getPuestoId()));
                ps.setString(5, v.getValor());
        });
        batch.clear();
    }

    private void notificarOK(Resumen r, String cuerpo, List<String> to){
        String resumen = "nuevos=%d  updates=%d  sinPuesto=%d"
                         .formatted(r.nuevos, r.updates, r.sinPuesto);
        estadosSvc.actualizar(PROC,
                new EstadosAutomatizaciones(null, PROC,
                        "Completado", resumen, LocalDate.now()));

        String msg = (cuerpo==null || cuerpo.isBlank())
                   ? "Importación de variables diarias completada.\n["+resumen+"]"
                   : cuerpo + "\n\n["+resumen+"]";
        enviarCorreo("Importación Variables Diarias – Completada", msg, to);
    }

    private void manejarError(Exception e, String cuerpo, List<String> to){
        estadosSvc.actualizar(PROC,
                new EstadosAutomatizaciones(null, PROC,
                        "Error", e.getMessage(), LocalDate.now()));
        enviarCorreo("Importación Variables Diarias – ERROR",
                     "El proceso terminó con error:\n\n"+e.getMessage(), to);
    }

    private void enviarCorreo(String asunto,String texto,List<String> dest){
        List<String> base  = Arrays.stream(MAIL_TO.split("[,;]"))
                                   .map(String::trim).filter(s->!s.isBlank()).toList();
        List<String> extra = (dest==null)?List.of()
                                  : dest.stream()
                                        .flatMap(s->Arrays.stream(s.split("[,;]")))
                                        .map(String::trim).filter(s->!s.isBlank()).toList();
        Set<String> all = new LinkedHashSet<>(); all.addAll(base); all.addAll(extra);
        if(all.isEmpty()) return;

        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(MAIL_FROM);
        m.setTo(all.toArray(String[]::new));
        m.setSubject(asunto);
        m.setText(texto);
        mailSender.send(m);
    }

    private static String norm(String s){ return s.trim().toLowerCase(); }

    private static String clave(String c,long v){ return c+'|'+v; }

    private static byte[] uuidToBytes(UUID u){
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(u.getMostSignificantBits());
        bb.putLong(u.getLeastSignificantBits());
        return bb.array();
    }

    private String getStr(Cell c){
        if (c==null) return null;
        return switch(c.getCellType()){
            case STRING  -> c.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(c))
                    yield c.getLocalDateTimeCellValue().toLocalDate().format(DF);
                double v = c.getNumericCellValue();
                yield (v==Math.floor(v))
                        ? String.valueOf((long)v)
                        : String.valueOf(v);
            }
            case BOOLEAN -> c.getBooleanCellValue() ? "1" : "0";
            default      -> null;
        };
    }

    /* tipos internos -------------------------------------------------- */
    private static class RowInfo{
        final String idCredito;
        final Map<String,String> valores = new HashMap<>();
        RowInfo(String id){ this.idCredito=id; }
    }
    private record Resumen(int nuevos,int updates,int sinPuesto){
        Resumen(){ this(0,0,0); }
    }
}
