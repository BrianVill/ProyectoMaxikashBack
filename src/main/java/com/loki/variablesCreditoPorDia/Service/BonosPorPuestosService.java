package com.loki.variablesCreditoPorDia.Service;

import com.loki.bonos.DTOs.BonoResponseDTO;
import com.loki.estructuraUsuarios.Models.Puesto;
import com.loki.estructuraUsuarios.Models.Usuario;
import com.loki.estructuraUsuarios.Models.UsuarioPuestoPorSemana;
import com.loki.estructuraUsuarios.Repository.PuestoRepository;
import com.loki.estructuraUsuarios.Repository.UsuarioPuestoPorSemanaRepository;
import com.loki.estructuraUsuarios.Repository.UsuarioRepository;
import com.loki.variablesCreditoPorDia.Models.BonosPorPuestos;
import com.loki.variablesCreditoPorDia.Models.PuestoCumpleBono;
import com.loki.variablesCreditoPorDia.Repository.BonosPorPuestosRepository;
import com.loki.variablesCreditoPorDia.Repository.PuestoCumpleBonoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio que administra el cálculo, lectura y limpieza de bonos por puestos.
 */
@Service
public class BonosPorPuestosService {

    /* ────────── Repos y helpers ────────── */
    @Autowired private BonosPorPuestosRepository         repo;
    @Autowired private PuestoCumpleBonoRepository        pcbRepo;
    @Autowired private UsuarioPuestoPorSemanaRepository  upRepo;
    @Autowired private UsuarioRepository                 usuarioRepo;
    @Autowired private PuestoRepository                  puestoRepo;           // ← nuevo
    @Autowired private JdbcTemplate                      jdbc;
    @Autowired private RestTemplate                      rest;

    @Value("${endpoints.bonos-service-url}")
    private String BONOS_URL;

