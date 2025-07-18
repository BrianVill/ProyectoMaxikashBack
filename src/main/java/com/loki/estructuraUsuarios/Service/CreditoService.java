package com.loki.estructuraUsuarios.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loki.estructuraUsuarios.DTOs.CreditoDTO;
import com.loki.estructuraUsuarios.Exceptions.ResourceNotFoundException;
import com.loki.estructuraUsuarios.Models.Credito;
import com.loki.estructuraUsuarios.Models.Puesto;
import com.loki.estructuraUsuarios.Repository.CreditoRepository;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CreditoService {

    @Autowired
    private CreditoRepository CreditoRepository;

    @Transactional
    public Credito saveCredito(
            Credito cobros, Puesto puesto, String idCredito, UUID idGestor) {

        // Verificar si ya existe un registro con ese idCredito
        if (CreditoRepository.findByIdCredito(idCredito).isPresent()) {
            throw new IllegalArgumentException(" El idCredito '" + idCredito + "' ya existe.");
        }

        return CreditoRepository.save(cobros);
    }

    @Transactional
    public Credito updateCredito(
            Credito cobros, String idCredito) {

        // Si el idCredito se cambió, verificamos que no exista en otro registro.
        Optional<Credito> existenteOpt = CreditoRepository.findByIdCredito(idCredito);
        if (existenteOpt.isPresent() && existenteOpt.get().getId() != cobros.getId()) {
            throw new IllegalArgumentException(" El idCredito '" + idCredito + "' ya existe en otro registro.");
        }


        return CreditoRepository.save(cobros);
    }

    
    @Transactional(readOnly = true)
    public List<CreditoDTO> getAllCobros() {
        return CreditoRepository.findAll()
                .stream()
                .map(this::convertToCobrosClienteDto) // Cambio aquí
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CreditoDTO getCobrosById(String id) {
        Credito cobro = CreditoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cobro con ID " + id + " no encontrado"));
        return convertToCobrosClienteDto(cobro);
    }

    /*@Transactional(readOnly = true)
    public List<CreditoDTO> getCobrosByGestor(UUID idGestor) {
        List<Credito> cobros = CreditoRepository.findByIdGestor(idGestor);
        if (cobros.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron cobros para el gestor con ID: " + idGestor);
        }
        return cobros.stream().map(this::convertToCobrosClienteDto).collect(Collectors.toList()); // Cambio aquí
    }*/

    @Transactional(readOnly = true)
    public List<CreditoDTO> getCobrosByCredito(String idCredito) {
        List<Credito> cobros = CreditoRepository.findAllByIdCredito(idCredito);
        if (cobros.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron cobros para el idCredito: " + idCredito);
        }
        return cobros.stream().map(this::convertToCobrosClienteDto).collect(Collectors.toList());
    }

    @Transactional
        public void deleteCredito(String id) {
                if (!CreditoRepository.existsById(id)) {
                        throw new ResourceNotFoundException("Cobro con ID " + id + " no encontrado");
                }
                CreditoRepository.deleteById(id);
        }

    public CreditoDTO convertToCobrosClienteDto(Credito cobros) {
        return new CreditoDTO(
            cobros.getId(), // <-- tipo String
            cobros.getNombre(),
            cobros.getColor(),
            cobros.getLat(),
            cobros.getLon()
        );
    }

}
