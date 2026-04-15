package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.AlumnoCompletoDto;
import ar.com.sauce.colegio.rest.dto.AlumnoDto;
import ar.com.sauce.colegio.rest.dto.CursoDetalleResponseDto;
import ar.com.sauce.colegio.rest.model.*;
import ar.com.sauce.colegio.rest.repository.*;
import jakarta.transaction.Transactional;
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
    @Autowired
    private IPadreRepository padreRepository;
    @Autowired
    private IMadreRepository madreRepository;
    @Autowired
    private ICartaMedicaRepository cartaMedicaRepository;

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

    @Transactional
    public void guardarAlumnoCompleto(AlumnoCompletoDto dto) {
        // 1. Guardar/Actualizar Alumno
        Alumno alumno = (dto.getAlumnoId() != null) ?
                alumnoRepository.findById(dto.getAlumnoId()).orElse(new Alumno()) : new Alumno();
        // mapear campos...
        alumno = alumnoRepository.save(alumno);

        // 2. Guardar Padre
        Padre padre = new Padre();
        padre.setAlumno(alumno);
        // mapear campos del DTO...
        padreRepository.save(padre);

        // 3. Guardar Madre
        Madre madre = new Madre();
        madre.setAlumno(alumno);
        // mapear campos...
        madreRepository.save(madre);

        // 4. Guardar Carta Médica
        CartaMedica carta = new CartaMedica();
        carta.setAlumno(alumno);
        // mapear campos...
        cartaMedicaRepository.save(carta);
    }
}