    /* ══════════════════════════════════════════════════
       0. UTILIDADES COMUNES
       ══════════════════════════════════════════════════ */

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
    private List<Map<String, Object>> toEnrichedList(List<BonosPorPuestos> lista) {

        /* 1. IDs únicos que necesitamos resolver */
        Set<Long>  bonosIds   = lista.stream().map(BonosPorPuestos::getIdBono)  .collect(Collectors.toSet());
        Set<UUID>  puestosIds = lista.stream().map(BonosPorPuestos::getPuestoId).collect(Collectors.toSet());

        /* 2. Map id→nombre para bonos y puestos */
        Map<Long, String> bonoNombres   = fetchNombresBonos(bonosIds);

        Map<UUID, String> puestoNombres = puestoRepo.findAllById(puestosIds).stream()
                                                   .collect(Collectors.toMap(
                                                           Puesto::getId,
                                                           Puesto::getNombre));

        /* 3. Armar resultado evitando type-mismatch */
        return lista.stream()
                    .map(b -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("idBono",        b.getIdBono());
                        m.put("nombreBono",    bonoNombres.getOrDefault(b.getIdBono(), "N/D"));
                        m.put("idPuesto",      b.getPuestoId());
                        m.put("nombrePuesto",  puestoNombres.getOrDefault(b.getPuestoId(), "N/D"));
                        m.put("monto",         b.getMonto());
                        m.put("fecha",         b.getFecha());
                        return m;
                    })
                    .toList();
    }

    /* ══════════════════════════════════════════════════
       1. CÁLCULO PRINCIPAL
       ══════════════════════════════════════════════════ */
    @Transactional
    public void calcularBonosPuestos() {

        List<PuestoCumpleBono> listaCumplen = pcbRepo.findAll().stream()
                                                     .filter(PuestoCumpleBono::isCumple)
                                                     .toList();

        for (PuestoCumpleBono pcb : listaCumplen) {

            UUID      puestoId = pcb.getPuestoId();
            Long      idBono   = pcb.getIdBono();
            LocalDate fecha    = pcb.getFecha();

            /* 1. Monto del bono para ese día */
            Double monto = getMontoDelBono(idBono, fecha);
            if (monto == null || monto == 0.0) continue;

            /* 2. Upsert en bonos_por_puestos */
            BonosPorPuestos bp = repo.findByPuestoIdAndIdBono(puestoId, idBono)
                                     .orElse(new BonosPorPuestos());

            bp.setPuestoId(puestoId);
            bp.setIdBono(idBono);
            bp.setMonto(monto);
            bp.setFecha(fecha);
            repo.save(bp);

            /* 3. Usuarios asignados a ese puesto esa semana */
            List<UsuarioPuestoPorSemana> asignaciones =
                    upRepo.findAllByPuestoIdAndFecha(puestoId, fecha);

            if (asignaciones.isEmpty()) {
                System.out.printf("[BONOS] Puesto %s sin usuarios asignados%n", puestoId);
                continue;
            }

            /* 4. Actualizar sueldo_final de los usuarios */
            List<Usuario> afectados = new ArrayList<>();
            for (UsuarioPuestoPorSemana up : asignaciones) {
                usuarioRepo.findById(up.getUsuarioId()).ifPresent(u -> {
                    double base = (u.getSueldoFinal() != null) ? u.getSueldoFinal()
                               : (u.getSueldo()      != null) ? u.getSueldo()
                               : 0.0;
                    u.setSueldoFinal(base + monto);
                    afectados.add(u);
                });
            }
            usuarioRepo.saveAll(afectados);

            String nombres = afectados.stream()
                                      .map(Usuario::getNombre)
                                      .collect(Collectors.joining(", "));
            System.out.printf("[BONOS] +%.2f al puesto %s (%d usuario/s): %s%n",
                              monto, puestoId, afectados.size(), nombres);
        }
    }

    /* ══════════════════════════════════════════════════
       2. RESET (tabla + sueldos)
       ══════════════════════════════════════════════════ */
    @Transactional
    public void resetearBonosYSueldos() {
        repo.deleteAll();
        jdbc.update("UPDATE usuario SET sueldo_final = 0");
    }

    /* ══════════════════════════════════════════════════
       3. LECTURA (enriquecidos)
       ══════════════════════════════════════════════════ */
    public List<Map<String, Object>> getAll() {
        return toEnrichedList(repo.findAll());
    }

    public List<Map<String, Object>> getByPuesto(UUID puestoId) {
        return toEnrichedList(repo.findByPuestoId(puestoId));
    }

    public List<Map<String, Object>> getByFechaBetween(LocalDate inicio, LocalDate fin) {
        return toEnrichedList(repo.findByFechaBetween(inicio, fin));
    }

    /* ══════════════════════════════════════════════════
       4. BORRAR TABLA
       ══════════════════════════════════════════════════ */
    @Transactional
    public void deleteAllBonosPorPuestos() {
        repo.deleteAll();
    }

    /* ══════════════════════════════════════════════════
       5. HELPER: Obtiene monto del bono para un día
       ══════════════════════════════════════════════════ */
    private Double getMontoDelBono(Long idBono, LocalDate fecha) {

        BonoResponseDTO bono = Arrays.stream(rest.getForObject(BONOS_URL, BonoResponseDTO[].class))
                                     .filter(b -> b.getId().equals(idBono))
                                     .findFirst().orElse(null);
        if (bono == null) return null;

        return switch (fecha.getDayOfWeek()) {
            case MONDAY    -> Double.parseDouble(bono.getLunes());
            case TUESDAY   -> Double.parseDouble(bono.getMartes());
            case WEDNESDAY -> Double.parseDouble(bono.getMiercoles());
            case THURSDAY  -> Double.parseDouble(bono.getJueves());
            case FRIDAY    -> Double.parseDouble(bono.getViernes());
            case SATURDAY  -> Double.parseDouble(bono.getSabado());
            case SUNDAY    -> Double.parseDouble(bono.getDomingo());
        };
    }
}
