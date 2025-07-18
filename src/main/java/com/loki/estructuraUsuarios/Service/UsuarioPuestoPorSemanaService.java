package com.loki.estructuraUsuarios.Service;

import com.loki.estructuraUsuarios.Models.UsuarioPuestoPorSemana;
import com.loki.estructuraUsuarios.Repository.UsuarioPuestoPorSemanaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UsuarioPuestoPorSemanaService {

    private final UsuarioPuestoPorSemanaRepository repo;

    public UsuarioPuestoPorSemanaService(UsuarioPuestoPorSemanaRepository repo) {
        this.repo = repo;
    }

    /* ------- CRUD ------- */
    public List<UsuarioPuestoPorSemana> getAll()                      { return repo.findAll(); }
    public Optional<UsuarioPuestoPorSemana> getById(Long id)          { return repo.findById(id); }
    public UsuarioPuestoPorSemana save(UsuarioPuestoPorSemana e)      { return repo.save(e); }
    public void delete(Long id)                                       { repo.deleteById(id); }

    /* ------- búsquedas útiles ------- */
    public List<UsuarioPuestoPorSemana> getPorSemana(UUID puestoId, LocalDate start, LocalDate end){
        return repo.findByPuestoAndSemana(puestoId, start, end);
    }

    public Optional<UsuarioPuestoPorSemana> getPorSemanaUsuario(UUID usuarioId, LocalDate start, LocalDate end){
        return repo.findByUsuarioAndSemana(usuarioId, start, end);
    }

    public Optional<UsuarioPuestoPorSemana> getExact(UUID usuarioId, UUID puestoId,
                                                     LocalDate start, LocalDate end){
        return repo.findByUserAndPuestoAndDates(usuarioId, puestoId, start, end);
    }

    public List<UsuarioPuestoPorSemana> getSemana(LocalDate start, LocalDate end){
        return repo.findAllByFechaInicioAndFechaFin(start, end);
    }
}
