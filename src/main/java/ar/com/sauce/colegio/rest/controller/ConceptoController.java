package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.dto.ConceptoDto;
import ar.com.sauce.colegio.rest.model.Concepto;
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

@RestController
@RequestMapping("/concepto")
@CrossOrigin(origins = "http://localhost:4200")
public class ConceptoController {

    @Autowired
    private ConceptoService service;

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
}