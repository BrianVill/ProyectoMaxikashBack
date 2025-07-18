package com.loki.estructuraUsuarios.Controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loki.estructuraUsuarios.DTOs.CreditoMapDTO;
import com.loki.estructuraUsuarios.DTOs.GestorMapDTO;
import com.loki.estructuraUsuarios.Service.MapQueryService;

@RestController
@RequestMapping("/api/map")
public class MapDataController {

    private final MapQueryService mapQueryService;

    public MapDataController(MapQueryService mapQueryService) {
        this.mapQueryService = mapQueryService;
    }

    @GetMapping("/creditos")
    public List<CreditoMapDTO> creditosActivos() {
        return mapQueryService.getAllCreditosActivos();
    }

    @GetMapping("/gestores")
    public List<GestorMapDTO> gestoresActivos() {
        return mapQueryService.getAllGestoresActivos();
    }
}
