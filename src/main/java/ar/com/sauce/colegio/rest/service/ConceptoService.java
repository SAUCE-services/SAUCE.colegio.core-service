package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.ConceptoDto;
import ar.com.sauce.colegio.rest.dto.LineaDetalleDto;
import ar.com.sauce.colegio.rest.dto.NovedadCargaDto;
import ar.com.sauce.colegio.rest.dto.NovedadesAlumnoResponseDto;
import ar.com.sauce.colegio.rest.model.Concepto;
import ar.com.sauce.colegio.rest.model.Periodo;
import ar.com.sauce.colegio.rest.repository.IAlumnoRepository;
import ar.com.sauce.colegio.rest.repository.IConceptoRepository;
import ar.com.sauce.colegio.rest.repository.IPeriodoRepository;
import ar.com.sauce.colegio.rest.repository.projection.DeudaIndividualProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConceptoService {

    @Autowired
    private IConceptoRepository conceptoRepository;
    @Autowired
    private IAlumnoRepository alumnoRepository;
    @Autowired
    private IPeriodoRepository periodoRepository;

    public Page<ConceptoDto> findAllPaged(Pageable pageable) {
        return conceptoRepository.findAll(pageable).map(this::convertToDto);
    }

    public Concepto save(Concepto concepto) {
        return conceptoRepository.save(concepto);
    }

    private ConceptoDto convertToDto(Concepto concepto) {
        return new ConceptoDto(
                concepto.getConceptoId(),
                concepto.getDescripcion(),
                concepto.getImporte()
        );
    }

    public List<LineaDetalleDto> obtenerDeudaIndividual(Long alumnoId) {
        List<DeudaIndividualProjection> historial = conceptoRepository.findDeudaIndividualByAlumnoId(alumnoId);

        List<LineaDetalleDto> detalles = new ArrayList<>();

        for (DeudaIndividualProjection hp : historial) {
            detalles.add(new LineaDetalleDto(
                    hp.getFechaEstado(),   // 1. fechaEstado
                    hp.getConcepto(),      // 2. concepto
                    hp.getEstado(),        // 3. estado
                    hp.getImporte(),       // 4. importe
                    hp.getFechaRegistro(), // 5. fechaRegistro
                    hp.getPeriodo()        // 6. periodo
            ));
        }

        return detalles;
    }

    /**
    * Modifica un concepto existente buscando por su ID
     */
    public Concepto editarConcepto(Long id, ConceptoDto dto) {
        Concepto concepto = conceptoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Concepto no encontrado con el ID: " + id));

        concepto.setDescripcion(dto.getDescripcion());
        concepto.setImporte(dto.getImporte());

        return conceptoRepository.save(concepto);
    }

    /**
     * Genera un listado en PDF con absolutamente todos los conceptos vigentes
     */
    public byte[] generarPdfTodosLosConceptos() {
        // Recuperamos la lista completa de conceptos ordenados por ID
        List<Concepto> conceptos = conceptoRepository.findAll();

        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);

        // Margen izquierdo ajustado a 40 para ganchos o carpetas
        document.setMargins(20, 20, 20, 40);

        // --- ENCABEZADO ---
        document.add(new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion))
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(8)
                .setMarginBottom(0));

        document.add(new Paragraph("Unión Vecinal de Servicios Públicos El Sauce - Colegio")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold().setMarginTop(0).setFontSize(11));

        document.add(new Paragraph("Listado General de Conceptos")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold().setFontSize(13).setMarginBottom(15));

        // --- TABLA DE CONCEPTOS (3 Columnas: ID, Descripción, Importe) ---
        float[] columnWidths = {1.5f, 6.0f, 2.5f};
        Table table = new Table(columnWidths).useAllAvailableWidth();

        // Encabezados de la tabla
        table.addHeaderCell(new Cell().add(new Paragraph("Código")).setBold().setFontSize(9));
        table.addHeaderCell(new Cell().add(new Paragraph("Concepto / Descripción")).setBold().setFontSize(9));
        table.addHeaderCell(new Cell().add(new Paragraph("Importe Base")).setBold().setFontSize(9).setTextAlignment(TextAlignment.RIGHT));

        if (conceptos.isEmpty()) {
            table.addCell(new Cell(1, 3)
                    .add(new Paragraph("No se registran conceptos cargados en el sistema."))
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));
        } else {
            for (Concepto con : conceptos) {
                String importeStr = con.getImporte() != null ? formatoMoneda.format(con.getImporte()) : "$ 0,00";

                table.addCell(new Cell().add(new Paragraph(con.getConceptoId().toString())).setFontSize(8.5f));
                table.addCell(new Cell().add(new Paragraph(con.getDescripcion())).setFontSize(8.5f));
                table.addCell(new Cell().add(new Paragraph(importeStr)).setFontSize(8.5f).setTextAlignment(TextAlignment.RIGHT));
            }
        }
        document.add(table);

        // --- PIE DE REPORTE ---
        document.add(new Paragraph("\nCantidad Total de Conceptos: " + conceptos.size())
                .setBold()
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(9)
                .setBorderTop(new SolidBorder(1)));

        document.close();
        return out.toByteArray();
    }

    /**
     * Consulta las novedades por Alumno filtrando mediante el NOMBRE (String) del Período
     */
    public NovedadesAlumnoResponseDto obtenerNovedadesPorAlumnoYPeriodoNombre(Long alumnoId, String periodoNombre) {
        ar.com.sauce.colegio.rest.model.Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado con legajo: " + alumnoId));

        // Ejecutamos la consulta filtrada por el texto exacto del período
        List<Object[]> resultados = conceptoRepository.findNovedadesByAlumnoYPeriodoNombre(alumnoId, periodoNombre.trim());

        List<LineaDetalleDto> grilla = resultados.stream().map(row -> {
            LineaDetalleDto linea = new LineaDetalleDto();

            // 🛡️ PARSEO SEGURO DE FECHA ESTADO (row[0])
            if (row[0] != null) {
                if (row[0] instanceof LocalDate) {
                    linea.setFechaEstado((LocalDate) row[0]);
                } else if (row[0] instanceof Date) {
                    linea.setFechaEstado(((Date) row[0]).toLocalDate());
                } else if (row[0] instanceof Timestamp) {
                    linea.setFechaEstado(((Timestamp) row[0]).toLocalDateTime().toLocalDate());
                }
            }

            linea.setConcepto((String) row[1]);
            linea.setEstado((String) row[2]);
            linea.setImporte(row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO);

            // 🛡️ PARSEO SEGURO DE FECHA REGISTRO (row[4])
            if (row[4] != null) {
                if (row[4] instanceof LocalDate) {
                    linea.setFechaRegistro((LocalDate) row[4]);
                } else if (row[4] instanceof Date) {
                    linea.setFechaRegistro(((Date) row[4]).toLocalDate());
                } else if (row[4] instanceof Timestamp) {
                    linea.setFechaRegistro(((Timestamp) row[4]).toLocalDateTime().toLocalDate());
                }
            }

            linea.setPeriodo((String) row[5]);
            return linea;
        }).collect(Collectors.toList());

        NovedadesAlumnoResponseDto response = new NovedadesAlumnoResponseDto();
        response.setLegajo(alumno.getAlumnoId());
        response.setNombreCompleto(alumno.getApellido() + ", " + alumno.getNombre());
        response.setDetallesGrilla(grilla);

        return response;
    }

    /**
     * Inserta la novedad resolviendo internamente el ID del período en base a su String descriptivo
     */
    public List<LineaDetalleDto> agregarNovedadManualConPeriodoNombre(NovedadCargaDto dto) {
        // Buscamos dinámicamente el período en la base de datos por su descripción
        Periodo per = periodoRepository.findAll().stream()
                .filter(p -> p.getDescripcion().equalsIgnoreCase(dto.getPeriodoNombre().trim()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("El período especificado no existe: " + dto.getPeriodoNombre()));

        // Insertamos usando el ID real recuperado
        conceptoRepository.registrarNovedadManual(
                dto.getAlumnoId(),
                per.getPeriodoId(), // 👈 ID resuelto de forma segura en el Back
                dto.getConceptoId(),
                dto.getImporte()
        );

        // Retornamos los datos frescos filtrados por ese mismo período para que se actualice la grilla
        return obtenerNovedadesPorAlumnoYPeriodoNombre(dto.getAlumnoId(), dto.getPeriodoNombre()).getDetallesGrilla();
    }
}