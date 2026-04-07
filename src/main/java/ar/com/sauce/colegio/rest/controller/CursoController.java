package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.dto.CursoDto;
import ar.com.sauce.colegio.rest.model.Curso;
import ar.com.sauce.colegio.rest.service.CursoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/curso")
@Slf4j
@RequiredArgsConstructor
public class CursoController {

    private final CursoService service;

    @GetMapping("/")
    public ResponseEntity<List<CursoDto>> findAll() {
        return new ResponseEntity<>(service.findAllDto(), HttpStatus.OK);
    }
}
