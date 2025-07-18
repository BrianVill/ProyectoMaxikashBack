package com.loki.estructuraUsuarios.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.loki.estructuraUsuarios.Models.Nivel;
import com.loki.estructuraUsuarios.Repository.NivelRepository;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NivelCacheService {

    private Map<Integer, Nivel> nivelCache;

    @Autowired
    private NivelRepository nivelRepository;

    // Se ejecuta una vez al iniciar la aplicación
    @PostConstruct
    public void init() {
        List<Nivel> nivelList = nivelRepository.findAll();
        nivelCache = nivelList.stream()
                .collect(Collectors.toMap(Nivel::getNivel, nivel -> nivel));
        System.out.println("Nivel precargados en cache: " + nivelCache);
    }

    // Devuelve un nivel en particular según su número
    public Nivel getNivel(int nivel) {
        return nivelCache.get(nivel);
    }

    // Devuelve todos los nivel cacheados
    public Map<Integer, Nivel> getAllNivel() {
        return nivelCache;
    }

    // Si en algún momento se actualizan los nivel, se puede refrescar la cache
    public void refreshCache() {
        init();
    }
}
