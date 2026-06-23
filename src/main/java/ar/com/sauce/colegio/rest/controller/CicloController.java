package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.dto.CicloLectivoDto;
import ar.com.sauce.colegio.rest.service.CicloService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ciclos-lectivos")
@CrossOrigin(origins = "http://localhost:4200")
public class CicloController {

    private final CicloService service;

    public CicloController(CicloService service) {
        this.service = service;
    }

    // Para cargar la grilla al iniciar la pantalla
    @GetMapping
    public ResponseEntity<List<CicloLectivoDto>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    // Maneja tanto el botón "Agregar" como "Actualizar"
    @PostMapping("/guardar")
    public ResponseEntity<CicloLectivoDto> guardar(@RequestBody CicloLectivoDto dto) {
        return ResponseEntity.ok(service.guardarOCorregir(dto));
    }
}