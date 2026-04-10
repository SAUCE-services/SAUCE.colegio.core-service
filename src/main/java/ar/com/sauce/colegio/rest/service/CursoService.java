package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.CursoDto;
import ar.com.sauce.colegio.rest.model.Curso;
import ar.com.sauce.colegio.rest.repository.ICursoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CursoService {

    private final ICursoRepository repository;

    @Autowired
    public CursoService(ICursoRepository repository) {
        this.repository = repository;
    }

    public Page<CursoDto> findAllPaginated(Pageable pageable) {
        return repository.findAll(pageable).map(this::convertToDto);
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

        if (curso.getCiclo() != null) {
            dto.setNombreCiclo(curso.getCiclo().getNombre()); //
        }

        return dto;
    }

    public Page<CursoDto> findByAnioCiclo(String anio, Pageable pageable) {
        // Usamos el repositorio con paginación y mapeamos cada resultado al DTO
        return repository.findAllByCiclo_NombreContaining(anio, pageable)
                .map(this::convertToDto);
    }

    public List<String> listarNombresDeCiclos() {
        return repository.findAll().stream()
                .map(curso -> curso.getCiclo() != null ? curso.getCiclo().getNombre() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted(Comparator.reverseOrder()) // 👈 acá
                .collect(Collectors.toList());
    }
}