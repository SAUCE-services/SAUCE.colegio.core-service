package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.dto.PeriodoDto;
import ar.com.sauce.colegio.rest.model.Periodo;
import ar.com.sauce.colegio.rest.service.PeriodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest; // Import necesario
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/periodo")
@CrossOrigin(origins = "http://localhost:4200")
public class PeriodoController {

    @Autowired
    private PeriodoService service;

    // ✅ Corregido: Ahora usa el service y RequestParam para evitar el error de "string" en el sort
    @GetMapping("/paginado")
    public ResponseEntity<Page<PeriodoDto>> findAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // Forzamos el orden por ID para que coincida con tu ejecutable
        Pageable pageable = PageRequest.of(page, size, Sort.by("periodoId").descending());
        return new ResponseEntity<>(service.findAllPaged(pageable), HttpStatus.OK);
    }

    @GetMapping("/buscar")
    public ResponseEntity<Page<PeriodoDto>> buscar(
            @RequestParam(required = false) LocalDate primerVenc,
            @RequestParam(required = false) LocalDate segundoVenc,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("periodoId").descending());
        return new ResponseEntity<>(service.findByFiltroFechas(primerVenc, segundoVenc, pageable), HttpStatus.OK);
    }
}