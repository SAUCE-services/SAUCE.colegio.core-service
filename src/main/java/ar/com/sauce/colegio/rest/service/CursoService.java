package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.*;
import ar.com.sauce.colegio.rest.model.Curso;
import ar.com.sauce.colegio.rest.repository.*;
import ar.com.sauce.colegio.rest.repository.projection.DeudaCursoProjection;
import jakarta.transaction.Transactional;
import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CursoService {

    private final ICursoRepository repository;
    private final IAlumnoRepository alumnoRepository;
    private final AlumnoService alumnoService; // 👈 Agregado para resolver findCursoConAlumnos

    @Autowired
    private ITurnoRepository turnoRepository;
    @Autowired
    private IMaestroRepository maestroRepository; // Verifica si tu interfaz es IMaestroRepository o similar
    @Autowired
    private IEstablecimientoRepository establecimientoRepository;
    @Autowired
    private ICicloRepository cicloRepository;

    // ✅ UNIFICADO: Todo se inyecta de forma limpia y segura por constructor
    @Autowired
    public CursoService(ICursoRepository repository,
                        IAlumnoRepository alumnoRepository,
                        AlumnoService alumnoService) {
        this.repository = repository;
        this.alumnoRepository = alumnoRepository;
        this.alumnoService = alumnoService;
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
            dto.setNombreCiclo(curso.getCiclo().getNombre());
        }

        return dto;
    }

    public Page<CursoDto> findByAnioCiclo(String anio, Pageable pageable) {
        return repository.findAllByCiclo_NombreContaining(anio, pageable)
                .map(this::convertToDto);
    }

    public List<String> listarNombresDeCiclos() {
        return repository.findAll().stream()
                .map(curso -> curso.getCiclo() != null ? curso.getCiclo().getNombre() : null)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    public DeudaCursoResponseDto obtenerDeudaPorCurso(String cursoNombre) {
        String nombreBusqueda = cursoNombre.trim();

        // Buscamos la metadata de cabecera usando tu AlumnoService existente
        CursoDetalleResponseDto infoCurso = alumnoService.findCursoConAlumnos(nombreBusqueda);

        // Ejecutamos la consulta relacional real usando el String del curso
        List<DeudaCursoProjection> proyecciones = alumnoRepository.findDeudaAlumnosByCursoNombre(nombreBusqueda);

        List<DeudaCursoDetalleDto> detalles = proyecciones.stream()
                .map(p -> new DeudaCursoDetalleDto(
                        p.getLegajo(),
                        p.getDni(),
                        p.getAlumno(),
                        p.getFactura(),
                        p.getPeriodo(),
                        p.getVencimiento(),
                        p.getImporte()
                )).collect(Collectors.toList());

        BigDecimal totalCurso = detalles.stream()
                .map(DeudaCursoDetalleDto::getImporte)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DeudaCursoResponseDto response = new DeudaCursoResponseDto();
        response.setCursoNombre(cursoNombre);

        if (infoCurso != null && infoCurso.getNombreEstablecimiento() != null) {
            response.setNombreMaestro(infoCurso.getNombreMaestro());
            response.setNombreTurno(infoCurso.getNombreTurno());
            response.setNombreCiclo(infoCurso.getNombreCiclo());
            response.setNombreEstablecimiento(infoCurso.getNombreEstablecimiento());
        } else {
            // Fallback seguro si la inicialización da limpia
            response.setNombreMaestro("Sin Asignar");
            response.setNombreTurno("N/C");
            response.setNombreCiclo("");
            response.setNombreEstablecimiento("Escuela Pascasio Moreno");
        }

        response.setDetalles(detalles);
        response.setTotalDeudaCurso(totalCurso);

        return response;
    }

    // 2. Generar el PDF usando el String del Curso
    public byte[] generarPdfDeudaPorCurso(String cursoNombre) {
        DeudaCursoResponseDto datos = obtenerDeudaPorCurso(cursoNombre);

        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");
        DateTimeFormatter dtfTablas = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 20, 20, 20);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // FUENTES
            Font font8 = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font font7_5 = FontFactory.getFont(FontFactory.HELVETICA, 7.5f);
            Font font8B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font font9B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font font9 = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font font11B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font font11 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font font13B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);

            // --- ENCABEZADO ---
            Paragraph pGen = new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion), font8);
            pGen.setAlignment(Element.ALIGN_RIGHT);
            document.add(pGen);

            Paragraph pEst = new Paragraph(datos.getNombreEstablecimiento(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
            pEst.setAlignment(Element.ALIGN_CENTER);
            document.add(pEst);

            Paragraph pTit = new Paragraph("Deuda por Curso", font13B);
            pTit.setAlignment(Element.ALIGN_CENTER);
            document.add(pTit);
            document.add(Chunk.NEWLINE);

            // --- CABECERA INFORMATIVA ---
            PdfPTable headerTable = new PdfPTable(new float[]{1.5f, 4.5f, 1f, 3f});
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingAfter(15f);

            addInfoCell(headerTable, "Curso:", font9B);
            addInfoCell(headerTable, datos.getCursoNombre().toUpperCase(), font9);
            addInfoCell(headerTable, "Ciclo:", font9B);
            addInfoCell(headerTable, datos.getNombreCiclo(), font9);

            addInfoCell(headerTable, "Docente:", font9B);
            addInfoCell(headerTable, datos.getNombreMaestro(), font9);
            addInfoCell(headerTable, "Turno:", font9B);
            addInfoCell(headerTable, datos.getNombreTurno(), font9);
            document.add(headerTable);

            // --- TABLA DE COMPROBANTES ---
            PdfPTable table = new PdfPTable(new float[]{1.2f, 1.5f, 3.5f, 1.3f, 2f, 1.5f, 1.8f});
            table.setWidthPercentage(100);

            String[] headers = {"Legajo", "DNI", "Alumno", "Factura", "Período", "Venc.", "Importe"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, font8B));
                table.addCell(cell);
            }

            if (datos.getDetalles().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No se registran comprobantes con deudas pendientes en este curso.", font8));
                emptyCell.setColspan(7);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(emptyCell);
            } else {
                for (DeudaCursoDetalleDto item : datos.getDetalles()) {
                    table.addCell(new PdfPCell(new Phrase(item.getLegajo().toString(), font7_5)));
                    table.addCell(new PdfPCell(new Phrase(item.getDni() != null ? item.getDni() : "", font7_5)));
                    table.addCell(new PdfPCell(new Phrase(item.getAlumno(), font7_5)));
                    table.addCell(new PdfPCell(new Phrase(item.getFactura().toString(), font7_5)));
                    table.addCell(new PdfPCell(new Phrase(item.getPeriodo(), font7_5)));
                    table.addCell(new PdfPCell(new Phrase(item.getVencimiento() != null ? item.getVencimiento().format(dtfTablas) : "", font7_5)));

                    PdfPCell cellImp = new PdfPCell(new Phrase(formatoMoneda.format(item.getImporte()), font7_5));
                    cellImp.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cellImp);
                }
            }
            document.add(table);

            // --- TOTAL GENERAL ---
            document.add(new Chunk(new org.openpdf.text.pdf.draw.LineSeparator(0.5f, 100, null, Element.ALIGN_CENTER, -2)));
            Paragraph pTotal = new Paragraph("TOTAL DEUDA CURSO: " + formatoMoneda.format(datos.getTotalDeudaCurso()), font11B);
            pTotal.setAlignment(Element.ALIGN_RIGHT);
            pTotal.setSpacingBefore(10f);
            document.add(pTotal);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de deuda por curso", e);
        }
        return out.toByteArray();
    }

    // Método auxiliar reutilizado
    private void addInfoCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(PdfPCell.NO_BORDER);
        table.addCell(cell);
    }

    @Transactional
    public CursoDto guardarOModificarCurso(CursoCargaDto dto) {
        Curso curso;

        // Si viene un ID válido, es una modificación. Si no, es una creación.
        if (dto.getCursoId() != null && dto.getCursoId() > 0) {
            curso = repository.findById(dto.getCursoId())
                    .orElseThrow(() -> new RuntimeException("Curso no encontrado con ID: " + dto.getCursoId()));
        } else {
            curso = new Curso();
        }

        // Asignamos la descripción escrita por el usuario
        curso.setDescripcion(dto.getDescripcion().trim());

        // Resolvemos y asignamos las opciones seleccionadas en la pantalla (image_c1491e.jpg)
        if (dto.getTurnoId() != null) {
            turnoRepository.findById(dto.getTurnoId()).ifPresent(curso::setTurno);
        }
        if (dto.getMaestroId() != null) {
            maestroRepository.findById(dto.getMaestroId()).ifPresent(curso::setMaestro);
        }
        if (dto.getEstablecimientoId() != null) {
            establecimientoRepository.findById(dto.getEstablecimientoId()).ifPresent(curso::setEstablecimiento);
        }
        if (dto.getCicloId() != null) {
            cicloRepository.findById(dto.getCicloId()).ifPresent(curso::setCiclo);
        }

        Curso guardado = repository.save(curso);
        return convertToDto(guardado); // Retorna con el formato de tu grilla preexistente
    }
}