package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.dto.MaestroDto;
import ar.com.sauce.colegio.rest.dto.MaestroRequestDto;
import ar.com.sauce.colegio.rest.model.Maestro;
import ar.com.sauce.colegio.rest.service.MaestroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/maestro")
@CrossOrigin(origins = "http://localhost:4200")
public class MaestroController {

    @Autowired
    private MaestroService service;

    @GetMapping("/paginado")
    public ResponseEntity<Page<MaestroDto>> findAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("apellido").ascending());
        return new ResponseEntity<>(service.findAllPaged(pageable), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.obtenerPorId(id));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // 🌟 Alta de un maestro nuevo
    @PostMapping("/")
    public ResponseEntity<?> guardar(@RequestBody MaestroRequestDto dto) {
        try {
            Maestro guardado = service.guardarMaestro(null, dto);
            return new ResponseEntity<>(guardado, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al guardar el maestro: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // 🌟 Edición de un maestro existente
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody MaestroRequestDto dto) {
        try {
            Maestro editado = service.guardarMaestro(id, dto);
            return new ResponseEntity<>(editado, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al actualizar el maestro: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}