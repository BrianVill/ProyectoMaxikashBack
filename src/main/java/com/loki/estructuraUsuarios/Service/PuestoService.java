package com.loki.estructuraUsuarios.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loki.estructuraUsuarios.DTOs.PuestoDTO;
import com.loki.estructuraUsuarios.Exceptions.ResourceNotFoundException;
import com.loki.estructuraUsuarios.Models.Credito;
import com.loki.estructuraUsuarios.Models.Nivel;
import com.loki.estructuraUsuarios.Models.Puesto;
import com.loki.estructuraUsuarios.Repository.CreditoRepository;
import com.loki.estructuraUsuarios.Repository.NivelRepository;
import com.loki.estructuraUsuarios.Repository.PuestoRepository;


import jakarta.persistence.EntityNotFoundException;

import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class PuestoService {

    @Autowired
    private PuestoRepository puestoRepository;

    @Autowired
    private NivelRepository nivelRepository;

    @Autowired
    private CreditoRepository CreditoRepository;

    @Autowired
    private NivelCacheService nivelCacheService;


    // Obtener todos los puestos como DTOs
    public List<PuestoDTO> getAllPuestos() {
        return puestoRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Método actualizado para convertir Puesto a DTO según los requerimientos
    public PuestoDTO convertToDTO(Puesto puesto) {
        // Asegurarse de inicializar la colección de créditos (si es LAZY)
        return new PuestoDTO(
                puesto.getId(),
                puesto.getNombre(),
                puesto.getLat(),
                puesto.getLon(),
                puesto.getIdPadreDirecto(),
                puesto.getNivel() != null ? puesto.getNivel() : 0
        );
    }

    @Transactional
    public Puesto savePuesto(Puesto puesto) {
        // 🔍 Depuración para verificar qué valores llegan antes de la validación
        System.out.println("✅ Entrando a savePuesto()");
        System.out.println("🟢 Datos recibidos en savePuesto()");
        System.out.println("    - ID Puesto: " + puesto.getId());
        // Preload maxNivel once
        Integer maxNivel = nivelRepository.findMaxNivel();
        // Se ejecutan todas las validaciones
        validatePuesto(puesto, maxNivel);

        if (puesto.getId() == null) {
            puesto.setId(UUID.randomUUID());
        }

        if (puesto.getNivel() == null) {
            throw new IllegalArgumentException("El nivel del puesto es requerido.");
        }

        if (puestoRepository.existsById(puesto.getId())) {
            throw new IllegalArgumentException(". El idpuesto '" + puesto.getId() + "' ya está en uso.");
        }


        System.out.println("🟢 Datos después de asignar valores por defecto:");

        return puestoRepository.save(puesto);
    }





    @Transactional
    public List<Puesto> saveAllPuestos(List<PuestoDTO> puestosDTO) {
        
        // 4. Preparar la lista de entidades a guardar
        List<Puesto> puestosToSave = new ArrayList<>();
        
        // 5. Guardar en bloque todos los puestos
        List<Puesto> savedPuestos = puestoRepository.saveAll(puestosToSave);
        

        return savedPuestos;
    }

    // Validación personalizada
    private void validatePuesto(Puesto puesto, Integer maxNivel) {
        List<String> errores = new ArrayList<>();

        // Example validations:
        if (puesto.getNombre() == null || puesto.getNombre().trim().isEmpty()) {
            errores.add("El nombre del puesto no puede estar vacío.");
        }
        if (puesto.getLat() != null && (puesto.getLat() < -90 || puesto.getLat() > 90)) {
            errores.add("La latitud debe estar entre -90 y 90.");
        }
        if (puesto.getLon() != null && (puesto.getLon() < -180 || puesto.getLon() > 180)) {
            errores.add("La longitud debe estar entre -180 y 180.");
        }

        // Hierarchical validations using maxNivel
        if (puesto.getNivel() != null) {
            int nivelPuesto = puesto.getNivel();
            if (nivelPuesto != 1) {
                if (maxNivel != null && nivelPuesto > 1 && nivelPuesto < maxNivel
                        && puesto.getIdPadreDirecto() == null) {
                    errores.add("Los puestos intermedios deben tener un idPadreDirecto definido.");
                } else if (maxNivel != null && nivelPuesto == maxNivel && puesto.getIdPadreDirecto() != null) {
                    errores.add("El puesto del nivel más alto no puede tener idPadreDirecto.");
                }
            }
            if (puesto.getIdPadreDirecto() != null) {
                Optional<Puesto> opPadre = puestoRepository.findById(puesto.getIdPadreDirecto());
                if (opPadre.isEmpty()) {
                    errores.add("El idPadreDirecto proporcionado no existe.");
                } else {
                    Puesto padre = opPadre.get();
                    if (padre.getNivel() == null || padre.getNivel() != nivelPuesto + 1) {
                        errores.add("El idPadreDirecto debe pertenecer a un puesto con un nivel superior.");
                    }
                }
            }

        }

        if (!errores.isEmpty()) {
            throw new IllegalArgumentException("Errores de validación: " + String.join(" | ", errores));
        }
    }

    @Transactional
    public List<Puesto> updateAllPuestos(List<PuestoDTO> puestosDTO) {
        // Pre-cargar el cache de nivel para el lote
        Map<Integer, Nivel> nivelCache = nivelCacheService.getAllNivel();

        // Pre-cargar el valor de maxNivel
        Integer maxNivel = nivelRepository.findMaxNivel();

        List<Puesto> updatedPuestos = new ArrayList<>();
        // Lista para acumular los créditos a actualizar o insertar en lote
        List<Credito> creditosToBatch = new ArrayList<>();

        // Opcional: mapa para acumular las actualizaciones bulk de idGestor
        Map<UUID, UUID> puestosConCambioPadre = new HashMap<>();

        for (PuestoDTO puestoDTO : puestosDTO) {
            Puesto updatedPuesto = puestoRepository.findById(puestoDTO.getId()).map(puesto -> {
                // Capturar el valor antiguo de idPadreDirecto antes de actualizarlo
                UUID oldIdPadreDirecto = puesto.getIdPadreDirecto();

                // Recuperar el nuevo nivel usando el cache
                Nivel newNivel = nivelCache.get(puestoDTO.getNivel());
                if (newNivel == null) {
                    throw new IllegalArgumentException("El nivel especificado no existe.");
                }
                // Actualizar campos del puesto
                puesto.setNombre(puestoDTO.getNombre());
                puesto.setLat(puestoDTO.getLat());
                puesto.setLon(puestoDTO.getLon());

                // Actualizar el idPadreDirecto
                puesto.setIdPadreDirecto(puestoDTO.getIdPadreDirecto());

                // Validar el puesto según reglas del negocio
                validatePuesto(puesto, maxNivel);

                // Guardar la actualización del puesto
                Puesto savedPuesto = puestoRepository.save(puesto);

                // Inicializar la colección de créditos (útil si está en LAZY)

                // Verificar si el idPadreDirecto ha cambiado (incluso de null a no null o
                // viceversa)
                if (!Objects.equals(oldIdPadreDirecto, puestoDTO.getIdPadreDirecto())) {
                    // Aquí podrías, si lo deseas, validar el idPadreDirecto (similar a otros
                    // métodos)
                    if (puestoDTO.getIdPadreDirecto() != null) {
                        Optional<Puesto> padreOpt = puestoRepository.findById(puestoDTO.getIdPadreDirecto());
                        if (padreOpt.isEmpty()) {
                            throw new IllegalArgumentException("El idPadreDirecto proporcionado no existe.");
                        } else {
                            Puesto padre = padreOpt.get();
                            if (padre.getNivel() == null
                                    || padre.getNivel() != savedPuesto.getNivel() + 1) {
                                throw new IllegalArgumentException(
                                        "El idPadreDirecto debe pertenecer a un puesto con un nivel superior.");
                            }
                        }
                    }
                    // Acumular la actualización bulk para este puesto
                    puestosConCambioPadre.put(savedPuesto.getId(), puestoDTO.getIdPadreDirecto());
                }

                return savedPuesto;
            }).orElseThrow(
                    () -> new EntityNotFoundException("Puesto con ID " + puestoDTO.getId() + " no encontrado."));
            updatedPuestos.add(updatedPuesto);
        }

        // Llamar a la actualización bulk de los créditos en base al idPadreDirecto
        // (idGestor)
        /*for (Map.Entry<UUID, UUID> entry : puestosConCambioPadre.entrySet()) {
            UUID puestoId = entry.getKey();
            UUID newIdGestor = entry.getValue();
            // Se actualiza en bulk todos los créditos asociados a ese puesto
            CreditoRepository.updateIdGestorByPuestoId(puestoId, newIdGestor);
        }*/

        // Guardar en lote todos los créditos actualizados o nuevos (los cambios en
        // bucket/asignacion/resolución)
        if (!creditosToBatch.isEmpty()) {
            CreditoRepository.saveAll(creditosToBatch);
            CreditoRepository.flush();
        }

        return updatedPuestos;
    }

    // Eliminar un puesto
    public void deletePuesto(UUID id) {
        puestoRepository.deleteById(id);
    }

    // Nuevo método para filtrar puestos por nombre
    @Transactional
    public List<PuestoDTO> getPuestosByNombre(String nombre) {
        return puestoRepository.findByNombre(nombre)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    

    /*
     * @Autowired
     * private JdbcTemplate jdbcTemplate;
     * 
     * @Transactional
     * public List<Map<String, Object>> getAllPuestosQuick() {
     * String sql = "SELECT " +
     * "u.id, " +
     * "u.idpuesto, " +
     * "u.nombre, " +
     * "u.fecha_de_ingreso, " +
     * "n.nivel as nivel, " +
     * "u.area, " +
     * "u.ubicacion, " +
     * "u.fecha_de_nacimiento, " +
     * "u.sexo, " +
     * "u.no_telefonico, " +
     * "u.correo_electronico, " +
     * "u.domicilio, " +
     * "u.lat, " +
     * "u.lon, " +
     * "u.sueldo, " +
     * "u.id_padre_directo, " +
     * "u.sueldo_final, " +
     * "u.color " +
     * "FROM puesto u " +
     * "JOIN nivel n ON u.nivel_id = n.nivel " +
     * return jdbcTemplate.queryForList(sql);
     * }
     */

    @Transactional
    public List<PuestoDTO> getAllPuestosOrdenadosPorNivel() {
        return puestoRepository.findAllByOrderByNivelAsc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /*
     * @Transactional
     * public List<Map<String, Object>> getPuestosByNivelQuick(int nivel) {
     * String sql = "SELECT " +
     * "u.id, " +
     * "u.idpuesto, " +
     * "u.nombre, " +
     * "u.fecha_de_ingreso, " +
     * "n.nivel as nivel, " +
     * "u.area, " +
     * "u.ubicacion, " +
     * "u.fecha_de_nacimiento, " +
     * "u.sexo, " +
     * "u.no_telefonico, " +
     * "u.correo_electronico, " +
     * "u.domicilio, " +
     * "u.lat, " +
     * "u.lon, " +
     * "u.sueldo, " +
     * "u.id_padre_directo, " +
     * "u.sueldo_final, " +
     * "u.color " +
     * "FROM puesto u " +
     * "JOIN nivel n ON u.nivel_id = n.nivel " +
     * "WHERE u.nivel_id = " + nivel + " " +
     * return jdbcTemplate.queryForList(sql);
     * }
     */

    // Filtrar puestos por nivel
    @Transactional
    public List<PuestoDTO> getPuestosByNivel(int nivel) {
        return puestoRepository.findByNivel(nivel)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Filtrar puestos por idPadreDirecto
    @Transactional
    public List<PuestoDTO> getPuestosByIdPadreDirecto(UUID idPadreDirecto) {
        return puestoRepository.findByIdPadreDirecto(idPadreDirecto)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Obtener puesto por ID
    @Transactional
    public Optional<PuestoDTO> getPuestoDTOById(UUID id) {
        return puestoRepository.findById(id) // Cargar créditos explícitamente
                .map(puesto -> convertToDTO(puesto)); // Convertir a DTO
    }

    @Transactional
    public void deletePuestosByNivel(int nivel) {
        List<Puesto> puestos = puestoRepository.findByNivel(nivel);
        if (puestos.isEmpty()) {
            throw new ResourceNotFoundException(". No hay puestos de nivel " + nivel + " para eliminar.");
        }
        puestoRepository.deleteAll(puestos);

    }
}
