package com.loki.variablesCreditoPorDia.Service;

import com.loki.variablesCreditoPorDia.DTOs.*;
import com.loki.variablesCreditoPorDia.Models.ClientesCumpleFiltros;
import com.loki.variablesCreditoPorDia.Models.VariablesCreditoPorDia;
import com.loki.variablesCreditoPorDia.Repository.*;

import com.loki.estructuraUsuarios.Models.EstadosAutomatizaciones;
import com.loki.estructuraUsuarios.Service.EstadosAutomatizacionesService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service @Slf4j
public class ClientesCumpleFiltrosService {

    /* ───────── repos ───────── */
    private final ClientesCumpleFiltrosRepository repo;
    private final VariablesCreditoPorDiaRepository varRepo;

    /* ───────── servicios dependientes ───────── */
    private final ClientesCumpleBonoService      cliBonoService;
    private final BonosPorPuestosService         bonosPuestosService;
    private final EstadosAutomatizacionesService estadosSvc;

    /* ───────── REST ───────── */
    private final RestTemplate rest;
    private final String URL_CONDICIONES;
    private final String URL_BONOS_LIST;
    private final String URL_BONO_BY_ID;

    /* ───────── correo ───────── */
    private final JavaMailSender mailSender;
    @Value("${app.mail.to}")          private String MAIL_TO;
    @Value("${spring.mail.username}") private String MAIL_FROM;

    private static final String ESTADO_NOMBRE = "EvaluarBonos";

     /* formatos de fecha admitidos */
     private static final DateTimeFormatter F1  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
     private static final DateTimeFormatter F2  = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
     private static final DateTimeFormatter F3  = DateTimeFormatter.ofPattern("dd-MM-yyyy");
     private static final DateTimeFormatter F4  = DateTimeFormatter.ofPattern("yyyy/MM/dd");
     private static final DateTimeFormatter F5  = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
     private static final DateTimeFormatter F6  = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
     private static final DateTimeFormatter F7  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
     private static final DateTimeFormatter F8  = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
     private static final DateTimeFormatter F9  = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /* paginado para no sobre-cargar memoria */
    private static final int PAGE_SIZE = 5_000;

    /* ───────────────────────── constructor ───────────────────────── */
    public ClientesCumpleFiltrosService(
            ClientesCumpleFiltrosRepository repo,
            VariablesCreditoPorDiaRepository varRepo,
            ClientesCumpleBonoService cliBonoService,
            BonosPorPuestosService bonosPuestosService,
            EstadosAutomatizacionesService estadosSvc,
            RestTemplate rest,
            @Value("${endpoints.condiciones-filtro-service-url}") String urlCond,
            @Value("${endpoints.bonos-service-url}")              String urlBonosList,
            @Value("${endpoints.bono-service-url}")               String urlBonoById,
            JavaMailSender mailSender) {

        this.repo                = repo;
        this.varRepo             = varRepo;
        this.cliBonoService      = cliBonoService;
        this.bonosPuestosService = bonosPuestosService;
        this.estadosSvc          = estadosSvc;
        this.rest                = rest;
        this.URL_CONDICIONES     = urlCond;
        this.URL_BONOS_LIST      = urlBonosList;
        this.URL_BONO_BY_ID      = urlBonoById;
        this.mailSender          = mailSender;
    }

    /* ════════════════════════════════════════
       0-A  WRAPPER ASYNC ORIGINAL (sin mail)
       ════════════════════════════════════════ */
    @Async @Transactional
    public void evaluarClientesCumpleFiltrosAsync() {
        evaluarClientesCumpleFiltrosAsync(null, null);
    }

    /* ════════════════════════════════════════
       0-B  WRAPPER ASYNC NUEVO (con mail)
       ════════════════════════════════════════ */
    @Async @Transactional
    public void evaluarClientesCumpleFiltrosAsync(String cuerpoEmail,
                                                  List<String> destinos) {
        ejecutarProceso(cuerpoEmail, destinos);
    }

