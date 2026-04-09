package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.AlumnoDto;
import ar.com.sauce.colegio.rest.dto.CursoDetalleResponseDto;
import ar.com.sauce.colegio.rest.model.Alumno;
import ar.com.sauce.colegio.rest.model.Curso;
import ar.com.sauce.colegio.rest.repository.IAlumnoRepository;
import ar.com.sauce.colegio.rest.repository.ICursoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlumnoService {
    @Autowired
    private IAlumnoRepository alumnoRepository;
    @Autowired
    private ICursoRepository cursoRepository;

    public List<AlumnoDto> findByCursoDto(String cursoNombre) {
        // Buscamos los alumnos y los transformamos al DTO
        return alumnoRepository.findAllByCursoIgnoreCase(cursoNombre).stream()
                .map(alumno -> new AlumnoDto(
                        alumno.getAlumnoId(),
                        alumno.getApellido() + ", " + alumno.getNombre() // Formato exacto de la imagen
                ))
                .collect(Collectors.toList());
    }

    // sauce/colegio/rest/service/AlumnoService.java
    public CursoDetalleResponseDto findCursoConAlumnos(String cursoNombre) {
        String nombreBusqueda = cursoNombre.trim();

        // 1. ✅ Usamos el nuevo método optimizado (1 sola consulta para toda la cabecera)
        Curso curso = cursoRepository.findByDescripcionConDetalles(nombreBusqueda).orElse(null);

        // 2. Traemos la lista de alumnos
        List<AlumnoDto> alumnosDto = alumnoRepository.findAllByCursoIgnoreCase(nombreBusqueda).stream()
                .map(alumno -> new AlumnoDto(
                        alumno.getAlumnoId(),
                        alumno.getApellido() + ", " + alumno.getNombre()
                ))
                .collect(Collectors.toList());

        CursoDetalleResponseDto response = new CursoDetalleResponseDto();
        if (curso != null) {
            // Al usar JOIN FETCH, estos getters ya tienen la información y no van a la DB
            response.setNombreMaestro(curso.getMaestro().getApellido() + ", " + curso.getMaestro().getNombre());
            response.setNombreTurno(curso.getTurno().getDescripcion());
            response.setNombreEstablecimiento(curso.getEstablecimiento().getNombre());
            response.setNombreCiclo(curso.getCiclo().getNombre());
        }
        response.setAlumnos(alumnosDto);
        return response;
    }
}