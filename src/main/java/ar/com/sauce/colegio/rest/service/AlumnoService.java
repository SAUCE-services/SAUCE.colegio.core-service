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
    @Autowired
    private ITipoDocumentoRepository tipoDocumentoRepository;
    @Autowired
    private ITipoNacionalidadRepository tipoNacionalidadRepository;
    @Autowired
    private ITransporteRepository transporteRepository;
    @Autowired
    private INivelEstudioRepository nivelEstudioRepository;
    @Autowired
    private IDepartamentoRepository departamentoRepository;
    @Autowired
    private ILocalidadRepository localidadRepository;
    @Autowired
    private IActividadRepository actividadRepository;
    @Autowired
    private IParentescoRepository parentescoRepository;
    @Autowired
    private IGrupoSanguineoRepository grupoSanguineoRepository;
    @Autowired
    private IObraSocialRepository obraSocialRepository;
    @Autowired
    private IEstablecimientoRepository establecimientoRepository;


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
        // 1. PROCESAR ALUMNO
        Alumno alumno = (dto.getAlumnoId() != null && dto.getAlumnoId() > 0) ?
                alumnoRepository.findById(dto.getAlumnoId()).orElse(new Alumno()) : new Alumno();

        alumno.setApellido(dto.getApellido()); // Evita el error de 'apellido' null
        alumno.setNombre(dto.getNombre());
        alumno.setNroDocumento(dto.getNroDocumento());
        alumno.setFechaNacimiento(dto.getFechaNacimiento());
        alumno.setFechaIngreso(dto.getFechaIngreso());
        alumno.setCurso(dto.getCurso() != null ? dto.getCurso() : "");
        if (alumno.getUuid() == null || alumno.getUuid().isEmpty()) {
            String uuidProvisorio = java.util.UUID.randomUUID().toString().replace("-", "");
            alumno.setUuid(uuidProvisorio);
        }
        establecimientoRepository.findById(1L).ifPresent(alumno::setEstablecimiento);
        // Relaciones del Alumno
        if (dto.getTipoDocumentoId() != null) tipoDocumentoRepository.findById(dto.getTipoDocumentoId()).ifPresent(alumno::setTipoDocumento);
        if (dto.getNacionalidadId() != null) tipoNacionalidadRepository.findById(dto.getNacionalidadId()).ifPresent(alumno::setTipoNacionalidad);
        if (dto.getTransporteId() != null) transporteRepository.findById(dto.getTransporteId()).ifPresent(alumno::setTransporte);

        alumno = alumnoRepository.save(alumno);

        // 2. PROCESAR PADRE
        Padre padre = padreRepository.findByAlumnoAlumnoId(alumno.getAlumnoId());
        if (padre == null) padre = new Padre();
        padre.setAlumno(alumno); // Mantiene la relación con el alumno guardado
        padre.setApellido(dto.getApellidoPadre());
        padre.setNombre(dto.getNombrePadre());
        padre.setNroDocumento(dto.getNroDocumentoPadre());

// --- Relaciones mediante Repositorios ---
// Usamos los IDs específicos del padre definidos en tu DTO
        if (dto.getTipoDocumentoPadreId() != null) {
            tipoDocumentoRepository.findById(dto.getTipoDocumentoPadreId()).ifPresent(padre::setTipoDocumento);
        }

        if (dto.getNivelEstudioPadreId() != null) {
            nivelEstudioRepository.findById(dto.getNivelEstudioPadreId()).ifPresent(padre::setNivelEstudio);
        }

        if (dto.getDepartamentoPadreId() != null) {
            departamentoRepository.findById(dto.getDepartamentoPadreId()).ifPresent(padre::setDepartamento);
        }

        if (dto.getLocalidadPadreId() != null) {
            localidadRepository.findById(dto.getLocalidadPadreId()).ifPresent(padre::setLocalidad);
        }

        if (dto.getActividadPadreId() != null) {
            actividadRepository.findById(dto.getActividadPadreId()).ifPresent(padre::setActividad);
        }

        if (dto.getParentescoPadreId() != null) {
            parentescoRepository.findById(dto.getParentescoPadreId()).ifPresent(padre::setParentesco);
        }