    /* Version síncrona (Cloud Tasks) */
    @Transactional
    public void evaluarClientesCumpleFiltrosSync(String cuerpoEmail,
                                                 List<String> destinos) {
        ejecutarProceso(cuerpoEmail, destinos);
    }

    /* ════════════════════════════════════════
       1. CORE  (retorna Summary)
       ════════════════════════════════════════ */
    @Transactional
    public Summary evaluarClientesCumpleFiltros() {

        log.info("[EvaluarBonos] ▷ Iniciando proceso…");

        AtomicInteger upserts        = new AtomicInteger();
        AtomicInteger bonosSinFiltro = new AtomicInteger();

        /* 1) filtros tipo-1 */
        CondicionesDTO[] arr = rest.getForObject(URL_CONDICIONES, CondicionesDTO[].class);
        List<CondicionesDTO> filtrosTipo1 = Arrays.stream(arr)
                                                  .filter(c -> c.getTipoId() == 1)
                                                  .toList();
        log.info("[EvaluarBonos]  • Filtros tipo-1: {}", filtrosTipo1.size());

        /* 2) variables paginadas */
        int page = 0;
        Page<VariablesCreditoPorDia> pag;
        do {
            pag = varRepo.findAll(PageRequest.of(page, PAGE_SIZE));
            log.info("[EvaluarBonos]  • Página {} con {} registros", page, pag.getNumberOfElements());

            for (VariablesCreditoPorDia v : pag) {
                for (CondicionesDTO c : filtrosTipo1) {

                    Long idVar = c.getVariable().getId();
                    if (!v.getIdVariable().equals(idVar)) continue;

                    String op    = c.getOperador().getName();
                    String rawC  = c.getValor();
                    String rawV  = v.getValor();

                    LocalDate fechaCond = parseDate(rawC);
                    LocalDate fechaVar  = parseDate(rawV);
                    boolean   cumple;

                    if (fechaCond != null && fechaVar != null) {
                        cumple = compararFechas(fechaVar, fechaCond, op);
                    } else {
                        Double numCond = parseDouble(rawC);
                        Double numVar  = parseDouble(rawV);

                        if (numCond != null && numVar != null) {
                            cumple = compararNumeros(numVar, numCond, op);
                        } else {
                            cumple = compararStrings(rawV, rawC, op);
                        }
                    }

                    if (upsertDesdeVariable(v, c.getId(), cumple))
                        upserts.incrementAndGet();
                }
            }

            repo.flush(); // libera memoria en cada iteración
            page++;
        } while (!pag.isEmpty());

        log.info("[EvaluarBonos]  • Upserts tras filtros: {}", upserts.get());


        /* 3) bonos sin filtros tipo-1 */
        bonosSinFiltro.addAndGet(evaluarBonosSinFiltros());
        log.info("[EvaluarBonos]  • Upserts bonos sin filtro: {}", bonosSinFiltro.get());

        /* 4) procesos encadenados */
        cliBonoService.evaluarClientesCumpleBono();
        bonosPuestosService.calcularBonosPuestos();
        log.info("[EvaluarBonos]  • Cálculos de bonos encadenados completados");

        return new Summary(upserts.get(), bonosSinFiltro.get());
    }

    /* ════════════════════════════════════════
       2. UPSERT auxiliar
       ════════════════════════════════════════ */
    @Transactional
    public boolean upsertDesdeVariable(VariablesCreditoPorDia var,
                                       Long idCondicion,
                                       boolean cumple) {

        ClientesCumpleFiltros ent = repo
                .findByIdCreditoAndIdCondicion(var.getIdCredito(), idCondicion)
                .orElseGet(ClientesCumpleFiltros::new);

        ent.setIdCredito (var.getIdCredito());
        ent.setIdCondicion(idCondicion);
        ent.setCumple(cumple);
        ent.setFecha(var.getFecha());

        repo.save(ent);
        return true;
    }

