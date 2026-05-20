package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.ConceptoDto;
import ar.com.sauce.colegio.rest.dto.LineaDetalleDto;
import ar.com.sauce.colegio.rest.model.Concepto;
import ar.com.sauce.colegio.rest.repository.IConceptoRepository;
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
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConceptoService {

    @Autowired
    private IConceptoRepository conceptoRepository;

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
}