// --- Datos de Contacto y Dirección ---
        padre.setDirCalle(dto.getCallePadre());
        padre.setDirNumero(dto.getNroPadre());
        padre.setDirPiso(dto.getPisoPadre());
        padre.setDirDepto(dto.getDeptoPadre());
        padre.setTelefonoFijo(dto.getTelFijoPadre());
        padre.setTelefonoCelular(dto.getTelCelPadre());
// --- Estado y Guardado ---
// Conversión de boolean (DTO) a Integer (Entidad) según tu modelo
        padre.setPresente(dto.isPresentePadre() ? 1 : 0);
        padreRepository.save(padre);

        // 3. PROCESAR MADRE
        Madre madre = madreRepository.findByAlumnoAlumnoId(alumno.getAlumnoId());
        if (madre == null) madre = new Madre();
        madre.setAlumno(alumno); // Mantiene la relación con el alumno guardado
        madre.setApellido(dto.getApellidoMadre());
        madre.setNombre(dto.getNombreMadre());
        madre.setNroDocumento(dto.getNroDocumentoMadre());

// --- Relaciones mediante Repositorios ---
// Usamos los IDs específicos del madre definidos en tu DTO
        if (dto.getTipoDocumentoMadreId() != null) {
            tipoDocumentoRepository.findById(dto.getTipoDocumentoMadreId()).ifPresent(madre::setTipoDocumento);
        }

        if (dto.getNivelEstudioMadreId() != null) {
            nivelEstudioRepository.findById(dto.getNivelEstudioMadreId()).ifPresent(madre::setNivelEstudio);
        }

        if (dto.getDepartamentoMadreId() != null) {
            departamentoRepository.findById(dto.getDepartamentoMadreId()).ifPresent(madre::setDepartamento);
        }

        if (dto.getLocalidadMadreId() != null) {
            localidadRepository.findById(dto.getLocalidadMadreId()).ifPresent(madre::setLocalidad);
        }

        if (dto.getActividadMadreId() != null) {
            actividadRepository.findById(dto.getActividadMadreId()).ifPresent(madre::setActividad);
        }

        if (dto.getParentescoMadreId() != null) {
            parentescoRepository.findById(dto.getParentescoMadreId()).ifPresent(madre::setParentesco);
        }

// --- Datos de Contacto y Dirección ---
        madre.setDirCalle(dto.getCalleMadre());
        madre.setDirNumero(dto.getNroMadre());
        madre.setDirPiso(dto.getPisoMadre());
        madre.setDirDepto(dto.getDeptoMadre());
        madre.setTelefonoFijo(dto.getTelFijoMadre());
        madre.setTelefonoCelular(dto.getTelCelMadre());

// --- Estado y Guardado ---
// Conversión de boolean (DTO) a Integer (Entidad) según tu modelo
        madre.setPresente(dto.isPresenteMadre() ? 1 : 0);

        madreRepository.save(madre);

        // 4. PROCESAR CARTA MÉDICA
        CartaMedica carta = cartaMedicaRepository.findByAlumnoAlumnoId(alumno.getAlumnoId());
        if (carta == null) carta = new CartaMedica();
        carta.setAlumno(alumno);
        carta.setDescripcionEnfermedad(dto.getEnfermedades());
        carta.setPadeceEnfermedad(dto.isPadeceEnfermedad() ? "1" : "0");
        carta.setMedicamentosToma(dto.getTomaMedicamentos());

// ✅ Mapeo directo de String a String
        carta.setMedicamentosAlergia(dto.getMedicamentosAlergia());

// Otros campos de salud
        if (dto.getGrupoSanguineoId() != null) {
            grupoSanguineoRepository.findById(dto.getGrupoSanguineoId()).ifPresent(carta::setGrupoSanguineo);
        }
        if (dto.getObraSocialId() != null) {
            obraSocialRepository.findById(dto.getObraSocialId()).ifPresent(carta::setObraSocial);
        }

        carta.setTelefonoEmergencia(dto.getTelEmergencia1());
        carta.setTelefonoEmergencia2(dto.getTelEmergencia2());

        cartaMedicaRepository.save(carta);
    }
}