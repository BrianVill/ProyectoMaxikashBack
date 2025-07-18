package com.loki.estructuraUsuarios.Service;

import com.loki.estructuraUsuarios.DTOs.UsuarioDTO;
import com.loki.estructuraUsuarios.Exceptions.ResourceNotFoundException;
import com.loki.estructuraUsuarios.Models.Usuario;
import com.loki.estructuraUsuarios.Repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository repo;

    public UsuarioService(UsuarioRepository repo) {
        this.repo = repo;
    }

    /* ======================   CONVERSIÓN  ====================== */

    private UsuarioDTO toDTO(Usuario u){
        return new UsuarioDTO(
                u.getId(),
                u.getNombre(),
                u.getSueldo(),
                u.getSueldoFinal(),
                u.getColor());
    }
    private Usuario toEntity(UsuarioDTO d){
        Usuario u = new Usuario();
        u.setId       (d.getId());
        u.setNombre   (d.getNombre());
        u.setSueldo   (d.getSueldo());
        u.setSueldoFinal(d.getSueldoFinal());
        u.setColor    (d.getColor());
        return u;
    }

    /* ======================   CRUD BÁSICO  ===================== */

    @Transactional
    public UsuarioDTO create(UsuarioDTO dto){
        if (dto.getId()==null) dto.setId(UUID.randomUUID());
        if (repo.findByNombre(dto.getNombre()).isPresent())
            throw new IllegalArgumentException("Ya existe un usuario con nombre "+dto.getNombre());

        Usuario saved = repo.save(toEntity(dto));
        return toDTO(saved);
    }

    @Transactional
    public UsuarioDTO update(UUID id, UsuarioDTO dto){
        Usuario entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no existe: "+id));

        if (dto.getNombre()!=null)       entity.setNombre(dto.getNombre());
        if (dto.getSueldo()!=null)       entity.setSueldo(dto.getSueldo());
        if (dto.getSueldoFinal()!=null)  entity.setSueldoFinal(dto.getSueldoFinal());
        if (dto.getColor()!=null)        entity.setColor(dto.getColor());

        return toDTO(repo.save(entity));
    }

    @Transactional
    public boolean delete(UUID id){
        if (repo.existsById(id)){
            repo.deleteById(id);
            return true;
        }
        return false;
    }

    /* ======================   LECTURA  ========================= */

    public Optional<UsuarioDTO> getById(UUID id){
        return repo.findById(id).map(this::toDTO);
    }

    public List<UsuarioDTO> getAll(){
        return repo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    /* =============  ACCIÓN ESPECÍFICA: Incrementar sueldoFinal ============= */

    @Transactional
    public void incrementarSueldoFinalPorPuestoSemana(UUID puestoId,
                                                      LocalDate fecha,
                                                      Double monto){
        repo.incrementarSueldoFinalPorPuestoSemana(puestoId, fecha, monto);
    }
}
