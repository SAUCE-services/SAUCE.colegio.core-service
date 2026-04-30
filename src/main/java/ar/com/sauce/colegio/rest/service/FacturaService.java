package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.*;
import ar.com.sauce.colegio.rest.model.*;
import ar.com.sauce.colegio.rest.repository.*;
import ar.com.sauce.colegio.rest.repository.projection.ConceptoDetalleProjection;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.ZonedDateTime;
import java.time.ZoneId;

@Service
public class FacturaService {
    @Autowired
    private IFacturaRepository facturaRepository;
    @Autowired
    private IAlumnoRepository alumnoRepository;
    @Autowired
    private IConceptoRepository conceptoRepository;

    public HistoriaFacturacionDto obtenerHistoriaPorAlumno(Long alumnoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

        HistoriaFacturacionDto dto = new HistoriaFacturacionDto();
        dto.setLegajo(alumno.getAlumnoId());
        dto.setNombreCompleto(alumno.getApellido() + ", " + alumno.getNombre());

        // Grilla Superior: Lista de Facturas vinculadas al alumno
        List<Factura> facturas = facturaRepository.findByAlumnoId(alumnoId);

        dto.setFacturas(facturas.stream().map(f -> new FacturaDetalleDto(
                f.getNroFactura(),
                f.getTipoEstado() != null ? f.getTipoEstado().getDescripcion() : "",
                f.getFechaEstado(),
                f.getPrimerVencimiento(),
                f.getFechaPago(),
                f.getImporteAdeudado(),
                f.getImportePagado(),
                f.getFechaCancelacion(),
                f.getPeriodo() != null ? f.getPeriodo().getDescripcion() : ""
        )).collect(Collectors.toList()));

        return dto;
    }

    public List<LineaDetalleDto> obtenerDetalleDeFactura(Long facturaId) {
        Factura f = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

        List<LineaDetalleDto> detalles = new java.util.ArrayList<>();

        // Traemos todos los registros de alumnos_conceptos para esta factura
        List<ConceptoDetalleProjection> conceptos = conceptoRepository.findByFacturaId(facturaId);

        for (ConceptoDetalleProjection cp : conceptos) {
            detalles.add(new LineaDetalleDto(
                    f.getFechaEstado(),
                    cp.getDescripcion(), // Ahora traerá "Sin Asignar" o el nombre real
                    "Concepto FACTURADO",
                    cp.getImporte(),     // Traerá los 0.00 o los montos reales
                    obtenerFechaCreacion(f).toLocalDate(),
                    f.getPeriodo() != null ? f.getPeriodo().getDescripcion() : ""
            ));
        }

        // Se mantiene la lógica de intereses por si existe una factura vinculada
        if (f.getFacturaInteres() != null) {
            Factura i = f.getFacturaInteres();
            detalles.add(new LineaDetalleDto(
                    i.getFechaEstado(),
                    "Intereses por Mora",
                    "Concepto FACTURADO",
                    i.getImporteAdeudado(),
                    obtenerFechaCreacion(i).toLocalDate(),
                    i.getPeriodo() != null ? i.getPeriodo().getDescripcion() : ""
            ));
        }

        return detalles;
    }
    private java.time.LocalDateTime obtenerFechaCreacion(Factura factura) {
        try {
            java.lang.reflect.Field field = ar.com.sauce.colegio.rest.model.Auditable.class.getDeclaredField("created");
            field.setAccessible(true);
            return (java.time.LocalDateTime) field.get(factura);
        } catch (Exception e) {
            return java.time.LocalDateTime.now();
        }
    }

