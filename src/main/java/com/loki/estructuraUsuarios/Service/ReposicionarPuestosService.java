package com.loki.estructuraUsuarios.Service;

import com.loki.estructuraUsuarios.Models.Credito;
import com.loki.estructuraUsuarios.Models.Puesto;
import com.loki.estructuraUsuarios.Repository.CreditoRepository;
import com.loki.estructuraUsuarios.Repository.PuestoRepository;
import org.apache.commons.math3.util.FastMath;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ReposicionarPuestosService {

    private final CreditoRepository creditoRepo;
    private final PuestoRepository  puestoRepo;

    public ReposicionarPuestosService(CreditoRepository creditoRepo,
                                      PuestoRepository puestoRepo) {
        this.creditoRepo = creditoRepo;
        this.puestoRepo  = puestoRepo;
    }

    /**
     * Reubica los puestos para maximizar la cobertura de créditos
     *
     * @param capacidad   cupo máximo de créditos por puesto
     * @param maxIter     máximas iteraciones de mejora local
     * @param maxDistKm   radio máximo (km) para considerar un crédito asignable
     */
    @Transactional
    public void reposicionarPuestos(int capacidad, int maxIter, double maxDistKm) {

        /* ---------- datos de entrada ---------- */
        List<Credito> clientes = creditoRepo.findAll().stream()
                .filter(c -> esMexico(c.getLat(), c.getLon()))
                .toList();
        List<Puesto> puestos = puestoRepo.findByNivel(2);

        if (clientes.isEmpty() || puestos.isEmpty()) return;

        /* posición original: para medir cuánto se mueve cada puesto */
        Map<UUID,double[]> posOriginal = puestos.stream()
                .collect(Collectors.toMap(Puesto::getId,
                        p -> new double[]{p.getLat(), p.getLon()}));

        final double lambda = 0.02;           // penalización por desplazamiento (km⁻¹)

        /* ---------- bucle principal de mejora ---------- */
        int   iter     = 0;
        boolean mejoro;

        do {
            iter++;
            Map<Puesto,List<Credito>> asig = asignar(clientes, puestos, capacidad, maxDistKm);
            double bestScore = score(asig, puestos, posOriginal, lambda);
            mejoro = false;

            for (Puesto p : puestos) {

                /* 1) créditos ya servidos por este puesto */
                List<Credito> propios = asig.getOrDefault(p, List.of());
            
                /* ---  NUEVO: conjunto final con los ya asignados --- */
                final Set<Credito> yaAsignados = asig.values().stream()
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
            
                /* 2) hasta 'capacidad' créditos NO servidos más cercanos a p */
                List<Credito> sinAtender = clientes.stream()
                        .filter(c -> !yaAsignados.contains(c))        // ← ya no usamos 'asig'
                        .sorted(Comparator.comparingDouble(
                                c -> distanciaKm(c.getLat(), c.getLon(),
                                                 p.getLat(), p.getLon())))
                        .limit(capacidad)
                        .toList();
            
                if (propios.isEmpty() && sinAtender.isEmpty()) continue;
            
                /* 3) centroide candidato */
                double newLat = Stream.concat(propios.stream(), sinAtender.stream())
                                       .mapToDouble(Credito::getLat).average().orElse(p.getLat());
                double newLon = Stream.concat(propios.stream(), sinAtender.stream())
                                       .mapToDouble(Credito::getLon).average().orElse(p.getLon());
            
                double oldLat = p.getLat(), oldLon = p.getLon();
                p.setLat(newLat); p.setLon(newLon);
            
                Map<Puesto,List<Credito>> asigNew = asignar(clientes, puestos, capacidad, maxDistKm);
                double newScore = score(asigNew, puestos, posOriginal, lambda);
            
                if (newScore > bestScore) {           // mejora aceptada
                    bestScore = newScore;
                    asig      = asigNew;              // ← ya NO se usa en lambdas anteriores
                    mejoro    = true;
                } else {                              // revertimos
                    p.setLat(oldLat); p.setLon(oldLon);
                }
            }
            

            System.out.printf("Iter %2d  cobertura=%4d  score=%.1f%n",
                              iter, clientesAsignados(asig), score(asig, puestos, posOriginal, lambda));

        } while (mejoro && iter < maxIter);

        /* ---------- persistir nuevos lats/lons ---------- */
        puestoRepo.saveAll(puestos);

        /* ---------- informe de desplazamientos ---------- */
        puestos.forEach(p -> {
            double[] orig = posOriginal.get(p.getId());
            double dKm    = distanciaKm(orig[0], orig[1], p.getLat(), p.getLon());
            System.out.printf("%-20s movió %.2f km%n", p.getNombre(), dKm);
        });
    }

    @Transactional
    public void kMeansCapacitado(int capacidad, int maxIter, double maxDistKm) {

        List<Credito> clientes = creditoRepo.findAll().stream()
                .filter(c -> esMexico(c.getLat(), c.getLon()))
                .toList();
        List<Puesto> puestos = puestoRepo.findByNivel(2);
        if (clientes.isEmpty() || puestos.isEmpty()) return;

        Map<Puesto,List<Credito>> asign = new HashMap<>();
        puestos.forEach(p -> asign.put(p, new ArrayList<>(capacidad)));

        for (int it = 0; it < maxIter; it++) {

            // 1) vaciar
            asign.values().forEach(List::clear);

            // 2) asignación greedy
            for (Credito c : clientes) {
                Puesto best = null; double bestD = Double.MAX_VALUE;

                for (Puesto p : puestos) {
                    List<Credito> lista = asign.computeIfAbsent(p,
                            k -> new ArrayList<>(capacidad));   // ← fix

                    if (lista.size() == capacidad) continue;

                    double d = distanciaKm(p.getLat(), p.getLon(),
                                        c.getLat(), c.getLon());
                    if (d < bestD) { bestD = d; best = p; }
                }
                if (best != null && bestD <= maxDistKm) asign.get(best).add(c);
            }

            // 3) recalcular centroides
            boolean cambio = false;
            for (Puesto p : puestos) {
                List<Credito> lista = asign.get(p);
                if (lista.isEmpty()) continue;

                double newLat = lista.stream().mapToDouble(Credito::getLat).average().orElse(p.getLat());
                double newLon = lista.stream().mapToDouble(Credito::getLon).average().orElse(p.getLon());

                if (FastMath.abs(newLat - p.getLat()) > 1e-5 ||
                    FastMath.abs(newLon - p.getLon()) > 1e-5) {
                    p.setLat(newLat); p.setLon(newLon); cambio = true;
                }
            }

            System.out.printf("Iter %2d  créditos cubiertos=%d%n",
                            it + 1,
                            asign.values().stream().mapToInt(List::size).sum());

            if (!cambio) break;      // centroides estables
        }

        puestoRepo.saveAll(puestos);
    }


    /* =======================================================================
       MÉTODO PÚBLICO: primero tu heurística, luego uno o más balanceos
    ======================================================================= */
    @Transactional
    public void reposicionarConBalanceo(int capacidad,
                                        int iterPrincipal,
                                        int iterBalanceo,
                                        double maxDistKm) {

        // 1) heurística principal (la que ya tenías)
        reposicionarPuestos(capacidad, iterPrincipal, maxDistKm);

        // 2) balance-swap n veces o hasta que ya no mejore
        List<Credito> clientes = creditoRepo.findAll().stream()
                .filter(c -> esMexico(c.getLat(), c.getLon()))
                .toList();
        List<Puesto> puestos = puestoRepo.findByNivel(2);

        for (int i = 0; i < iterBalanceo; i++) {
            boolean mejoro = balancear(capacidad, maxDistKm, clientes, puestos);
            if (!mejoro) break;               // ya no sube la cobertura
        }
    }

    /* =======================================================================
        ❶  Balance-swap: intenta mover UN puesto poco cargado al mayor cluster
        ======================================================================= */
    // ── NEW
    private boolean balancear(int capacidad,
                                double maxDistKm,
                                List<Credito> clientes,
                                List<Puesto> puestos) {

        Map<Puesto,List<Credito>> asign = asignar(clientes, puestos, capacidad, maxDistKm);
        int servidosAntes = clientesAsignados(asign);

        /* 1) créditos sin asignar */
        List<Credito> sinAsignar = clientes.stream()
                .filter(c -> asign.values().stream().noneMatch(l -> l.contains(c)))
                .toList();
        if (sinAsignar.isEmpty()) return false;

        /* 2) cluster más grande de sinAsignar (agrupación muy ligera) */
        List<Credito> cluster = construirCluster(sinAsignar, maxDistKm);    // ── NEW
        if (cluster.size() < capacidad / 3) return false;  // cluster demasiado chico

        /* 3) puesto con MENOR carga */
        Puesto candidato = puestos.stream()
                .min(Comparator.comparingInt(p -> asign.getOrDefault(p, List.of()).size()))
                .orElse(null);
        if (candidato == null) return false;

        double oldLat = candidato.getLat(), oldLon = candidato.getLon();

        /* 4) mover candidato al centroide del cluster */
        double newLat = cluster.stream().mapToDouble(Credito::getLat).average().orElse(oldLat);
        double newLon = cluster.stream().mapToDouble(Credito::getLon).average().orElse(oldLon);
        candidato.setLat(newLat);
        candidato.setLon(newLon);

        /* 5) evaluar mejora */
        Map<Puesto,List<Credito>> asign2 = asignar(clientes, puestos, capacidad, maxDistKm);
        int servidosDespues = clientesAsignados(asign2);

        if (servidosDespues > servidosAntes) {          // mejora verdadera
            puestoRepo.save(candidato);
            System.out.printf("Balanceo +%d créditos (total %d)%n",
                                servidosDespues - servidosAntes, servidosDespues);
            return true;
        } else {                                        // revertir movimiento
            candidato.setLat(oldLat);
            candidato.setLon(oldLon);
            return false;
        }
    }

    /* =======================================================================
        ❷  Construye un cluster simple a partir del punto más denso
        ======================================================================= */
    // ── NEW
    private List<Credito> construirCluster(List<Credito> puntos, double radioKm) {

        if (puntos.isEmpty()) return List.of();

        /* a) elegir “seed” con mayor número de vecinos en radio */
        Credito seed = puntos.stream().max(Comparator.comparingInt(c ->
                (int) puntos.stream().filter(o ->
                        distanciaKm(c.getLat(), c.getLon(), o.getLat(), o.getLon()) <= radioKm)
                        .count()))
                .orElse(puntos.get(0));

        /* b) incluir todos los puntos dentro de radioKm del seed */
        return puntos.stream()
                .filter(c -> distanciaKm(seed.getLat(), seed.getLon(),
                                        c.getLat(), c.getLon()) <= radioKm)
                .toList();
    }

    /* ------------------------------------------------------------------ *
     *  Helpers
     * ------------------------------------------------------------------ */

    /** Asignación greedy sujeta a capacidad y radio máximo */
    private Map<Puesto,List<Credito>> asignar(List<Credito> clientes,
                                              List<Puesto> puestos,
                                              int capacidad,
                                              double maxDistKm) {
        Map<Puesto,List<Credito>> asig = new HashMap<>();
        puestos.forEach(p -> asig.put(p, new ArrayList<>(capacidad)));

        for (Credito c : clientes) {
            Puesto best = null;
            double bestD = Double.MAX_VALUE;

            for (Puesto p : puestos) {
                List<Credito> lista = asig.get(p);
                if (lista.size() >= capacidad) continue;

                double d = distanciaKm(p.getLat(), p.getLon(),
                                       c.getLat(), c.getLon());
                if (d < bestD) { bestD = d; best = p; }
            }
            if (best != null && bestD <= maxDistKm) asig.get(best).add(c);
        }
        return asig;
    }

    /** Función objetivo: créditos cubiertos – λ·(suma desplazamientos) */
    private double score(Map<Puesto,List<Credito>> asig,
                         List<Puesto> puestos,
                         Map<UUID,double[]> posOriginal,
                         double lambda) {

        int servidos = clientesAsignados(asig);
        double penalty = puestos.stream()
                .mapToDouble(p -> distanciaKm(posOriginal.get(p.getId())[0],
                                              posOriginal.get(p.getId())[1],
                                              p.getLat(), p.getLon()))
                .sum();
        return servidos - lambda * penalty;
    }

    private int clientesAsignados(Map<Puesto,List<Credito>> asig) {
        return asig.values().stream().mapToInt(List::size).sum();
    }

    /* Zona aproximada de México */
    private boolean esMexico(double lat, double lon) {
        return lat >= 14.0 && lat <= 33.0
            && lon >= -119.0 && lon <= -85.0;
    }

    /* Haversine rápido */
    private double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = FastMath.toRadians(lat2 - lat1);
        double dLon = FastMath.toRadians(lon2 - lon1);
        double a = FastMath.sin(dLat / 2) * FastMath.sin(dLat / 2)
                 + FastMath.cos(FastMath.toRadians(lat1))
                 * FastMath.cos(FastMath.toRadians(lat2))
                 * FastMath.sin(dLon / 2) * FastMath.sin(dLon / 2);
        return 2 * R * FastMath.atan2(FastMath.sqrt(a), FastMath.sqrt(1 - a));
    }
}
