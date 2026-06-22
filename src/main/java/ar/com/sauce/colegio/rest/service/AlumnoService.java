package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.AlumnoCompletoDto;
import ar.com.sauce.colegio.rest.dto.AlumnoDto;
import ar.com.sauce.colegio.rest.dto.CursoDetalleResponseDto;
import ar.com.sauce.colegio.rest.model.*;
import ar.com.sauce.colegio.rest.repository.*;
import jakarta.transaction.Transactional;
import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
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
        CursoDetalleResponseDto datosCurso = findCursoConAlumnos(cursoNombre);
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 20, 20, 20);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // FUENTES
            Font font8 = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font font9 = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font font9B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font font11B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font font13B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);

            // ENCABEZADO
            Paragraph pGen = new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion), font8);
            pGen.setAlignment(Element.ALIGN_RIGHT);
            document.add(pGen);

            String estName = (datosCurso.getNombreEstablecimiento() != null) ? datosCurso.getNombreEstablecimiento() : "Unión Vecinal de Servicios Públicos El Sauce - Colegio";
            Paragraph pEst = new Paragraph(estName, font9B);
            pEst.setAlignment(Element.ALIGN_CENTER);
            document.add(pEst);

            Paragraph pTit = new Paragraph("Alumnos por Curso", font13B);
            pTit.setAlignment(Element.ALIGN_CENTER);
            document.add(pTit);
            document.add(Chunk.NEWLINE);

            // TABLA DE CABECERA INFORMATIVA
            PdfPTable headerTable = new PdfPTable(new float[]{1.5f, 4.5f, 1f, 3f});
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingAfter(15f);

            addInfoCell(headerTable, "Curso:", font9B);
            addInfoCell(headerTable, cursoNombre.toUpperCase(), font9);
            addInfoCell(headerTable, "Ciclo:", font9B);
            addInfoCell(headerTable, (datosCurso.getNombreCiclo() != null ? datosCurso.getNombreCiclo() : ""), font9);

            addInfoCell(headerTable, "Docente:", font9B);
            addInfoCell(headerTable, (datosCurso.getNombreMaestro() != null ? datosCurso.getNombreMaestro() : "Sin Asignar"), font9);
            addInfoCell(headerTable, "Turno:", font9B);
            addInfoCell(headerTable, (datosCurso.getNombreTurno() != null ? datosCurso.getNombreTurno() : ""), font9);

            document.add(headerTable);

            // GRILLA DE ALUMNOS
            PdfPTable table = new PdfPTable(new float[]{2f, 8f});
            table.setWidthPercentage(100);

            table.addCell(new PdfPCell(new Phrase("Legajo", font9B)));
            table.addCell(new PdfPCell(new Phrase("Apellido, Nombre", font9B)));

            if (datosCurso.getAlumnos() == null || datosCurso.getAlumnos().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No se encontraron alumnos inscriptos en este curso.", font8));
                emptyCell.setColspan(2);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(emptyCell);
            } else {
                for (ar.com.sauce.colegio.rest.dto.AlumnoDto alumno : datosCurso.getAlumnos()) {
                    table.addCell(new PdfPCell(new Phrase(alumno.getAlumnoId().toString(), font8)));
                    table.addCell(new PdfPCell(new Phrase(alumno.getNombreCompleto(), font8)));
                }
            }
            document.add(table);

            // PIE
            int totalAlumnos = datosCurso.getAlumnos() != null ? datosCurso.getAlumnos().size() : 0;
            document.add(new Chunk(new org.openpdf.text.pdf.draw.LineSeparator(0.5f, 100, null, Element.ALIGN_CENTER, -2)));
            Paragraph pTotal = new Paragraph("Total de Alumnos: " + totalAlumnos, font11B);
            pTotal.setAlignment(Element.ALIGN_RIGHT);
            document.add(pTotal);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de alumnos por curso", e);
        }
        return out.toByteArray();
    }

    // Método auxiliar para limpiar la creación de celdas en la cabecera
    private void addInfoCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(PdfPCell.NO_BORDER);
        table.addCell(cell);
    }

    @Transactional
    public void asignarAlumnoACurso(Long alumnoId, String nombreCurso) {
        // 1. Actualizar el texto plano que usan las recaudaciones
        alumnoRepository.actualizarCurso(alumnoId, nombreCurso.trim());

        // 2. Buscar el curso real para obtener su ID e impactar en el módulo de deudas
        Curso curso = cursoRepository.findByDescripcion(nombreCurso.trim())
                .orElseThrow(() -> new RuntimeException("No se encontró el curso: " + nombreCurso));

        alumnoRepository.asignarCursoRelacional(alumnoId, curso.getCursoId());
    }

    @Transactional
    public void quitarAlumnoDeCurso(Long alumnoId) {
        // 1. Limpiar el texto plano
        alumnoRepository.actualizarCurso(alumnoId, "");

        // 2. Romper la relación en la tabla intermedia de deudas/ciclos
        alumnoRepository.quitarCursoRelacional(alumnoId);
    }
}