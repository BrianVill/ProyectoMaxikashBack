package com.loki.estructuraUsuarios.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.loki.estructuraUsuarios.DTOs.CreditoMapDTO;
import com.loki.estructuraUsuarios.DTOs.GestorMapDTO;
import com.loki.estructuraUsuarios.Models.CreditoPuestoPorSemana;
import com.loki.estructuraUsuarios.Models.Puesto;
import com.loki.estructuraUsuarios.Models.Usuario;
import com.loki.estructuraUsuarios.Repository.CreditoPuestoPorSemanaRepository;
import com.loki.estructuraUsuarios.Repository.CreditoRepository;
import com.loki.estructuraUsuarios.Repository.PuestoRepository;
import com.loki.estructuraUsuarios.Repository.UsuarioPuestoPorSemanaRepository;
import com.loki.estructuraUsuarios.Repository.UsuarioRepository;

@Service
public class MapQueryService {

    private final CreditoRepository creditoRepo;
    private final CreditoPuestoPorSemanaRepository cppsRepo;
    private final UsuarioPuestoPorSemanaRepository uppsRepo;
    private final PuestoRepository  puestoRepo;
    private final UsuarioRepository usuarioRepo;

    public MapQueryService(CreditoRepository creditoRepo,
                           CreditoPuestoPorSemanaRepository cppsRepo,
                           UsuarioPuestoPorSemanaRepository uppsRepo,
                           PuestoRepository puestoRepo,
                           UsuarioRepository usuarioRepo) {
        this.creditoRepo = creditoRepo;
        this.cppsRepo = cppsRepo;
        this.uppsRepo = uppsRepo;
        this.puestoRepo  = puestoRepo;
        this.usuarioRepo = usuarioRepo;
    }

    /* ------------------------------------------------ Creditos ------------------------------------------------ */

    public List<CreditoMapDTO> getAllCreditosActivos() {
        // 1) Calculamos lunes y domingo de la semana actual
        /* current week bounds */
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate sunday = LocalDate.now().with(DayOfWeek.SUNDAY);

        // 2) Consultamos todos los registros de CreditoPuestoPorSemana en ese rango y con puestoId == null
        List<CreditoPuestoPorSemana> registros = cppsRepo
            .findAllByFechaInicioAndFechaFinAndPuestoIdIsNull(monday, sunday);

        // 3) Extraemos los IDs de crédito
        List<String> creditoIds = registros.stream()
            .map(CreditoPuestoPorSemana::getCreditoId)
            .distinct()      // opcional, para quitar duplicados
            .toList();

        // 4) Recuperamos los objetos Credito a partir de esos IDs
        return creditoRepo.findAllById(creditoIds)
                          .stream()
                          .map(c -> new CreditoMapDTO(
                                  c.getId(),
                                  c.getNombre(),
                                  c.getLat(),
                                  c.getLon(),
                                  c.getColor()))
                          .toList();
    }

    /* ------------------------------------------------ Gestores ------------------------------------------------ */

    public List<GestorMapDTO> getAllGestoresActivos() {

        List<Puesto> gestores = puestoRepo.findByNivel(2);

        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate sunday = LocalDate.now().with(DayOfWeek.SUNDAY);

        return gestores.stream().map(g -> {

            /* First usuario for this puesto this week */
            UUID userId = uppsRepo.findUsuarioIdByPuestoAndSemana(g.getId(),monday,sunday);
            Optional<Usuario> user = usuarioRepo.findById(userId);
            if (!user.isPresent()) {
                System.out.println("The list is empty.");
                return new GestorMapDTO(
                    g.getId(),
                    g.getNombre(),
                    g.getLat(),
                    g.getLon(),
                    "amarillo",     // << color from Usuario
                    "NO HAY USUARIO"       // first usuario name
            );
            }
            Usuario u = user.get();
            
            /*usuarioRepo
                    .findTopByPuestoIdAndSemana(g.getId(), LocalDate.now())
                    .orElse(null);*/

            String usuarioName  = (u != null) ? u.getNombre() : "SIN_USUARIO";
            String usuarioColor = (u != null && u.getColor() != null)
                                    ? u.getColor()
                                    : "rojo";          // default if missing

            return new GestorMapDTO(
                    g.getId(),
                    g.getNombre(),
                    g.getLat(),
                    g.getLon(),
                    usuarioColor,     // << color from Usuario
                    usuarioName       // first usuario name
            );
        }).toList();
    }
}
