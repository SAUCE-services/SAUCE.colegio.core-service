package ar.com.sauce.colegio.rest.controller;

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
public class AlumnoController {

    private final AlumnoService service;

    @GetMapping("/curso")
    public ResponseEntity<CursoDetalleResponseDto> getAlumnosByCurso(@RequestParam String nombre) {
        // Cambiamos a findCursoConAlumnos para que traiga Ciclo, Turno, Maestro, etc.
        return new ResponseEntity<>(service.findCursoConAlumnos(nombre), HttpStatus.OK);
    }
}