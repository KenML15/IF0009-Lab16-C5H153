package ucr.ac.cr.medivault.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ucr.ac.cr.medivault.business.ExpedienteService;
import ucr.ac.cr.medivault.controller.dto.ExpedienteRequestDto;
import ucr.ac.cr.medivault.controller.dto.ExpedienteResponseDto;
import ucr.ac.cr.medivault.controller.dto.NotasRequestDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/expedientes")
public class ExpedienteController {

    private final ExpedienteService expService;

    public ExpedienteController(ExpedienteService expService) {
        this.expService = expService;
    }

    @GetMapping("/mis-expedientes")
    public ResponseEntity<List<ExpedienteResponseDto>> obtenerMisExpedientes(
            Authentication auth) {
        return ResponseEntity.ok(expService.listarExpedientesPropios(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpedienteResponseDto> obtenerPorId(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(expService.consultarPorId(id, auth.getName()));
    }

    @PostMapping
    public ResponseEntity<ExpedienteResponseDto> crearExpediente(
            @Valid @RequestBody ExpedienteRequestDto req, Authentication auth) {
        ExpedienteResponseDto creado =
                expService.registrarExpediente(req, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }


    @PatchMapping("/{id}/notas")
    public ResponseEntity<Void> actualizarNotas(
            @PathVariable Long id,
            @Valid @RequestBody NotasRequestDto req,
            Authentication auth) {
        expService.actualizarNotasDefensivo(id, req.notasClinicas(), auth.getName());
        return ResponseEntity.noContent().build();
    }
}