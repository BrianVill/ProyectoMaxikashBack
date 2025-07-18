package com.loki.variablesCreditoPorDia.Service;

import com.loki.bonos.DTOs.BonoResponseDTO;
import com.loki.estructuraUsuarios.Models.Puesto;
import com.loki.estructuraUsuarios.Repository.PuestoRepository;
import com.loki.variablesCreditoPorDia.DTOs.CondicionesDTO;
import com.loki.variablesCreditoPorDia.Models.PuestoCumpleBono;
import com.loki.variablesCreditoPorDia.Models.PuestoCumpleCondicion;
import com.loki.variablesCreditoPorDia.Repository.PuestoCumpleBonoRepository;
import com.loki.variablesCreditoPorDia.Repository.PuestoCumpleCondicionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Servicio que evalúa qué puestos cumplen cada bono y expone lecturas enriquecidas
 * con el nombre del bono y del puesto.
 */
@Service
public class PuestoCumpleBonoService {

    /* ────────── Repos y helpers ────────── */
    @Autowired private PuestoCumpleBonoRepository   repo;
    @Autowired private PuestoCumpleCondicionRepository condRepo;
    @Autowired private PuestoRepository             puestoRepo;          // ← nuevo
    @Autowired private RestTemplate                 rest;

    /* Endpoints externos */
    @Value("${endpoints.condiciones-service-url}")
    private String CONDICIONES_URL;
    @Value("${endpoints.bonos-service-url}")
    private String BONOS_URL;

    /* ═══════════════════════════════════════════════
       UTILIDADES COMUNES
       ═══════════════════════════════════════════════ */

    /** Devuelve un map {idBono → nombreBono} para los IDs requeridos */
    private Map<Long, String> fetchNombresBonos(Set<Long> idsNecesarios) {
        BonoResponseDTO[] bonos = rest.getForObject(BONOS_URL, BonoResponseDTO[].class);
        if (bonos == null) return Collections.emptyMap();

        return Arrays.stream(bonos)
                     .filter(b -> idsNecesarios.contains(b.getId()))
                     .collect(Collectors.toMap(
                             BonoResponseDTO::getId,
                             BonoResponseDTO::getNombre));
    }

    /** Convierte una lista de entidades a lista de Map enriquecida con nombres */
    private List<Map<String, Object>> toEnrichedList(List<PuestoCumpleBono> lista) {

        /* 1. IDs únicos que necesitamos resolver */
        Set<Long>  bonoIds   = lista.stream().map(PuestoCumpleBono::getIdBono)  .collect(Collectors.toSet());
        Set<UUID>  puestoIds = lista.stream().map(PuestoCumpleBono::getPuestoId).collect(Collectors.toSet());

        /* 2. Map id→nombre para bonos y puestos */
        Map<Long, String> bonoNombres   = fetchNombresBonos(bonoIds);

        Map<UUID, String> puestoNombres = puestoRepo.findAllById(puestoIds).stream()
                                                   .collect(Collectors.toMap(
                                                           Puesto::getId,
                                                           Puesto::getNombre));

        /* 3. Armar resultado */
        return lista.stream()
                    .map(p -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("idBono",        p.getIdBono());
                        m.put("nombreBono",    bonoNombres.getOrDefault(p.getIdBono(), "N/D"));
                        m.put("idPuesto",      p.getPuestoId());
                        m.put("nombrePuesto",  puestoNombres.getOrDefault(p.getPuestoId(), "N/D"));
                        m.put("cumple",        p.isCumple());
                        m.put("progreso",      p.getProgreso());
                        m.put("fecha",         p.getFecha());
                        return m;
                    })
                    .toList();
    }

    /* ═══════════════════════════════════════════════
       1. EVALUAR PUESTOS QUE CUMPLEN CONDICIONES DE BONOS
       ═══════════════════════════════════════════════ */
    @Async
    @Transactional
    public CompletableFuture<Void> evaluarPuestosCumplenBonos() {

        /* 1. Agrupar condiciones de tipo no-filtro por bono */
        Map<Long, List<Long>> condPorBono =
            Arrays.stream(rest.getForObject(CONDICIONES_URL, CondicionesDTO[].class))
                  .filter(c -> c.getTipoId() > 1)                       // tipos >1 = no-filtro
                  .collect(Collectors.groupingBy(
                          CondicionesDTO::getBonoId,
                          Collectors.mapping(CondicionesDTO::getId, Collectors.toList())));

        /* 2. Fecha de evaluación (última fecha cargada o hoy) */
        LocalDate fecha = condRepo.findMaxFecha()
                                  .orElse(LocalDate.now());

        /* 3. Condiciones cumplidas por puesto en esa fecha */
        Map<UUID, List<PuestoCumpleCondicion>> condPorPuesto =
            condRepo.findByFecha(fecha).stream()
                    .collect(Collectors.groupingBy(PuestoCumpleCondicion::getPuestoId));

        /* 4. Evaluar cada bono para cada puesto */
        for (var e : condPorBono.entrySet()) {
            Long idBono       = e.getKey();
            List<Long> idsReq = e.getValue();               // condiciones requeridas para este bono

            for (var reg : condPorPuesto.entrySet()) {
                UUID puestoId = reg.getKey();

                long ok = reg.getValue().stream()
                              .filter(c -> idsReq.contains(c.getIdCondicion()) && c.isCumple())
                              .count();

                boolean cumple = ok == idsReq.size();
                String  progreso = ok + "/" + idsReq.size();

                PuestoCumpleBono ent = repo.findByPuestoIdAndIdBono(puestoId, idBono)
                                           .orElse(new PuestoCumpleBono());

                ent.setPuestoId(puestoId);
                ent.setIdBono(idBono);
                ent.setCumple(cumple);
                ent.setProgreso(progreso);
                ent.setFecha(fecha);
                repo.save(ent);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    /** Variante síncrona para Cloud Tasks. */
    public void evaluarPuestosCumplenBonosSync() {
        evaluarPuestosCumplenBonos().join();
    }

    /* ═══════════════════════════════════════════════
       2. LECTURA – TODOS ENRIQUECIDOS
       ═══════════════════════════════════════════════ */

    public List<Map<String, Object>> getAll() {
        return toEnrichedList(repo.findAll());
    }

    public List<Map<String, Object>> getByPuesto(UUID puestoId) {
        return toEnrichedList(repo.findByPuestoId(puestoId));
    }

    public List<Map<String, Object>> getByFechaBetween(LocalDate ini, LocalDate fin) {
        return toEnrichedList(repo.findByFechaBetween(ini, fin));
    }
}