    public ReporteRecaudacionDto obtenerRecaudacionEstructurada(LocalDate fecha) {
        // 1. Obtener datos planos del repositorio
        List<Map<String, Object>> datos = facturaRepository.findRecaudacionByFecha(fecha);

        // ✅ IMPORTANTE: Usamos LinkedHashMap para mantener el orden del SQL
        Map<String, Map<String, List<Map<String, Object>>>> agrupado = datos.stream()
                .collect(Collectors.groupingBy(
                        m -> m.get("establecimiento") != null ? m.get("establecimiento").toString() : "SIN ESTABLECIMIENTO",
                        LinkedHashMap::new, // 👈 Esto mantiene el orden
                        Collectors.groupingBy(
                                m -> m.get("medioPago") != null ? m.get("medioPago").toString() : "Manual",
                                LinkedHashMap::new, // 👈 También para los medios de pago
                                Collectors.toList()
                        )
                ));

        // 3. Construir el objeto de reporte
        ReporteRecaudacionDto reporte = new ReporteRecaudacionDto();
        reporte.setFechaReporte(fecha);
        BigDecimal granTotal = BigDecimal.ZERO;

        for (var entryEst : agrupado.entrySet()) {
            RecaudacionEstablecimientoDto estDto = new RecaudacionEstablecimientoDto();
            estDto.setNombre(entryEst.getKey());
            BigDecimal totalEst = BigDecimal.ZERO;

            for (var entryMedio : entryEst.getValue().entrySet()) {
                RecaudacionMedioDto medioDto = new RecaudacionMedioDto();
                medioDto.setNombre(entryMedio.getKey());

                // Convertimos cada Map plano a nuestro DTO de detalle
                List<RecaudacionDetalleDto> detalles = entryMedio.getValue().stream()
                        .map(m -> {
                            // Conversión segura de los tipos que vienen del Map nativo
                            Number facturaNro = (Number) m.get("factura");
                            Number legajoNro = (Number) m.get("legajo");
                            Object pagadoObj = m.get("pagado");
                            BigDecimal pagado = (pagadoObj instanceof BigDecimal) ?
                                    (BigDecimal) pagadoObj :
                                    new BigDecimal(pagadoObj.toString());

                            return new RecaudacionDetalleDto(
                                    facturaNro.longValue(),
                                    (String) m.get("periodo"),
                                    legajoNro.longValue(),
                                    (String) m.get("nombre"),
                                    pagado
                            );
                        }).collect(Collectors.toList());

                BigDecimal subtotalMedio = detalles.stream()
                        .map(d -> d.getPagado())
                        .filter(p -> p != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP); // 👈 Forzar 2 decimales para contabilidad

                medioDto.setItems(detalles);
                medioDto.setCantidadPagos(detalles.size()); // "Cantidad de Pagos: X" en la imagen
                medioDto.setSubtotal(subtotalMedio);

                estDto.getMedios().add(medioDto);
                totalEst = totalEst.add(subtotalMedio);
            }

            estDto.setTotalEstablecimiento(totalEst);
            reporte.getEstablecimientos().add(estDto);
            granTotal = granTotal.add(totalEst);
        }

        reporte.setGranTotal(granTotal);
        return reporte;
    }

    public byte[] generarPdfRecaudacion(LocalDate fecha) {
        // 1. Obtenemos los datos procesados del DTO
        ReporteRecaudacionDto datos = obtenerRecaudacionEstructurada(fecha);

        // Configuramos el formato moneda para Argentina
        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        // Formato para la fecha de creación del sistema
        // ✅ CORRECCIÓN: Obtenemos la hora específica de Argentina
        ZoneId zonaArgentina = ZoneId.of("America/Argentina/Buenos_Aires");
        String fechaGeneracion = ZonedDateTime.now(zonaArgentina)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // --- ENCABEZADO ---

        // Fecha de generación (arriba a la derecha, más pequeña)
        document.add(new Paragraph("Generado el: " + fechaGeneracion)
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(8)
                .setMarginBottom(0));

        document.add(new Paragraph("Unión Vecinal de Servicios Públicos El Sauce - Colegio")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setMarginTop(0));

        document.add(new Paragraph("Recaudación Diaria")
                .setTextAlignment(TextAlignment.CENTER));

        // Fecha de los pagos (la que se filtró)
        document.add(new Paragraph("Fecha Pago: " + fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setTextAlignment(TextAlignment.RIGHT));

        // --- CONTENIDO ---

        for (RecaudacionEstablecimientoDto est : datos.getEstablecimientos()) {
            document.add(new Paragraph("\n" + est.getNombre()).setBold().setUnderline());

            for (RecaudacionMedioDto medio : est.getMedios()) {
                document.add(new Paragraph(medio.getNombre()).setItalic());

                Table table = new Table(5).useAllAvailableWidth();
                table.addHeaderCell("Factura");
                table.addHeaderCell("Período");
                table.addHeaderCell("Legajo");
                table.addHeaderCell("Apellido, Nombre");
                table.addHeaderCell("Pagado");

                for (RecaudacionDetalleDto item : medio.getItems()) {
                    table.addCell(item.getFactura().toString());
                    table.addCell(item.getPeriodo());
                    table.addCell(item.getLegajo().toString());
                    table.addCell(item.getNombre());
                    table.addCell(formatoMoneda.format(item.getPagado()));
                }
                document.add(table);

                String subtotalStr = formatoMoneda.format(medio.getSubtotal());
                document.add(new Paragraph("Cantidad de Pagos: " + medio.getCantidadPagos() +
                        " - Subtotal: " + subtotalStr)
                        .setTextAlignment(TextAlignment.RIGHT).setFontSize(9));
            }
        }

        // --- TOTALES ---

        String totalGralStr = formatoMoneda.format(datos.getGranTotal());
        document.add(new Paragraph("\nTOTAL GENERAL: " + totalGralStr)
                .setBold().setTextAlignment(TextAlignment.RIGHT).setFontSize(12));

        document.close();
        return out.toByteArray();
    }
}