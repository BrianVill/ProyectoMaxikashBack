package com.loki.variablesCreditoPorDia.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loki.bonos.Service.VariablesService;
import com.loki.estructuraUsuarios.Repository.CreditoRepository;
import com.loki.estructuraUsuarios.Repository.CreditoPuestoPorSemanaRepository;
import com.loki.variablesCreditoPorDia.DTOs.VariablesCreditoPorDiaDTO;
import com.loki.variablesCreditoPorDia.Exceptions.ResourceNotFoundException;
import com.loki.variablesCreditoPorDia.Models.VariablesCreditoPorDia;
import com.loki.variablesCreditoPorDia.Repository.VariablesCreditoPorDiaRepository;

@Service
public class VariablesCreditoPorDiaService {

    private final VariablesCreditoPorDiaRepository repo;
    private final CreditoRepository                creditoRepo;
    private final CreditoPuestoPorSemanaRepository cppsRepo;
    private final VariablesService                 variablesService;

    public VariablesCreditoPorDiaService(VariablesCreditoPorDiaRepository repo,
                                         CreditoRepository creditoRepo,
                                         CreditoPuestoPorSemanaRepository cppsRepo,
                                         VariablesService variablesService) {
        this.repo             = repo;
        this.creditoRepo      = creditoRepo;
        this.cppsRepo         = cppsRepo;
        this.variablesService = variablesService;
    }

    /* ===================================== CREATE (single) ===================================== */

    @Transactional
    public VariablesCreditoPorDiaDTO create(VariablesCreditoPorDiaDTO dto) {

        /* -------- validaciones -------- */
        if (dto.getIdCredito()==null || dto.getIdCredito().isBlank())
            throw new IllegalArgumentException("idCredito vacío.");
        String idCredito = dto.getIdCredito().trim();

        if (!creditoRepo.existsById(idCredito))
            throw new ResourceNotFoundException("No existe crédito "+idCredito);

        variablesService.findById(dto.getIdVariable())
                .orElseThrow(() -> new ResourceNotFoundException("Variable no existe: "+dto.getIdVariable()));

        /* fecha válida */
        LocalDate fecha = (dto.getFecha()!=null)? dto.getFecha() : LocalDate.now();
        if (fecha.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("La fecha no puede ser futura.");

        /* -------- puestoId para la semana -------- */
        UUID puestoId = cppsRepo.findSemana(idCredito, fecha)
                                .map(cp -> cp.getPuestoId())
                                .orElse(null);

        /* -------- upsert -------- */
        Optional<VariablesCreditoPorDia> opt = repo
                .findByIdCreditoAndIdVariableAndFecha(idCredito, dto.getIdVariable(), fecha);

        VariablesCreditoPorDia saved;
        if (opt.isPresent()) {
            VariablesCreditoPorDia e = opt.get();
            e.setValor(dto.getValor());
            saved = repo.save(e);
        } else {
            VariablesCreditoPorDia e = new VariablesCreditoPorDia(
                    idCredito,
                    puestoId,
                    dto.getIdVariable(),
                    dto.getValor());
            e.setFecha(fecha);
            saved = repo.save(e);
        }

        return new VariablesCreditoPorDiaDTO(
                saved.getId(),
                saved.getIdCredito(),
                saved.getPuestoId(),
                saved.getIdVariable(),
                saved.getFecha(),
                saved.getValor());
    }

    /* ===================================== CREATE (batch) ===================================== */

    @Transactional
    public List<VariablesCreditoPorDiaDTO> createMany(List<VariablesCreditoPorDiaDTO> dtos) {
        if (dtos==null || dtos.isEmpty())
            throw new IllegalArgumentException("lista vacía.");
        return dtos.parallelStream()
                   .map(this::create)
                   .collect(Collectors.toList());
    }

    /* ===================================== UPDATE ===================================== */

    @Transactional
    public VariablesCreditoPorDiaDTO update(Long id, VariablesCreditoPorDiaDTO dto) {
        VariablesCreditoPorDia entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe registro "+id));

        if (dto.getFecha()!=null && dto.getFecha().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Fecha futura.");

        entity.setFecha(dto.getFecha()!=null? dto.getFecha(): entity.getFecha());
        entity.setValor(dto.getValor());

        VariablesCreditoPorDia saved = repo.save(entity);

        return new VariablesCreditoPorDiaDTO(
                saved.getId(),
                saved.getIdCredito(),
                saved.getPuestoId(),
                saved.getIdVariable(),
                saved.getFecha(),
                saved.getValor());
    }

    /* ===================================== READ / DELETE ===================================== */

    @Transactional public Optional<VariablesCreditoPorDiaDTO> getById(Long id){
        return repo.findById(id)
                   .map(e -> new VariablesCreditoPorDiaDTO(
                           e.getId(), e.getIdCredito(), e.getPuestoId(),
                           e.getIdVariable(), e.getFecha(), e.getValor()));
    }

    @Transactional public List<VariablesCreditoPorDiaDTO> getAll(){
        return repo.findAll().stream()
                   .map(e -> new VariablesCreditoPorDiaDTO(
                           e.getId(), e.getIdCredito(), e.getPuestoId(),
                           e.getIdVariable(), e.getFecha(), e.getValor()))
                   .collect(Collectors.toList());
    }

    @Transactional public boolean delete(Long id){
        if(repo.existsById(id)){ repo.deleteById(id); return true; }
        return false;
    }
    @Transactional public void deleteAll(){ repo.deleteAll(); }
}
