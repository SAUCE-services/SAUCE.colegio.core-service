package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.dto.AlumnoCompletoDto;
import ar.com.sauce.colegio.rest.dto.AlumnoDto;
import ar.com.sauce.colegio.rest.dto.CursoDetalleResponseDto;
import ar.com.sauce.colegio.rest.service.AlumnoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alumno")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AlumnoController {

    private final AlumnoService service;

    @GetMapping("/curso")
    public ResponseEntity<CursoDetalleResponseDto> getAlumnosByCurso(@RequestParam String nombre) {
        // Cambiamos a findCursoConAlumnos para que traiga Ciclo, Turno, Maestro, etc.
        return new ResponseEntity<>(service.findCursoConAlumnos(nombre), HttpStatus.OK);
    }

    @PostMapping("/completo")
    public ResponseEntity<String> guardarCompleto(@RequestBody AlumnoCompletoDto dto) {
        try {
            service.guardarAlumnoCompleto(dto);
            return new ResponseEntity<>("Datos guardados correctamente", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al guardar: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}