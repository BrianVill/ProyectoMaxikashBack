package com.loki.variablesCreditoPorDia.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.loki.variablesCreditoPorDia.DTOs.CondicionesDTO;
import com.loki.variablesCreditoPorDia.Models.ClientesCumpleBono;
import com.loki.variablesCreditoPorDia.Models.ClientesCumpleFiltros;
import com.loki.variablesCreditoPorDia.Models.VariablesCreditoPorDia;
import com.loki.variablesCreditoPorDia.Repository.*;

import jakarta.transaction.Transactional;

@Service
public class ClientesCumpleBonoService {

    @Autowired
    private ClientesCumpleBonoRepository bonoRepo;
    @Autowired
    private ClientesCumpleFiltrosRepository filtrosRepo;
    @Autowired
    private VariablesCreditoPorDiaRepository varRepo;

    @Autowired
    private PuestoCumpleCondicionService puestoCondService;
    @Autowired
    private PuestoCumpleBonoService puestoBonoService;

    @Autowired
    private RestTemplate rest;
    @Value("${endpoints.condiciones-filtro-service-url}")
    private String CONDICIONES_URL;

    @Async
    @Transactional
    public CompletableFuture<Void> evaluarClientesCumpleBono() {

        /* 1. condiciones filtro agrupadas por bono */
        Map<Long, List<Long>> condicionesPorBono = Arrays
                .stream(rest.getForObject(CONDICIONES_URL, CondicionesDTO[].class))
                .filter(c -> c.getTipoId() == 1)
                .collect(Collectors.groupingBy(
                        CondicionesDTO::getBonoId,
                        Collectors.mapping(CondicionesDTO::getId, Collectors.toList())));

        /* 2. última fecha en ClientesCumpleFiltros */
        LocalDate fecha = filtrosRepo.findMaxFecha()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay registros en ClientesCumpleFiltros"));

        /* 3. registros agrupados por crédito */
        Map<String, List<ClientesCumpleFiltros>> registrosPorCredito = filtrosRepo.findByFecha(fecha)
                .stream()
                .collect(Collectors.groupingBy(ClientesCumpleFiltros::getIdCredito));

        /* 4. evaluar cada bono */
        for (Map.Entry<Long, List<Long>> e : condicionesPorBono.entrySet()) {
            Long idBono = e.getKey();
            List<Long> idsCondiciones = e.getValue();

            for (Map.Entry<String, List<ClientesCumpleFiltros>> reg : registrosPorCredito.entrySet()) {
                String idCredito = reg.getKey();
                long cumpleCnt = reg.getValue().stream()
                        .filter(c -> idsCondiciones.contains(c.getIdCondicion()) && c.getCumple())
                        .count();
                boolean cumpleBono = cumpleCnt == idsCondiciones.size();

                /* 5. datos auxiliares */
                List<VariablesCreditoPorDia> vars = varRepo.findByIdCredito(idCredito);
                if (vars.isEmpty())
                    continue;

                VariablesCreditoPorDia ref = vars.get(0);
                UUID puestoId = ref.getPuestoId(); // ← directo
                if (puestoId == null)
                    continue;

                /* 6. upsert */
                ClientesCumpleBono entity = bonoRepo.findByIdCreditoAndIdBono(idCredito, idBono)
                        .orElse(new ClientesCumpleBono());

                entity.setIdCredito(idCredito);
                entity.setPuestoId(puestoId); // ← usa puestoId
                entity.setIdBono(idBono);
                entity.setCumple(cumpleBono);
                entity.setFecha(ref.getFecha());

                bonoRepo.save(entity);
            }
        }

        /* 7. cadenas siguientes */
        puestoCondService.evaluarPuestosCumplenCondiciones();
        puestoBonoService.evaluarPuestosCumplenBonos();

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Variante síncrona para Cloud Tasks.
     */
    public void evaluarClientesCumpleBonoSync() {
        evaluarClientesCumpleBono().join();
    }

    /* CRUD de apoyo */
    public List<ClientesCumpleBono> getAll() {
        return bonoRepo.findAll();
    }

    public List<ClientesCumpleBono> getByCredito(String c) {
        return bonoRepo.findByIdCredito(c);
    }
}
