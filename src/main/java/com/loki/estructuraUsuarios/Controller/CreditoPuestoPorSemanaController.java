package com.loki.estructuraUsuarios.Controller;

import com.loki.estructuraUsuarios.Models.CreditoPuestoPorSemana;
import com.loki.estructuraUsuarios.Service.CreditoPuestoPorSemanaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/creditopuesto")
public class CreditoPuestoPorSemanaController {

    @Autowired private CreditoPuestoPorSemanaService svc;

    /* ----- lectura ----- */
    @GetMapping                 public List<CreditoPuestoPorSemana> getAll()                 { return svc.getAll(); }
    @GetMapping("/{id}")        public CreditoPuestoPorSemana getById(@PathVariable Long id) { return svc.getById(id).orElse(null); }

    /* buscar por semana (YYYY-MM-DD) */
    @GetMapping("/semana")
    public List<CreditoPuestoPorSemana> getSemana(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin){
        return svc.getSemana(inicio, fin);
    }

    /* créditos sin puesto en una semana */
    @GetMapping("/sinpuesto")
    public List<CreditoPuestoPorSemana> sinPuesto(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin){
        return svc.sinPuesto(inicio, fin);
    }

    /* ----- alta / edición ----- */
    @PostMapping                public CreditoPuestoPorSemana save(@RequestBody CreditoPuestoPorSemana e){ return svc.save(e); }

    /* ----- borrar ----- */
    @DeleteMapping("/{id}")     public ResponseEntity<Void> delete(@PathVariable Long id){
        svc.delete(id); return ResponseEntity.noContent().build();
    }
}
