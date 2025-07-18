package com.loki.variablesCreditoPorDia.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.loki.variablesCreditoPorDia.DTOs.CondicionesDTO;
import com.loki.variablesCreditoPorDia.Models.*;
import com.loki.variablesCreditoPorDia.Repository.*;

import jakarta.transaction.Transactional;

@Service
public class PuestoCumpleCondicionService {

    /* ---------- repos ---------- */
    @Autowired private PuestoCumpleCondicionRepository repo;
    @Autowired private ClientesCumpleBonoRepository     cliBonoRepo;
    @Autowired private VariablesCreditoPorDiaRepository varRepo;

    /* ---------- REST ---------- */
    @Autowired private RestTemplate rest;
    @Value("${endpoints.condiciones-service-url}")
    private String CONDICIONES_URL;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /* ==================================================================== */
    /* 1. Evaluar condiciones para cada puesto                              */
    /* ==================================================================== */
    @Async
    @Transactional
    public CompletableFuture<Void> evaluarPuestosCumplenCondiciones() {

        /* a) puestos que ya cumplieron algún bono (clientes) -------------- */
        List<UUID> puestos = cliBonoRepo.findAll().stream()
                .filter(ClientesCumpleBono::isCumple)
                .map(ClientesCumpleBono::getPuestoId)
                .distinct()
                .toList();

        if (puestos.isEmpty())
            return CompletableFuture.completedFuture(null);

        /* b) condiciones de tipo > 1 -------------------------------------- */
        List<CondicionesDTO> condiciones = Arrays.stream(
                        rest.getForObject(CONDICIONES_URL, CondicionesDTO[].class))
                .filter(c -> c.getTipoId() > 1)
                .toList();

        /* c) variables agrupadas por puesto ------------------------------- */
        Map<UUID, List<VariablesCreditoPorDia>> varsPorPuesto = new HashMap<>();
        for (UUID p : puestos) {
            /* 🔹 ahora traemos TODAS las variables del puesto */
            varsPorPuesto.put(p, varRepo.findByPuestoId(p));
        }

        /* d) evaluar y upsert --------------------------------------------- */
        for (UUID puestoId : puestos) {
            List<VariablesCreditoPorDia> vars = varsPorPuesto.getOrDefault(
                    puestoId, Collections.emptyList());

            for (CondicionesDTO c : condiciones) {
                boolean cumple = eval(vars, c);

                PuestoCumpleCondicion ent = repo
                        .findByPuestoIdAndIdCondicion(puestoId, c.getId())
                        .orElse(new PuestoCumpleCondicion());

                ent.setPuestoId(puestoId);
                ent.setIdCondicion(c.getId());
                ent.setCumple(cumple);
                ent.setFecha(LocalDate.now());

                repo.save(ent);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    /** Variante síncrona para Cloud Tasks. */
    public void evaluarPuestosCumplenCondicionesSync() {
        evaluarPuestosCumplenCondiciones().join();
    }

    /* ==================================================================== */
    /* 2. Helpers de lectura / borrado                                      */
    /* ==================================================================== */
    public List<PuestoCumpleCondicion> getAll()            { return repo.findAll(); }

    public List<PuestoCumpleCondicion> getByPuesto(UUID p) { return repo.findByPuestoId(p); }

    @Transactional
    public boolean delete(UUID puestoId, Long condId) {
        return repo.findByPuestoIdAndIdCondicion(puestoId, condId)
                   .map(ent -> { repo.delete(ent); return true; })
                   .orElse(false);
    }

    /* ==================================================================== */
    /* 3. Evaluación numérica / porcentual                                  */
    /* ==================================================================== */
    private boolean eval(List<VariablesCreditoPorDia> vars, CondicionesDTO c) {

        Long   idVar  = c.getVariable().getId();
        String op     = c.getOperador().getName();
        double cmpVal = Double.parseDouble(c.getValor());

        List<Double> valores = vars.stream()
                .filter(v -> v.getIdVariable().equals(idVar))
                .map(v -> {
                    try { return Double.parseDouble(v.getValor()); }
                    catch (NumberFormatException e) { return 0.0; }
                })
                .collect(Collectors.toList());

        if (valores.isEmpty()) return false;

        int tipo = Math.toIntExact(c.getTipoId());
        double valor = switch (tipo) {
            case 2 -> valores.stream()
                             .filter(v -> v == 1)
                             .count() * 100.0 / valores.size();  // porcentaje
            case 3 -> valores.stream().mapToDouble(Double::doubleValue).sum(); // suma
            case 4 -> valores.stream().mapToDouble(Double::doubleValue)
                             .average().orElse(0);               // promedio
            case 5 -> valores.stream().filter(v -> v == 1).count();            // conteo
            default -> 0;
        };

        return switch (op) {
            case ">"  -> valor >  cmpVal;
            case "<"  -> valor <  cmpVal;
            case ">=" -> valor >= cmpVal;
            case "<=" -> valor <= cmpVal;
            case "="  -> valor == cmpVal;
            case "!=" -> valor != cmpVal;
            default   -> false;
        };
    }
}
