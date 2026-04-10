package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.dto.ConceptoDto;
import ar.com.sauce.colegio.rest.model.Concepto;
import ar.com.sauce.colegio.rest.service.ConceptoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
}