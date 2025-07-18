package com.loki.estructuraUsuarios.Service;

import com.loki.estructuraUsuarios.Models.CreditoPuestoPorSemana;
import com.loki.estructuraUsuarios.Repository.CreditoPuestoPorSemanaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CreditoPuestoPorSemanaService {

    private final CreditoPuestoPorSemanaRepository repo;

    public CreditoPuestoPorSemanaService(CreditoPuestoPorSemanaRepository repo) {
        this.repo = repo;
    }

    /* -------- CRUD básico -------- */
    public List<CreditoPuestoPorSemana> getAll()                         { return repo.findAll(); }
    public Optional<CreditoPuestoPorSemana> getById(Long id)             { return repo.findById(id); }
    public CreditoPuestoPorSemana save(CreditoPuestoPorSemana e)         { return repo.save(e); }
    public void delete(Long id)                                          { repo.deleteById(id); }

    /* -------- utilidades de búsqueda -------- */
    public Optional<CreditoPuestoPorSemana> getSemana(String credito, LocalDate fecha){
        return repo.findSemana(credito, fecha);
    }

    public List<CreditoPuestoPorSemana> getSemana(LocalDate start, LocalDate end){
        return repo.findByFechaInicioAndFechaFin(start, end);
    }

    public List<CreditoPuestoPorSemana> sinPuesto(LocalDate start, LocalDate end){
        return repo.findAllByFechaInicioAndFechaFinAndPuestoIdIsNull(start, end);
    }
}
