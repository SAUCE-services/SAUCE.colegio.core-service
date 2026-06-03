package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.*;
import ar.com.sauce.colegio.rest.model.Concepto;
import ar.com.sauce.colegio.rest.model.Periodo;
import ar.com.sauce.colegio.rest.repository.IAlumnoRepository;
import ar.com.sauce.colegio.rest.repository.IConceptoRepository;
import ar.com.sauce.colegio.rest.repository.IPeriodoRepository;
import ar.com.sauce.colegio.rest.repository.projection.DeudaIndividualProjection;
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
        List<Concepto> conceptos = conceptoRepository.findAll();

        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 20, 20, 20);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // FUENTES
            Font font8B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font font8_5 = FontFactory.getFont(FontFactory.HELVETICA, 8.5f);
            Font font9B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font font11B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font font13B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);

            // ENCABEZADO
            Paragraph pGen = new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion), FontFactory.getFont(FontFactory.HELVETICA, 8));
            pGen.setAlignment(Element.ALIGN_RIGHT);
            document.add(pGen);

            Paragraph pTit = new Paragraph("Unión Vecinal de Servicios Públicos El Sauce - Colegio", font11B);
            pTit.setAlignment(Element.ALIGN_CENTER);
            document.add(pTit);

            Paragraph pSub = new Paragraph("Listado General de Conceptos", font13B);
            pSub.setAlignment(Element.ALIGN_CENTER);
            pSub.setSpacingAfter(15f);
            document.add(pSub);

            // TABLA DE CONCEPTOS
            PdfPTable table = new PdfPTable(new float[]{1.5f, 6.0f, 2.5f});
            table.setWidthPercentage(100);

            // Encabezados
            String[] headers = {"Código", "Concepto / Descripción", "Importe Base"};
            for (int i = 0; i < headers.length; i++) {
                PdfPCell cell = new PdfPCell(new Phrase(headers[i], font9B));
                if (i == 2) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cell);
            }

            // Filas
            if (conceptos.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No se registran conceptos cargados en el sistema.", font8_5));
                emptyCell.setColspan(3);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(emptyCell);
            } else {
                for (Concepto con : conceptos) {
                    table.addCell(new PdfPCell(new Phrase(con.getConceptoId().toString(), font8_5)));
                    table.addCell(new PdfPCell(new Phrase(con.getDescripcion(), font8_5)));

                    PdfPCell cellImp = new PdfPCell(new Phrase(con.getImporte() != null ? formatoMoneda.format(con.getImporte()) : "$ 0,00", font8_5));
                    cellImp.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cellImp);
                }
            }
            document.add(table);

            // PIE DE REPORTE
            document.add(new Chunk(new org.openpdf.text.pdf.draw.LineSeparator(0.5f, 100, null, Element.ALIGN_CENTER, -2)));
            Paragraph pTotal = new Paragraph("\nCantidad Total de Conceptos: " + conceptos.size(), font9B);
            pTotal.setAlignment(Element.ALIGN_RIGHT);
            document.add(pTotal);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de conceptos", e);
        }
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

    public List<NovedadCursoDto> obtenerNovedadesPorCurso(Long cursoId, String periodoNombre, String cicloNombre) {
        // 🌟 Pasamos los 3 parámetros al repositorio y mapeamos el nuevo campo cursoNombre
        return conceptoRepository.findNovedadesPorCursoYPeriodo(cursoId, periodoNombre, cicloNombre).stream()
                .map(p -> new NovedadCursoDto(
                        p.getLegajo(),
                        p.getAlumno(),
                        p.getCursoNombre(), // 🌟 Agregamos el nombre del curso extraído de la proyección
                        p.getConcepto(),
                        p.getImporte(),
                        p.getEstado(),
                        p.getFechaRegistro()
                ))
                .collect(Collectors.toList());
        }
}