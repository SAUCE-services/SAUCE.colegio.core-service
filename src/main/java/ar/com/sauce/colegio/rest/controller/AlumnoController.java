package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.dto.AlumnoCompletoDto;
import ar.com.sauce.colegio.rest.dto.AlumnoDto;
import ar.com.sauce.colegio.rest.dto.CursoDetalleResponseDto;
import ar.com.sauce.colegio.rest.service.AlumnoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @GetMapping("/{id}")
    public ResponseEntity<AlumnoCompletoDto> getAlumnoCompleto(@PathVariable Long id) {
        try {
            return new ResponseEntity<>(service.obtenerAlumnoCompleto(id), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>((HttpHeaders) null, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/curso-pdf")
    public ResponseEntity<byte[]> descargarPdfAlumnosByCurso(@RequestParam String nombre) {
        // Generamos los bytes del PDF invocando el nuevo método del Service
        byte[] pdfContents = service.generarPdfAlumnosPorCurso(nombre);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        // 'inline' abre de una el visor de PDF del navegador web listo para mandar a imprimir
        headers.add("Content-Disposition", "inline; filename=alumnos_curso_" + nombre.replaceAll(" ", "_") + ".pdf");

        return new ResponseEntity<>(pdfContents, headers, HttpStatus.OK);
    }
}