package com.loki.estructuraUsuarios.Service;

import com.loki.estructuraUsuarios.Models.EstadosAutomatizaciones;
import com.loki.estructuraUsuarios.Repository.EstadosAutomatizacionesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Gestión de la tabla <estados_automatizaciones>.
 *
 *  • crear(...)      → siempre INSERT (id=null)         ⇢ registra “En Proceso”, etc.
 *  • actualizar(...) → UPSERT por nombre (update/insert)⇢ mantiene UNA fila viva.
 *  • guardar(...)    → save directo (respeta id)
 *  • obtener*/
@Service
public class EstadosAutomatizacionesService {

    @Autowired
    private EstadosAutomatizacionesRepository repo;

    /* ──────────── CRUD lectura / delete ──────────── */
    public List<EstadosAutomatizaciones> obtenerTodos()              { return repo.findAll(); }
    public Optional<EstadosAutomatizaciones> obtenerPorId(Long id)   { return repo.findById(id); }
    public Optional<EstadosAutomatizaciones> obtenerPorNombre(String n){ return repo.findByNombre(n); }
    public void eliminar(Long id)                                    { repo.deleteById(id); }

    /* ──────────── save directo ──────────── */
    public EstadosAutomatizaciones guardar(EstadosAutomatizaciones e){ return repo.save(e); }

    /* ──────────── crear (solo INSERT) ──────────── */
    @Transactional
    public EstadosAutomatizaciones crear(String nombre,String estado,String msg){
        EstadosAutomatizaciones e = new EstadosAutomatizaciones(
                null, nombre, estado, msg, LocalDate.now());
        return repo.save(e);        // fuerza INSERT
    }
    @Transactional
    public EstadosAutomatizaciones crear(EstadosAutomatizaciones e){
        e.setId(null);
        e.setFecha(LocalDate.now());
        return repo.save(e);        // fuerza INSERT
    }

    /* ──────────── actualizar (UPSERT por nombre) ──────────── */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EstadosAutomatizaciones actualizar(String nombre,
                                              EstadosAutomatizaciones nuevo){

        return repo.findByNombre(nombre)
                   .map(e -> {                       // UPDATE
                       e.setEstado (nuevo.getEstado());
                       e.setMensaje(nuevo.getMensaje());
                       e.setFecha  (LocalDate.now());
                       return e;
                   })
                   .orElseGet(() -> {               // INSERT
                       nuevo.setNombre(nombre);
                       nuevo.setFecha(LocalDate.now());
                       return repo.save(nuevo);
                   });
    }
}
