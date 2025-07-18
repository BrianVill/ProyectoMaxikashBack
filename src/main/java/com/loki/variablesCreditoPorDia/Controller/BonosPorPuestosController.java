package com.loki.variablesCreditoPorDia.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.loki.variablesCreditoPorDia.Service.BonosPorPuestosService;

@RestController
@RequestMapping("/bonospuestos")
public class BonosPorPuestosController {

    @Autowired private BonosPorPuestosService svc;

    /* cálculo principal */
    @PostMapping("/calcular")
    public ResponseEntity<String> calcular() {
        svc.calcularBonosPuestos();
        return ResponseEntity.ok("✅ Cálculo de bonos iniciado.");
    }

    /* reset sueldos + registros (opcional) */
    @PostMapping("/resetear")
    public ResponseEntity<String> resetear() {
        svc.resetearBonosYSueldos();
        return ResponseEntity.ok("✅ Bonos y sueldos reseteados.");
    }

    /* lectura */
    @GetMapping                   public List<Map<String,Object>> getAll() { return svc.getAll(); }
    @GetMapping("/{puestoId}")    public List<Map<String,Object>> getPorPuesto(@PathVariable UUID puestoId){
        return svc.getByPuesto(puestoId);
    }

    /* filtrar por rango de fechas */
    @GetMapping("/fechas")
    public List<Map<String,Object>> getEntreFechas(
            @RequestParam @DateTimeFormat(pattern="dd/MM/yyyy") LocalDate ini,
            @RequestParam @DateTimeFormat(pattern="dd/MM/yyyy") LocalDate fin){
        return svc.getByFechaBetween(ini, fin);
    }

    /* vaciar tabla */
    @DeleteMapping
    public ResponseEntity<Void> deleteAll(){
        svc.deleteAllBonosPorPuestos();
        return ResponseEntity.noContent().build();
    }
}
