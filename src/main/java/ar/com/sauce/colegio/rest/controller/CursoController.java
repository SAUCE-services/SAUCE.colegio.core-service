package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.dto.CursoDto;
import ar.com.sauce.colegio.rest.dto.DeudaCursoResponseDto;
import ar.com.sauce.colegio.rest.model.Curso;
import ar.com.sauce.colegio.rest.service.CursoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/curso")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CursoController {

    private final CursoService service;

    @GetMapping("/")
    public ResponseEntity<Page<CursoDto>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Definimos el orden por defecto: Ciclo (descendente) para que los últimos años salgan primero
        Pageable pageable = PageRequest.of(page, size, Sort.by("ciclo.nombre").descending());
        return new ResponseEntity<>(service.findAllPaginated(pageable), HttpStatus.OK);
    }

    @GetMapping("/ciclo/{anio}")
    public ResponseEntity<Page<CursoDto>> findByAnio(
            @PathVariable String anio,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Definimos la paginación manualmente para asegurar el orden por descripción
        Pageable pageable = PageRequest.of(page, size, Sort.by("descripcion").ascending());

        return new ResponseEntity<>(service.findByAnioCiclo(anio, pageable), HttpStatus.OK);
    }

    // CursoController.java
    @GetMapping("/ciclos-disponibles")
    public ResponseEntity<List<String>> getCiclosUnicos() {
        // Supongamos que tu service tiene un método que hace un "SELECT DISTINCT nombre FROM Ciclo"
        return new ResponseEntity<>(service.listarNombresDeCiclos(), HttpStatus.OK);
    }

    // 1. Endpoint para exponer los datos a la grilla de la App de Angular
    @GetMapping("/deuda")
    public ResponseEntity<DeudaCursoResponseDto> obtenerDeudaPorCurso(@RequestParam String cursoNombre) {
        return ResponseEntity.ok(service.obtenerDeudaPorCurso(cursoNombre));
    }

    // 2. Endpoint para descargar o previsualizar el reporte PDF de Deudas del Curso
    @GetMapping("/deuda/pdf")
    public ResponseEntity<byte[]> descargarPdfDeudaPorCurso(@RequestParam String cursoNombre) {
        byte[] pdfContents = service.generarPdfDeudaPorCurso(cursoNombre);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        headers.setContentDispositionFormData("inline", "Resumen_Deuda_" + cursoNombre.replace(" ", "_") + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfContents, headers, HttpStatus.OK);
    }
}
