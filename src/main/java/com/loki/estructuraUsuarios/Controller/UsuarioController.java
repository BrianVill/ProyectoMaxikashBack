package com.loki.estructuraUsuarios.Controller;

import com.loki.estructuraUsuarios.DTOs.UsuarioDTO;
import com.loki.estructuraUsuarios.Service.UsuarioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService svc;
    public UsuarioController(UsuarioService svc){ this.svc = svc; }

    /* -------------------  CRUD REST ------------------- */

    @GetMapping
    public List<UsuarioDTO> getAll(){
        return svc.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> getById(@PathVariable UUID id){
        return svc.getById(id)
                  .map(ResponseEntity::ok)
                  .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> create(@RequestBody UsuarioDTO dto){
        return ResponseEntity.status(HttpStatus.CREATED).body(svc.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> update(@PathVariable UUID id,
                                             @RequestBody  UsuarioDTO dto){
        return ResponseEntity.ok(svc.update(id,dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id){
        return svc.delete(id)
               ? ResponseEntity.noContent().build()
               : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /* ----------  Acción específica: incrementar sueldoFinal  ---------- */
    @PostMapping("/incrementar-sueldo")
    public ResponseEntity<Void> incSueldo(@RequestParam UUID puestoId,
                                          @RequestParam Double monto,
                                          @RequestParam
                                          @DateTimeFormat(pattern="dd/MM/yyyy") LocalDate fecha){
        svc.incrementarSueldoFinalPorPuestoSemana(puestoId,fecha,monto);
        return ResponseEntity.ok().build();
    }
}
