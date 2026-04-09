package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.dto.CursoDto;
import ar.com.sauce.colegio.rest.model.Curso;
import ar.com.sauce.colegio.rest.service.CursoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
}