    /* ════════════════════════════════════════
       3. Bonos sin filtro tipo-1
       ════════════════════════════════════════ */
    @Transactional
    public int evaluarBonosSinFiltros() {

        List<CondicionesDTO> filtros = Arrays.stream(
                rest.getForObject(URL_CONDICIONES, CondicionesDTO[].class))
                .filter(c -> c.getTipoId() == 1)
                .toList();

        List<Bono> bonos = Arrays.asList(
                rest.getForObject(URL_BONOS_LIST, Bono[].class));

        int contador = 0;

        for (Bono b : bonos) {
            boolean tieneFiltro = filtros.stream()
                                         .anyMatch(c -> c.getBonoId().equals(b.getId()));
            if (tieneFiltro) continue;

            /* variables otra vez en paging para no desbordar RAM */
            int page = 0;
            Page<VariablesCreditoPorDia> p;
            do {
                p = varRepo.findAll(PageRequest.of(page, PAGE_SIZE));
                for (VariablesCreditoPorDia v : p) {
                    upsertDesdeVariable(v, 0L, true);
                    contador++;
                }
                page++;
                repo.flush();
            } while (!p.isEmpty());
        }
        return contador;
    }

    /* ════════════════════════════════════════
       4. Métodos Controller (get / delete)
       ════════════════════════════════════════ */
    public List<ClientesCumpleFiltrosDTO> getAll(){
        return repo.findAll().stream().map(this::toDTO).toList();
    }
    public List<ClientesCumpleFiltrosDTO> getByCredito(String c){
        return repo.findByIdCredito(c).stream().map(this::toDTO).toList();
    }
    @Transactional public boolean delete(Long id){
        if (repo.existsById(id)){ repo.deleteById(id); return true; }
        return false;
    }

    /* ════════════════════════════════════════
       5. Helpers
       ════════════════════════════════════════ */
    private ClientesCumpleFiltrosDTO toDTO(ClientesCumpleFiltros e){
        return new ClientesCumpleFiltrosDTO(
                e.getId(), e.getIdCredito(), e.getIdCondicion(),
                e.getCumple(), e.getFecha());
    }


