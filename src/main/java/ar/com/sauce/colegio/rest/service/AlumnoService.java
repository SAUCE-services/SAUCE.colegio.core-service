package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.AlumnoCompletoDto;
import ar.com.sauce.colegio.rest.dto.AlumnoDto;
import ar.com.sauce.colegio.rest.dto.CursoDetalleResponseDto;
import ar.com.sauce.colegio.rest.model.*;
import ar.com.sauce.colegio.rest.repository.*;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    // Dentro de ar.com.sauce.colegio.rest.service.AlumnoService.java
    public CursoDetalleResponseDto findCursoConAlumnos(String cursoNombre) {
        String nombreBusqueda = cursoNombre.trim();

        // 1. ✅ Trae la lista de coincidencia y evita de raíz el IncorrectResultSizeDataAccessException
        List<Curso> cursosConincidentes = cursoRepository.findByDescripcionConDetalles(nombreBusqueda);

        // Tomamos el primero de la lista de manera segura
        Curso curso = cursosConincidentes.stream().findFirst().orElse(null);

        // 2. Traemos la lista de alumnos
        List<AlumnoDto> alumnosDto = alumnoRepository.findAllByCursoIgnoreCase(nombreBusqueda).stream()
                .map(alumno -> new AlumnoDto(
                        alumno.getAlumnoId(),
                        alumno.getApellido() + ", " + alumno.getNombre()
                ))
                .collect(Collectors.toList());

        CursoDetalleResponseDto response = new CursoDetalleResponseDto();
        if (curso != null) {
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
        // 1. PROCESAR ALUMNO (O crear o recuperar existente)
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

    public AlumnoCompletoDto obtenerAlumnoCompleto(Long alumnoId) {
        // 1. Buscamos el alumno (base)
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado con legajo: " + alumnoId));

        AlumnoCompletoDto dto = new AlumnoCompletoDto();

        // Mapeo Datos Alumno
        dto.setAlumnoId(alumno.getAlumnoId());
        dto.setApellido(alumno.getApellido());
        dto.setNombre(alumno.getNombre());
        dto.setNroDocumento(alumno.getNroDocumento());
        dto.setCurso(alumno.getCurso());
        dto.setFechaNacimiento(alumno.getFechaNacimiento());
        dto.setFechaIngreso(alumno.getFechaIngreso());
        if (alumno.getTipoDocumento() != null) dto.setTipoDocumentoId(alumno.getTipoDocumento().getTipoDocumentoId());
        if (alumno.getTipoNacionalidad() != null) dto.setNacionalidadId(alumno.getTipoNacionalidad().getTipoNacionalidadId());
        if (alumno.getTransporte() != null) dto.setTransporteId(alumno.getTransporte().getTransporteId());

        // 2. Buscamos y mapeamos al Padre
        Padre padre = padreRepository.findByAlumnoAlumnoId(alumnoId);
        if (padre != null) {
            dto.setApellidoPadre(padre.getApellido());
            dto.setNombrePadre(padre.getNombre());
            dto.setNroDocumentoPadre(padre.getNroDocumento());
            dto.setCallePadre(padre.getDirCalle());
            dto.setNroPadre(padre.getDirNumero());
            dto.setPisoPadre(padre.getDirPiso());
            dto.setDeptoPadre(padre.getDirDepto());
            dto.setTelFijoPadre(padre.getTelefonoFijo());
            dto.setTelCelPadre(padre.getTelefonoCelular());
            dto.setPresentePadre(padre.getPresente() != null && padre.getPresente() == 1);
            if (padre.getTipoDocumento() != null) dto.setTipoDocumentoPadreId(padre.getTipoDocumento().getTipoDocumentoId());
            if (padre.getNivelEstudio() != null) dto.setNivelEstudioPadreId(padre.getNivelEstudio().getNivelEstudioId());
            if (padre.getDepartamento() != null) dto.setDepartamentoPadreId(padre.getDepartamento().getDepartamentoId());
            if (padre.getLocalidad() != null) dto.setLocalidadPadreId(padre.getLocalidad().getLocalidadId());
            if (padre.getActividad() != null) dto.setActividadPadreId(padre.getActividad().getActividadId());
            if (padre.getParentesco() != null) dto.setParentescoPadreId(padre.getParentesco().getParentescoId());
        }

        // 3. Buscamos y mapeamos a la Madre
        Madre madre = madreRepository.findByAlumnoAlumnoId(alumnoId);
        if (madre != null) {
            dto.setApellidoMadre(madre.getApellido());
            dto.setNombreMadre(madre.getNombre());
            dto.setNroDocumentoMadre(madre.getNroDocumento());
            dto.setCalleMadre(madre.getDirCalle());
            dto.setNroMadre(madre.getDirNumero());
            dto.setPisoMadre(madre.getDirPiso());
            dto.setDeptoMadre(madre.getDirDepto());
            dto.setTelFijoMadre(madre.getTelefonoFijo());
            dto.setTelCelMadre(madre.getTelefonoCelular());
            dto.setPresenteMadre(madre.getPresente() != null && madre.getPresente() == 1);
            if (madre.getTipoDocumento() != null) dto.setTipoDocumentoMadreId(madre.getTipoDocumento().getTipoDocumentoId());
            if (madre.getNivelEstudio() != null) dto.setNivelEstudioMadreId(madre.getNivelEstudio().getNivelEstudioId());
            if (madre.getDepartamento() != null) dto.setDepartamentoMadreId(madre.getDepartamento().getDepartamentoId());
            if (madre.getLocalidad() != null) dto.setLocalidadMadreId(madre.getLocalidad().getLocalidadId());
            if (madre.getActividad() != null) dto.setActividadMadreId(madre.getActividad().getActividadId());
            if (madre.getParentesco() != null) dto.setParentescoMadreId(madre.getParentesco().getParentescoId());
        }

        // 4. Buscamos y mapeamos la Carta Médica
        CartaMedica carta = cartaMedicaRepository.findByAlumnoAlumnoId(alumnoId);
        if (carta != null) {
            dto.setEnfermedades(carta.getDescripcionEnfermedad());
            dto.setPadeceEnfermedad("1".equals(carta.getPadeceEnfermedad()));
            dto.setTomaMedicamentos(carta.getMedicamentosToma());
            dto.setMedicamentosAlergia(carta.getMedicamentosAlergia());
            dto.setTelEmergencia1(carta.getTelefonoEmergencia());
            dto.setTelEmergencia2(carta.getTelefonoEmergencia2());
            if (carta.getGrupoSanguineo() != null) dto.setGrupoSanguineoId(carta.getGrupoSanguineo().getGrupoSanguineoId());
            if (carta.getObraSocial() != null) dto.setObraSocialId(carta.getObraSocial().getObraSocialId());
        }

        return dto;
    }

    public byte[] generarPdfAlumnosPorCurso(String cursoNombre) {
        // 1. Recuperamos el DTO estructurado con toda la metadata del curso y sus alumnos asignados
       CursoDetalleResponseDto datosCurso = findCursoConAlumnos(cursoNombre);

        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);

        // Mantenemos tus márgenes estándar (40 a la izquierda para los ganchos de la carpeta)
        document.setMargins(20, 20, 20, 40);

        // --- ENCABEZADO ---
        document.add(new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion))
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(8)
                .setMarginBottom(0));

        document.add(new Paragraph(datosCurso.getNombreEstablecimiento() != null ? datosCurso.getNombreEstablecimiento() : "Unión Vecinal de Servicios Públicos El Sauce - Colegio")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold().setMarginTop(0).setFontSize(11));

        document.add(new Paragraph("Alumnos por Curso")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold().setFontSize(13));

        // --- BLOQUE DE DETALLE DEL CURSO (CABECERA INFORMATIVA) ---
        Table headerTable = new Table(new float[]{1.5f, 4.5f, 1f, 3f}).useAllAvailableWidth();
        headerTable.setMarginBottom(15);
        headerTable.setMarginTop(10);

        headerTable.addCell(new Cell().add(new Paragraph("Curso:")).setBold().setFontSize(9).setBorder(Border.NO_BORDER));
        headerTable.addCell(new Cell().add(new Paragraph(cursoNombre.toUpperCase())).setFontSize(9).setBorder(Border.NO_BORDER));
        headerTable.addCell(new Cell().add(new Paragraph("Ciclo:")).setBold().setFontSize(9).setBorder(Border.NO_BORDER));
        headerTable.addCell(new Cell().add(new Paragraph(datosCurso.getNombreCiclo() != null ? datosCurso.getNombreCiclo() : "")).setFontSize(9).setBorder(Border.NO_BORDER));

        headerTable.addCell(new Cell().add(new Paragraph("Docente:")).setBold().setFontSize(9).setBorder(Border.NO_BORDER));
        headerTable.addCell(new Cell().add(new Paragraph(datosCurso.getNombreMaestro() != null ? datosCurso.getNombreMaestro() : "Sin Asignar")).setFontSize(9).setBorder(Border.NO_BORDER));
        headerTable.addCell(new Cell().add(new Paragraph("Turno:")).setBold().setFontSize(9).setBorder(Border.NO_BORDER));
        headerTable.addCell(new Cell().add(new Paragraph(datosCurso.getNombreTurno() != null ? datosCurso.getNombreTurno() : "")).setFontSize(9).setBorder(Border.NO_BORDER));

        document.add(headerTable);

        // --- GRILLA DE ALUMNOS ---
        // Repartimos el ancho: 2f para Legajo y 8f para el nombre completo
        Table table = new Table(new float[]{2f, 8f}).useAllAvailableWidth();

        // Encabezados de columnas
        table.addHeaderCell(new Cell().add(new Paragraph("Legajo")).setBold().setFontSize(9));
        table.addHeaderCell(new Cell().add(new Paragraph("Apellido, Nombre")).setBold().setFontSize(9));

        if (datosCurso.getAlumnos() == null || datosCurso.getAlumnos().isEmpty()) {
            table.addCell(new Cell(1, 2)
                    .add(new Paragraph("No se encontraron alumnos inscriptos en este curso."))
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));
        } else {
            for (ar.com.sauce.colegio.rest.dto.AlumnoDto alumno : datosCurso.getAlumnos()) {
                table.addCell(new Cell().add(new Paragraph(alumno.getAlumnoId().toString())).setFontSize(8));
                table.addCell(new Cell().add(new Paragraph(alumno.getNombreCompleto())).setFontSize(8));
            }
        }
        document.add(table);

        // --- PIE DE REPORTE ---
        int totalAlumnos = datosCurso.getAlumnos() != null ? datosCurso.getAlumnos().size() : 0;
        document.add(new Paragraph("\nTotal de Alumnos: " + totalAlumnos)
                .setBold()
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(10)
                .setBorderTop(new SolidBorder(1)));

        document.close();
        return out.toByteArray();
    }
}