package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.CursoDto;
import ar.com.sauce.colegio.rest.model.Curso;
import ar.com.sauce.colegio.rest.repository.ICursoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CursoService {

    private final ICursoRepository repository;

    @Autowired
    public CursoService(ICursoRepository repository) {
        this.repository = repository;
    }

    public List<CursoDto> findAllDto() {
        return repository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
    }

    private CursoDto convertToDto(Curso curso) {
        CursoDto dto = new CursoDto();
        dto.setCursoId(curso.getCursoId());
        dto.setDescripcion(curso.getDescripcion());

        // Concatenación del nombre del Maestro
        if (curso.getMaestro() != null) {
            String nombreCompleto = curso.getMaestro().getApellido() + ", " + curso.getMaestro().getNombre();
            dto.setNombreMaestro(nombreCompleto);
        }

        // Otros campos del DTO
        if (curso.getEstablecimiento() != null) {
            dto.setNombreEstablecimiento(curso.getEstablecimiento().getNombre());
        }

        if (curso.getTurno() != null) {
            dto.setNombreTurno(curso.getTurno().getDescripcion());
        }

        return dto;
    }
}