     /* ──────────────── parsers ──────────────── */
     private LocalDate parseDate(String raw){
        if (raw==null || raw.isBlank()) return null;
        String t = raw.trim();
        for (DateTimeFormatter f : List.of(F1,F2,F3,F4,F5,F6,F7,F8)) {
            try { return LocalDate.parse(t, f); }
            catch (DateTimeParseException ignored) {}
        }
        for (DateTimeFormatter f : List.of(F9)) {
            try { return LocalDateTime.parse(t, f).toLocalDate(); }
            catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private Double parseDouble(String raw){
        if (raw==null || raw.isBlank()) return null;
        String n = raw.trim();
        n = n.replaceAll("[^0-9.,-]", "");
        n = n.replace(" ", "");         // quitar espacios
        n = n.replace(',', '.');          // decimal con coma

        if (n.indexOf('.') != n.lastIndexOf('.')) {
            int last = n.lastIndexOf('.');
            String pre = n.substring(0, last).replace(".", "");
            n = pre + n.substring(last); // quitar separadores de miles
        }

        try { return Double.parseDouble(n); }
        catch (NumberFormatException e){ return null; }
    }

    /* ──────────────── comparadores ──────────────── */
    private boolean compararFechas(LocalDate v, LocalDate c, String op){
        String o = normalizarOperador(op);
        return switch (o) {
            case "<"  -> v.isBefore (c);
            case ">"  -> v.isAfter  (c);
            case "<=" -> !v.isAfter (c);
            case ">=" -> !v.isBefore(c);
            case "="  -> v.equals   (c);
            case "!=" -> !v.equals  (c);
            default   -> false;
        };
    }

    private boolean compararNumeros(double v, double c, String op){
        String o = normalizarOperador(op);
        return switch (o) {
            case "<"  -> v <  c;
            case ">"  -> v >  c;
            case "<=" -> v <= c;
            case ">=" -> v >= c;
            case "="  -> v == c;
            case "!=" -> v != c;
            default   -> false;
        };
    }

    private boolean compararStrings(String v, String c, String op){
        if (v == null || c == null) return false;
        String left  = v.trim();
        String right = c.trim();
        String o = normalizarOperador(op);
        return switch (o) {
            case "="  -> left.equalsIgnoreCase(right);
            case "!=" -> !left.equalsIgnoreCase(right);
            default   -> false;
        };
    }

    private String normalizarOperador(String op){
        if (op == null) return "";
        String o = op.trim().toLowerCase();
        return switch (o) {
            case "<", "lt", "menor", "menorque" -> "<";
            case ">", "gt", "mayor", "mayorque" -> ">";
            case "<=", "\u2264", "lte", "menor o igual", "menor_o_igual", "menorigual", "menor o igual que", "menorigualque" -> "<=";
            case ">=", "\u2265", "gte", "mayor o igual", "mayor_o_igual", "mayorigual", "mayor o igual que", "mayorigualque" -> ">=";
            case "!=", "<>", "ne", "diferente", "distinto", "no igual", "not equal" -> "!=";
            case "=", "==", "eq", "igual", "igual que", "igualque" -> "=";
            default -> o;
        };
    }

    /* ───────── correo helper ───────── */
    private void enviarCorreo(String asunto,String texto,List<String> dest){
        List<String> base = Arrays.stream(MAIL_TO.split("[,;]"))
                                  .map(String::trim).filter(s->!s.isBlank()).toList();
        List<String> extra = (dest==null)?List.of()
                : dest.stream().flatMap(s->Arrays.stream(s.split("[,;]")))
                       .map(String::trim).filter(s->!s.isBlank()).toList();
        Set<String> all = new LinkedHashSet<>();
        all.addAll(base); all.addAll(extra);
        if(all.isEmpty()) return;

        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(MAIL_FROM);
        m.setTo(all.toArray(String[]::new));
        m.setSubject(asunto);
        m.setText(texto);
        mailSender.send(m);
    }

    /* ════════════════════════════════════════
       6. Ejecutor común para async / sync
       ════════════════════════════════════════ */
    private void ejecutarProceso(String cuerpoEmail, List<String> destinos){
        try {
            Summary s = evaluarClientesCumpleFiltros();

            String msg = "Completado. upserts=%d  bonosSinFiltro=%d"
                         .formatted(s.upserts, s.bonosSinFiltro);

            estadosSvc.actualizar(ESTADO_NOMBRE, new EstadosAutomatizaciones(
                    null, ESTADO_NOMBRE, "Completado", msg, LocalDate.now()));

            log.info("[EvaluarBonos] ▶ FIN — {}", msg);

            String cuerpo = (cuerpoEmail == null || cuerpoEmail.isBlank())
                          ? "Proceso EvaluarBonos finalizado.\n[" + msg + "]"
                          : cuerpoEmail + "\n\n[" + msg + "]";
            enviarCorreo("Evaluar Bonos – Completado", cuerpo, destinos);

        } catch (Exception e) {

            estadosSvc.actualizar(ESTADO_NOMBRE, new EstadosAutomatizaciones(
                    null, ESTADO_NOMBRE, "Error", e.getMessage(), LocalDate.now()));

            log.error("[EvaluarBonos]  ERROR: {}", e.getMessage(), e);
            enviarCorreo("Evaluar Bonos – ERROR",
                         "El proceso terminó con error:\n\n" + e.getMessage(), destinos);

            throw new RuntimeException(e);
        }
    }

    /* ════════════════════════════════════════
       7. Records internos
       ════════════════════════════════════════ */
    private record Summary(int upserts,int bonosSinFiltro){}
}
