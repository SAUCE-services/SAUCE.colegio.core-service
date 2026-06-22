package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.dto.*;
import ar.com.sauce.colegio.rest.model.Concepto;
import ar.com.sauce.colegio.rest.repository.IPeriodoRepository;
import ar.com.sauce.colegio.rest.service.ConceptoService;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/concepto")
@CrossOrigin(origins = "http://localhost:4200")
public class ConceptoController {

    private ConceptoService service;
    private final IPeriodoRepository periodoRepository;

    @Autowired
    public ConceptoController(ConceptoService service, IPeriodoRepository periodoRepository) {
        this.service = service;
        this.periodoRepository = periodoRepository;
    }

    @GetMapping("/paginado")
    public ResponseEntity<Page<ConceptoDto>> findAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("ConceptoId").ascending());
        return new ResponseEntity<>(service.findAllPaged(pageable), HttpStatus.OK);
    }

    @PostMapping("/")
    public ResponseEntity<Concepto> save(@RequestBody Concepto concepto) {
        return new ResponseEntity<>(service.save(concepto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Concepto> actualizarConcepto(@PathVariable Long id, @RequestBody ConceptoDto dto) {
        try {
            Concepto editado = service.editarConcepto(id, dto);
            return new ResponseEntity<>(editado, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> descargarPdfTodosLosConceptos() {
        byte[] pdfContents = service.generarPdfTodosLosConceptos();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        // 'inline' permite abrirlo directamente en el visor de Firefox o Chrome listo para imprimir
        headers.add("Content-Disposition", "inline; filename=listado_general_conceptos.pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfContents, headers, HttpStatus.OK);
    }

    // 1. GET para cargar la grilla segmentada por el nombre de texto del período (ej: /novedades/alumno/1484?periodoNombre=MAYO%20-%202026)
    @GetMapping("/novedades/alumno/{alumnoId}")
    public ResponseEntity<NovedadesAlumnoResponseDto> consultarNovedadesAlumno(
            @PathVariable Long alumnoId,
            @RequestParam String periodoNombre) { // 👈 Ahora es un String plano
        return ResponseEntity.ok(service.obtenerNovedadesPorAlumnoYPeriodoNombre(alumnoId, periodoNombre));
    }

    // 2. POST para registrar la novedad manual y refrescar la grilla del mes actual
    @PostMapping("/novedades/agregar")
    public ResponseEntity<List<LineaDetalleDto>> agregarNovedadAlumno(@RequestBody NovedadCargaDto dto) {
        List<LineaDetalleDto> grillaActualizada = service.agregarNovedadManualConPeriodoNombre(dto);
        return new ResponseEntity<>(grillaActualizada, HttpStatus.OK);
    }

    // POST para anular novedad específica de un alumno
    @PostMapping("/novedades/anular")
    public ResponseEntity<List<LineaDetalleDto>> anularNovedadAlumno(@RequestBody NovedadCargaDto dto) {
        return ResponseEntity.ok(service.anularNovedadIndividual(dto));
    }

    /**
     * Obtiene las novedades cargadas para todos los alumnos de un curso en un periodo específico.
     * La URL espera: /concepto/novedades/curso/1484?periodoNombre=MAYO%20-%202026
     */
    @GetMapping("/novedades/curso/{cursoId}")
    public ResponseEntity<?> consultarNovedadesPorCurso(
            @PathVariable Long cursoId,
            @RequestParam String periodo,
            @RequestParam String ciclo) { // 🌟 Nuevo RequestParam en Swagger
        return ResponseEntity.ok(service.obtenerNovedadesPorCurso(cursoId, periodo, ciclo));
    }

    @PostMapping("/novedades/curso/{cursoId}/agregar-todos")
    public ResponseEntity<Void> agregarTodos(@PathVariable Long cursoId, @RequestBody NovedadCargaDto dto,@RequestParam String ciclo) {
        // dto trae periodoNombre, conceptoId e importe
        Long periodoId = periodoRepository.findAll().stream()
                .filter(p -> p.getDescripcion().equalsIgnoreCase(dto.getPeriodoNombre()))
                .findFirst().orElseThrow().getPeriodoId();

        service.procesarNovedadMasiva(cursoId, periodoId, dto.getConceptoId(), dto.getImporte(),ciclo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/novedades/curso/{cursoId}/anular-todos")
    public ResponseEntity<Void> anularTodos(@PathVariable Long cursoId, @RequestParam String periodoNombre,@RequestParam String ciclo) {
        Long periodoId = periodoRepository.findAll().stream()
                .filter(p -> p.getDescripcion().equalsIgnoreCase(periodoNombre))
                .findFirst().orElseThrow().getPeriodoId();

        service.procesarAnulacionMasiva(cursoId, periodoId,ciclo);
        return ResponseEntity.ok().build();
    }
}