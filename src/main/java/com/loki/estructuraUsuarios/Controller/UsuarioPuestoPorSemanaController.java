package com.loki.estructuraUsuarios.Controller;

import com.loki.estructuraUsuarios.Models.UsuarioPuestoPorSemana;
import com.loki.estructuraUsuarios.Service.UsuarioPuestoPorSemanaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuariopuesto")
public class UsuarioPuestoPorSemanaController {

    @Autowired private UsuarioPuestoPorSemanaService svc;

    /* ---- lectura ---- */
    @GetMapping                  public List<UsuarioPuestoPorSemana> getAll()                   { return svc.getAll(); }
    @GetMapping("/{id}")         public UsuarioPuestoPorSemana getById(@PathVariable Long id)   { return svc.getById(id).orElse(null); }

    @GetMapping("/semana")
    public List<UsuarioPuestoPorSemana> getSemana(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin){
        return svc.getSemana(inicio, fin);
    }

    @GetMapping("/porpuesto")
    public List<UsuarioPuestoPorSemana> getPuestoSemana(
            @RequestParam UUID puestoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin){
        return svc.getPorSemana(puestoId, inicio, fin);
    }

    @GetMapping("/porusuario")
    public UsuarioPuestoPorSemana getUsuarioSemana(
            @RequestParam UUID usuarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin){
        return svc.getPorSemanaUsuario(usuarioId, inicio, fin).orElse(null);
    }

    /* ---- alta / edición ---- */
    @PostMapping                 public UsuarioPuestoPorSemana save(@RequestBody UsuarioPuestoPorSemana e){
        return svc.save(e);
    }

    /* ---- borrar ---- */
    @DeleteMapping("/{id}")      public ResponseEntity<Void> delete(@PathVariable Long id){
        svc.delete(id); return ResponseEntity.noContent().build();
    }
}
