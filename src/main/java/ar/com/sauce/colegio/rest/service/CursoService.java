package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.CursoDetalleResponseDto;
import ar.com.sauce.colegio.rest.dto.CursoDto;
import ar.com.sauce.colegio.rest.dto.DeudaCursoDetalleDto;
import ar.com.sauce.colegio.rest.dto.DeudaCursoResponseDto;
import ar.com.sauce.colegio.rest.model.Curso;
import ar.com.sauce.colegio.rest.repository.IAlumnoRepository;
import ar.com.sauce.colegio.rest.repository.ICursoRepository;
import ar.com.sauce.colegio.rest.repository.projection.DeudaCursoProjection;
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
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);

        document.setMargins(20, 20, 20, 40);

        // --- ENCABEZADO ---
        document.add(new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion))
                .setTextAlignment(TextAlignment.RIGHT).setFontSize(8));

        document.add(new Paragraph(datos.getNombreEstablecimiento())
                .setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(11));

        document.add(new Paragraph("Deuda por Curso")
                .setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(13));

        // --- CABECERA INFORMATIVA ---
        Table headerTable = new Table(new float[]{1.5f, 4.5f, 1f, 3f}).useAllAvailableWidth();
        headerTable.setMarginBottom(15).setMarginTop(10);

        headerTable.addCell(new Cell().add(new Paragraph("Curso:")).setBold().setFontSize(9).setBorder(Border.NO_BORDER));
        headerTable.addCell(new Cell().add(new Paragraph(datos.getCursoNombre().toUpperCase())).setFontSize(9).setBorder(Border.NO_BORDER));
        headerTable.addCell(new Cell().add(new Paragraph("Ciclo:")).setBold().setFontSize(9).setBorder(Border.NO_BORDER));
        headerTable.addCell(new Cell().add(new Paragraph(datos.getNombreCiclo())).setFontSize(9).setBorder(Border.NO_BORDER));

        headerTable.addCell(new Cell().add(new Paragraph("Docente:")).setBold().setFontSize(9).setBorder(Border.NO_BORDER));
        headerTable.addCell(new Cell().add(new Paragraph(datos.getNombreMaestro())).setFontSize(9).setBorder(Border.NO_BORDER));
        headerTable.addCell(new Cell().add(new Paragraph("Turno:")).setBold().setFontSize(9).setBorder(Border.NO_BORDER));
        headerTable.addCell(new Cell().add(new Paragraph(datos.getNombreTurno())).setFontSize(9).setBorder(Border.NO_BORDER));

        document.add(headerTable);

        // --- TABLA DE COMPROBANTES ---
        float[] columnWidths = {1.2f, 1.5f, 3.5f, 1.3f, 2f, 1.5f, 1.8f};
        Table table = new Table(columnWidths).useAllAvailableWidth();

        table.addHeaderCell(new Cell().add(new Paragraph("Legajo")).setBold().setFontSize(8));
        table.addHeaderCell(new Cell().add(new Paragraph("DNI")).setBold().setFontSize(8));
        table.addHeaderCell(new Cell().add(new Paragraph("Alumno")).setBold().setFontSize(8));
        table.addHeaderCell(new Cell().add(new Paragraph("Factura")).setBold().setFontSize(8));
        table.addHeaderCell(new Cell().add(new Paragraph("Período")).setBold().setFontSize(8));
        table.addHeaderCell(new Cell().add(new Paragraph("Venc.")).setBold().setFontSize(8));
        table.addHeaderCell(new Cell().add(new Paragraph("Importe")).setBold().setFontSize(8).setTextAlignment(TextAlignment.RIGHT));

        if (datos.getDetalles().isEmpty()) {
            table.addCell(new Cell(1, 7)
                    .add(new Paragraph("No se registran comprobantes con deudas pendientes en este curso."))
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));
        } else {
            for (DeudaCursoDetalleDto item : datos.getDetalles()) {
                String fVenc = item.getVencimiento() != null ? item.getVencimiento().format(dtfTablas) : "";

                table.addCell(new Cell().add(new Paragraph(item.getLegajo().toString())).setFontSize(7.5f));
                table.addCell(new Cell().add(new Paragraph(item.getDni() != null ? item.getDni() : "")).setFontSize(7.5f));
                table.addCell(new Cell().add(new Paragraph(item.getAlumno())).setFontSize(7.5f));
                table.addCell(new Cell().add(new Paragraph(item.getFactura().toString())).setFontSize(7.5f));
                table.addCell(new Cell().add(new Paragraph(item.getPeriodo())).setFontSize(7.5f));
                table.addCell(new Cell().add(new Paragraph(fVenc)).setFontSize(7.5f));
                table.addCell(new Cell().add(new Paragraph(formatoMoneda.format(item.getImporte())))
                        .setFontSize(7.5f).setTextAlignment(TextAlignment.RIGHT));
            }
        }
        document.add(table);

        // --- TOTAL GENERAL ---
        document.add(new Paragraph("\nTOTAL DEUDA CURSO: " + formatoMoneda.format(datos.getTotalDeudaCurso()))
                .setBold().setTextAlignment(TextAlignment.RIGHT).setFontSize(11).setBorderTop(new SolidBorder(1)));

        document.close();
        return out.toByteArray();
    }
}