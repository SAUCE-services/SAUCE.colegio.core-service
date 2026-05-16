package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.*;
import ar.com.sauce.colegio.rest.model.*;
import ar.com.sauce.colegio.rest.repository.*;
import ar.com.sauce.colegio.rest.repository.projection.ConceptoDetalleProjection;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    @Autowired
    private IPeriodoRepository periodoRepository;

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

    public List<LineaDetalleDto> obtenerDetalleDeFactura(Long nroFactura) {
        // 1. CORRECCIÓN CLAVE: Buscamos por nro_factura (720) y NO por el id autoincremental
        Factura f = facturaRepository.findByNroFactura(nroFactura)
                .orElseThrow(() -> new RuntimeException("Factura Nro " + nroFactura + " no encontrada"));

        List<LineaDetalleDto> detalles = new ArrayList<>();

        // Traemos todos los registros de alumnos_conceptos para esta factura
        List<ConceptoDetalleProjection> conceptos = conceptoRepository.findByNroFactura(f.getNroFactura());

        for (ConceptoDetalleProjection cp : conceptos) {
            detalles.add(new LineaDetalleDto(
                    f.getFechaEstado(),
                    cp.getDescripcion(), // Ahora traerá "Sin Asignar" o el nombre real
                    "Concepto FACTURADO",
                    cp.getImporte(),     // Traerá los montos reales de la base de datos
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
    private LocalDateTime obtenerFechaCreacion(Factura factura) {
        try {
            java.lang.reflect.Field field = ar.com.sauce.colegio.rest.model.Auditable.class.getDeclaredField("created");
            field.setAccessible(true);
            return (LocalDateTime) field.get(factura);
        } catch (Exception e) {
            return LocalDateTime.now();
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
                            // Extraer y convertir la fecha
                            Object fechaObj = m.get("fecha");
                            LocalDate fechaDto = null;
                            if (fechaObj instanceof java.sql.Date) {
                                fechaDto = ((java.sql.Date) fechaObj).toLocalDate();
                            } else if (fechaObj instanceof java.sql.Timestamp) {
                                fechaDto = ((java.sql.Timestamp) fechaObj).toLocalDateTime().toLocalDate();
                            }

                            return new RecaudacionDetalleDto(
                                    facturaNro.longValue(),
                                    (String) m.get("periodo"),
                                    legajoNro.longValue(),
                                    (String) m.get("nombre"),
                                    fechaDto,
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
        // ✅ Formato solo fecha
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);

        // MARGEN: Ajustado con 40 a la izquierda para el encarpetado
        document.setMargins(20, 20, 20, 40);

        // --- ENCABEZADO ---

        // CORRECCIÓN: Ahora imprime la fecha real de generación
        document.add(new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion))
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
            document.add(new Paragraph("\n" + est.getNombre()).setBold().setUnderline().setFontSize(11));

            for (RecaudacionMedioDto medio : est.getMedios()) {
                document.add(new Paragraph(medio.getNombre()).setItalic().setMarginLeft(10));

                Table table = new Table(5).useAllAvailableWidth();
                // Definimos celdas de encabezado con estilo consistente
                table.addHeaderCell(new Cell().add(new Paragraph("Factura")).setBold().setFontSize(9));
                table.addHeaderCell(new Cell().add(new Paragraph("Período")).setBold().setFontSize(9));
                table.addHeaderCell(new Cell().add(new Paragraph("Legajo")).setBold().setFontSize(9));
                table.addHeaderCell(new Cell().add(new Paragraph("Apellido, Nombre")).setBold().setFontSize(9));
                table.addHeaderCell(new Cell().add(new Paragraph("Pagado")).setBold().setFontSize(9));

                for (RecaudacionDetalleDto item : medio.getItems()) {
                    table.addCell(new Cell().add(new Paragraph(item.getFactura().toString())).setFontSize(8));
                    table.addCell(new Cell().add(new Paragraph(item.getPeriodo())).setFontSize(8));
                    table.addCell(new Cell().add(new Paragraph(item.getLegajo().toString())).setFontSize(8));
                    table.addCell(new Cell().add(new Paragraph(item.getNombre())).setFontSize(8));
                    table.addCell(new Cell().add(new Paragraph(formatoMoneda.format(item.getPagado())))
                            .setFontSize(8).setTextAlignment(TextAlignment.RIGHT));
                }
                document.add(table);

                String subtotalStr = formatoMoneda.format(medio.getSubtotal());
                document.add(new Paragraph("Cantidad de Pagos: " + medio.getCantidadPagos() +
                        " - Subtotal: " + subtotalStr)
                        .setTextAlignment(TextAlignment.RIGHT).setBold().setFontSize(9));
            }
        }

        // --- TOTALES ---

        String totalGralStr = formatoMoneda.format(datos.getGranTotal());
        document.add(new Paragraph("\nTOTAL GENERAL: " + totalGralStr)
                .setBold()
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(11)
                .setBorderTop(new SolidBorder(1)));

        document.close();
        return out.toByteArray();
    }

    public ReporteFacturaPeriodoDto obtenerFacturasPeriodoEstructurada(String descripcion) {
        // 1. Obtenemos los datos planos desde el repositorio usando la descripción
        List<Map<String, Object>> datos = facturaRepository.findFacturasByPeriodoDesc(descripcion);

        // 2. Agrupamos por establecimiento manteniendo el orden (LinkedHashMap)
        Map<String, List<Map<String, Object>>> agrupado = datos.stream()
                .collect(Collectors.groupingBy(
                        m -> m.get("establecimiento") != null ? m.get("establecimiento").toString() : "SIN ESTABLECIMIENTO",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        ReporteFacturaPeriodoDto reporte = new ReporteFacturaPeriodoDto();
        reporte.setDescripcionPeriodo(descripcion);

        // Fecha de generación con zona horaria de Argentina
        reporte.setFechaGeneracion(LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")));
        BigDecimal granTotal = BigDecimal.ZERO;
        int cantidadTotal = 0;

        for (var entryEst : agrupado.entrySet()) {
            FacturaPeriodoEstablecimientoDto estDto = new FacturaPeriodoEstablecimientoDto();
            estDto.setNombre(entryEst.getKey());

            List<RecaudacionDetalleDto> detalles = entryEst.getValue().stream()
                    .map(m -> {
                        // Conversión segura de tipos numéricos
                        Number facturaNro = (Number) m.get("factura");
                        Number legajoNro = (Number) m.get("legajo");

                        // Manejo de importe total (totalFactura o pagado)
                        Object totalObj = m.get("totalFactura") != null ? m.get("totalFactura") : m.get("pagado");
                        BigDecimal total = (totalObj != null) ? new BigDecimal(totalObj.toString()) : BigDecimal.ZERO;

                        // ✅ CORRECCIÓN CRÍTICA: Extracción y conversión de la fecha
                        Object fechaObj = m.get("fecha");
                        LocalDate fechaDto = null;
                        if (fechaObj instanceof java.sql.Date) {
                            fechaDto = ((java.sql.Date) fechaObj).toLocalDate();
                        } else if (fechaObj instanceof java.sql.Timestamp) {
                            fechaDto = ((java.sql.Timestamp) fechaObj).toLocalDateTime().toLocalDate();
                        }

                        // Retornamos el DTO con los 6 parámetros (incluyendo la fechaDto)
                        return new RecaudacionDetalleDto(
                                facturaNro != null ? facturaNro.longValue() : 0L,
                                (String) m.get("periodo"),
                                legajoNro != null ? legajoNro.longValue() : 0L,
                                (String) m.get("nombre"),
                                fechaDto, // Quinto parámetro
                                total.setScale(2, RoundingMode.HALF_UP) // Sexto parámetro
                        );
                    }).collect(Collectors.toList());

            // Cálculo del total por establecimiento
            BigDecimal totalEst = detalles.stream()
                    .map(RecaudacionDetalleDto::getPagado)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            estDto.setItems(detalles);
            estDto.setCantidadFacturas(detalles.size());
            estDto.setTotalEstablecimiento(totalEst.setScale(2, RoundingMode.HALF_UP));

            reporte.getEstablecimientos().add(estDto);
            granTotal = granTotal.add(totalEst);
            cantidadTotal += estDto.getCantidadFacturas();
        }

        reporte.setGranTotal(granTotal.setScale(2, RoundingMode.HALF_UP));
        reporte.setCantidadTotalFacturas(cantidadTotal);
        return reporte;
    }

    // 2. Método para generar el PDF tal cual la imagen
    public byte[] generarPdfFacturasPeriodo(String descripcion) {
        ReporteFacturaPeriodoDto datos = obtenerFacturasPeriodoEstructurada(descripcion);

        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        // ✅ Formato con hora para la fecha de generación
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);

        // ✅ Margen izquierdo aumentado a 40 para encarpetado
        document.setMargins(20, 20, 20, 40);

        // --- ENCABEZADO ---

        document.add(new Paragraph("Unión Vecinal de Servicios Públicos El Sauce - Colegio")
                .setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(12).setMarginBottom(2));

        // ✅ CORRECCIÓN: Se usa LocalDateTime.now() para imprimir la fecha real de creación
        document.add(new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion))
                .setTextAlignment(TextAlignment.RIGHT).setFontSize(9).setMarginBottom(2));

        document.add(new Paragraph("Facturas por Período")
                .setTextAlignment(TextAlignment.CENTER).setFontSize(14).setBold().setMarginBottom(2));

        document.add(new Paragraph("Período: " + datos.getDescripcionPeriodo())
                .setTextAlignment(TextAlignment.RIGHT).setBold().setFontSize(10).setMarginBottom(10));

        // --- LISTADO POR ESTABLECIMIENTOS ---
        for (FacturaPeriodoEstablecimientoDto est : datos.getEstablecimientos()) {
            document.add(new Paragraph(est.getNombre())
                    .setBold().setUnderline().setFontSize(10).setMarginBottom(5));

            Table table = new Table(new float[]{1.5f, 2.5f, 1.5f, 5f, 2.5f}).useAllAvailableWidth();

            // Header de la tabla
            table.addHeaderCell(new Cell().add(new Paragraph("Factura").setBold().setFontSize(9)).setBorder(Border.NO_BORDER));
            table.addHeaderCell(new Cell().add(new Paragraph("Período").setBold().setFontSize(9)).setBorder(Border.NO_BORDER));
            table.addHeaderCell(new Cell().add(new Paragraph("Legajo").setBold().setFontSize(9)).setBorder(Border.NO_BORDER));
            table.addHeaderCell(new Cell().add(new Paragraph("Apellido, Nombre").setBold().setFontSize(9)).setBorder(Border.NO_BORDER));
            table.addHeaderCell(new Cell().add(new Paragraph("Facturado").setBold().setFontSize(9)).setTextAlignment(TextAlignment.RIGHT).setBorder(Border.NO_BORDER));

            for (RecaudacionDetalleDto item : est.getItems()) {
                table.addCell(new Cell().add(new Paragraph(item.getFactura().toString()).setFontSize(8)).setBorder(Border.NO_BORDER));
                table.addCell(new Cell().add(new Paragraph(item.getPeriodo()).setFontSize(8)).setBorder(Border.NO_BORDER));
                table.addCell(new Cell().add(new Paragraph(item.getLegajo().toString()).setFontSize(8)).setBorder(Border.NO_BORDER));
                table.addCell(new Cell().add(new Paragraph(item.getNombre()).setFontSize(8)).setBorder(Border.NO_BORDER));
                // En este reporte se usa el importe facturado (item.getPagado() representa el importe de la factura aquí)
                table.addCell(new Cell().add(new Paragraph(formatoMoneda.format(item.getPagado())).setFontSize(8)).setTextAlignment(TextAlignment.RIGHT).setBorder(Border.NO_BORDER));
            }
            document.add(table);

            // Subtotal del establecimiento
            String totalEstStr = formatoMoneda.format(est.getTotalEstablecimiento());
            document.add(new Paragraph("Cantidad de Facturas: " + est.getCantidadFacturas() + " - Total: " + totalEstStr)
                    .setTextAlignment(TextAlignment.RIGHT).setBold().setFontSize(9).setMarginTop(2).setMarginBottom(10));
        }

        document.close();
        return out.toByteArray();
    }
    public ReporteRecaudacionDto obtenerRecaudacionPeriodoCompleta(String periodo) {
        List<Map<String, Object>> datos = facturaRepository.findRecaudacionFinalByPeriodo(periodo);

        Map<String, Map<String, List<Map<String, Object>>>> agrupado = datos.stream()
                .collect(Collectors.groupingBy(
                        m -> m.get("establecimiento") != null ? m.get("establecimiento").toString() : "SIN ESTABLECIMIENTO",
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                m -> m.get("medioPago") != null ? m.get("medioPago").toString() : "Manual",
                                LinkedHashMap::new,
                                Collectors.toList()
                        )
                ));

        ReporteRecaudacionDto reporte = new ReporteRecaudacionDto();
        BigDecimal granTotal = BigDecimal.ZERO;
        int acumuladorPagosGral = 0;

        for (var entryEst : agrupado.entrySet()) {
            RecaudacionEstablecimientoDto estDto = new RecaudacionEstablecimientoDto();
            estDto.setNombre(entryEst.getKey());
            BigDecimal totalEst = BigDecimal.ZERO;

            for (var entryMedio : entryEst.getValue().entrySet()) {
                RecaudacionMedioDto medioDto = new RecaudacionMedioDto();
                medioDto.setNombre(entryMedio.getKey());

                List<RecaudacionDetalleDto> items = entryMedio.getValue().stream()
                        .map(m -> {
                            // Extracción segura de valores numéricos
                            Number facturaNro = (Number) m.get("factura");
                            Number legajoNro = (Number) m.get("legajo");
                            BigDecimal pagado = m.get("pagado") != null ? new BigDecimal(m.get("pagado").toString()) : BigDecimal.ZERO;

                            // Conversión de fecha de SQL a LocalDate
                            Object fechaObj = m.get("fecha");
                            LocalDate fechaDto = null;
                            if (fechaObj instanceof java.sql.Date) {
                                fechaDto = ((java.sql.Date) fechaObj).toLocalDate();
                            } else if (fechaObj instanceof java.sql.Timestamp) {
                                fechaDto = ((java.sql.Timestamp) fechaObj).toLocalDateTime().toLocalDate();
                            }

                            // Retornamos el DTO con los 6 parámetros requeridos
                            return new RecaudacionDetalleDto(
                                    facturaNro != null ? facturaNro.longValue() : 0L,
                                    (String) m.get("periodo"),
                                    legajoNro != null ? legajoNro.longValue() : 0L,
                                    (String) m.get("nombre"),
                                    fechaDto, // Quinto parámetro: Fecha
                                    pagado    // Sexto parámetro: Pagado
                            );
                        }).collect(Collectors.toList());

                BigDecimal subtotalMedio = items.stream()
                        .map(RecaudacionDetalleDto::getPagado)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                medioDto.setItems(items);
                medioDto.setCantidadPagos(items.size());
                medioDto.setSubtotal(subtotalMedio.setScale(2, RoundingMode.HALF_UP));

                estDto.getMedios().add(medioDto);
                totalEst = totalEst.add(subtotalMedio);
                acumuladorPagosGral += items.size();
            }
            estDto.setTotalEstablecimiento(totalEst.setScale(2, RoundingMode.HALF_UP));
            reporte.getEstablecimientos().add(estDto);
            granTotal = granTotal.add(totalEst);
        }

        reporte.setGranTotal(granTotal.setScale(2, RoundingMode.HALF_UP));
        reporte.setCantidadTotalPagos(acumuladorPagosGral);

        return reporte;
    }

    public byte[] generarPdfRecaudacionPeriodoFinal(String periodo) {
        ReporteRecaudacionDto datos = obtenerRecaudacionPeriodoCompleta(periodo);

        // Configuración de formato para Argentina y fechas
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        // ✅ Se agrega formateador con hora para la generación
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(out));
        Document doc = new Document(pdf, PageSize.A4);

        // ✅ Margen izquierdo de 40 para ganchos/carpetas
        doc.setMargins(20, 20, 20, 40);

        // Encabezado
        // ✅ CORRECCIÓN: Se usa LocalDateTime.now().format()
        doc.add(new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion))
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(8)
                .setMarginBottom(0));
        doc.add(new Paragraph("Recaudación por Período")
                .setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(14));
        doc.add(new Paragraph("Período: " + periodo)
                .setTextAlignment(TextAlignment.RIGHT).setFontSize(10));

        for (RecaudacionEstablecimientoDto est : datos.getEstablecimientos()) {
            doc.add(new Paragraph("\n" + est.getNombre()).setBold().setUnderline().setFontSize(10));

            for (RecaudacionMedioDto medio : est.getMedios()) {
                doc.add(new Paragraph(medio.getNombre()).setItalic().setFontSize(9).setMarginLeft(20));

                // Tabla con 6 columnas
                Table table = new Table(new float[]{1.2f, 2f, 1.2f, 4.5f, 1.8f, 2.3f}).useAllAvailableWidth();

                // Encabezados sin bordes
                table.addHeaderCell(new Cell().add(new Paragraph("Factura").setBold().setFontSize(8)).setBorder(Border.NO_BORDER));
                table.addHeaderCell(new Cell().add(new Paragraph("Período").setBold().setFontSize(8)).setBorder(Border.NO_BORDER));
                table.addHeaderCell(new Cell().add(new Paragraph("Legajo").setBold().setFontSize(8)).setBorder(Border.NO_BORDER));
                table.addHeaderCell(new Cell().add(new Paragraph("Apellido, Nombre").setBold().setFontSize(8)).setBorder(Border.NO_BORDER));
                table.addHeaderCell(new Cell().add(new Paragraph("Fecha").setBold().setFontSize(8)).setBorder(Border.NO_BORDER));
                table.addHeaderCell(new Cell().add(new Paragraph("Pagado").setBold().setFontSize(8)).setTextAlignment(TextAlignment.RIGHT).setBorder(Border.NO_BORDER));

                for (RecaudacionDetalleDto item : medio.getItems()) {
                    table.addCell(new Cell().add(new Paragraph(item.getFactura().toString()).setFontSize(7)).setBorder(Border.NO_BORDER));
                    table.addCell(new Cell().add(new Paragraph(item.getPeriodo()).setFontSize(7)).setBorder(Border.NO_BORDER));
                    table.addCell(new Cell().add(new Paragraph(item.getLegajo().toString()).setFontSize(7)).setBorder(Border.NO_BORDER));
                    table.addCell(new Cell().add(new Paragraph(item.getNombre()).setFontSize(7)).setBorder(Border.NO_BORDER));

                    String fechaFormateada = item.getFecha() != null ? item.getFecha().format(dtf) : "";
                    table.addCell(new Cell().add(new Paragraph(fechaFormateada).setFontSize(7)).setBorder(Border.NO_BORDER));

                    table.addCell(new Cell().add(new Paragraph(fmt.format(item.getPagado())).setFontSize(7)).setTextAlignment(TextAlignment.RIGHT).setBorder(Border.NO_BORDER));
                }
                doc.add(table);

                doc.add(new Paragraph("Cantidad de Pagos: " + medio.getCantidadPagos() + " - " + fmt.format(medio.getSubtotal()))
                        .setTextAlignment(TextAlignment.RIGHT).setFontSize(9).setBold());
            }

            int pagosEst = est.getMedios().stream().mapToInt(RecaudacionMedioDto::getCantidadPagos).sum();
            doc.add(new Paragraph("Cantidad de Pagos: " + pagosEst + " - " + fmt.format(est.getTotalEstablecimiento()))
                    .setTextAlignment(TextAlignment.RIGHT).setFontSize(9).setBold().setItalic().setMarginBottom(10));
        }

        // Página Final: Gran Total
        doc.add(new AreaBreak());
        doc.add(new Paragraph("Recaudación por Período").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(14));
        doc.add(new Paragraph("Período: " + periodo).setTextAlignment(TextAlignment.RIGHT).setFontSize(10));

        doc.add(new Paragraph("\nCantidad de Pagos: " + datos.getCantidadTotalPagos() + " - " + fmt.format(datos.getGranTotal()))
                .setTextAlignment(TextAlignment.RIGHT).setBold().setFontSize(11).setBorderTop(new SolidBorder(1)));

        doc.close();
        return out.toByteArray();
    }

    public ReporteRecaudacionDto obtenerRecaudacionPorFechas(LocalDate desde, LocalDate hasta) {
        // 1. Pasamos los LocalDate tal cual vienen
        List<Map<String, Object>> datos = facturaRepository.findRecaudacionPorFechas(desde, hasta);

        Map<String, Map<String, List<Map<String, Object>>>> agrupado = datos.stream()
                .collect(Collectors.groupingBy(
                        m -> m.get("establecimiento").toString(),
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                m -> m.get("medioPago").toString(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        )
                ));

        ReporteRecaudacionDto reporte = new ReporteRecaudacionDto();
        BigDecimal granTotal = BigDecimal.ZERO;
        int acumuladorPagosGral = 0;

        for (var entryEst : agrupado.entrySet()) {
            RecaudacionEstablecimientoDto estDto = new RecaudacionEstablecimientoDto();
            estDto.setNombre(entryEst.getKey());
            BigDecimal totalEst = BigDecimal.ZERO;

            for (var entryMedio : entryEst.getValue().entrySet()) {
                RecaudacionMedioDto medioDto = new RecaudacionMedioDto();
                medioDto.setNombre(entryMedio.getKey());

                List<RecaudacionDetalleDto> items = entryMedio.getValue().stream()
                        .map(m -> {
                            // 2. CONVERSIÓN SEGURA DE FECHA
                            Object fechaObj = m.get("fecha");
                            LocalDate fechaPago = null;

                            if (fechaObj instanceof java.sql.Date) {
                                fechaPago = ((java.sql.Date) fechaObj).toLocalDate();
                            } else if (fechaObj instanceof LocalDate) {
                                fechaPago = (LocalDate) fechaObj;
                            }

                            return new RecaudacionDetalleDto(
                                    ((Number) m.get("factura")).longValue(),
                                    (String) m.get("periodo"),
                                    ((Number) m.get("legajo")).longValue(),
                                    (String) m.get("nombre"),
                                    fechaPago, // Ahora es un LocalDate real
                                    new BigDecimal(m.get("pagado").toString())
                            );
                        }).collect(Collectors.toList());

                BigDecimal subtotalMedio = items.stream()
                        .map(RecaudacionDetalleDto::getPagado)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                medioDto.setItems(items);
                medioDto.setCantidadPagos(items.size());
                medioDto.setSubtotal(subtotalMedio);

                estDto.getMedios().add(medioDto);
                totalEst = totalEst.add(subtotalMedio);
                acumuladorPagosGral += items.size();
            }
            estDto.setTotalEstablecimiento(totalEst);
            reporte.getEstablecimientos().add(estDto);
            granTotal = granTotal.add(totalEst);
        }

        reporte.setGranTotal(granTotal);
        reporte.setCantidadTotalPagos(acumuladorPagosGral);
        return reporte;
    }

    public byte[] generarPdfRecaudacionPorFechas(LocalDate desde, LocalDate hasta) {
        // 1. Obtenemos los datos estructurados usando el método que ya tenemos
        ReporteRecaudacionDto datos = obtenerRecaudacionPorFechas(desde, hasta);

        // 2. Formateadores
        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("d/M/yyyy");
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");
        NumberFormat fmtMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(20, 20, 20, 40);

        // CABECERA
        document.add(new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion))
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(8)
                .setMarginBottom(0));
        document.add(new Paragraph("Recaudación por Fechas")
                .setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(14));

        document.add(new Paragraph("Fechas: " + desde.format(fmtFecha) + " - " + hasta.format(fmtFecha))
                .setTextAlignment(TextAlignment.RIGHT).setFontSize(10));

        // ITERACIÓN POR ESTABLECIMIENTO
        for (RecaudacionEstablecimientoDto est : datos.getEstablecimientos()) {
            document.add(new Paragraph("\n" + est.getNombre())
                    .setBold().setUnderline().setFontSize(11));

            // ITERACIÓN POR MEDIO DE PAGO
            for (RecaudacionMedioDto medio : est.getMedios()) {
                document.add(new Paragraph(medio.getNombre())
                        .setBold().setFontSize(9).setMarginLeft(20));

                // TABLA DE DETALLE
                float[] columnWidths = {2, 2, 2, 5, 2, 2}; // Ajustado para 6 columnas
                Table table = new Table(columnWidths).useAllAvailableWidth();

                table.addHeaderCell(new Cell().add(new Paragraph("Factura")).setFontSize(8).setBold());
                table.addHeaderCell(new Cell().add(new Paragraph("Período")).setFontSize(8).setBold());
                table.addHeaderCell(new Cell().add(new Paragraph("Legajo")).setFontSize(8).setBold());
                table.addHeaderCell(new Cell().add(new Paragraph("Apellido, Nombre")).setFontSize(8).setBold());
                table.addHeaderCell(new Cell().add(new Paragraph("Fecha")).setFontSize(8).setBold());
                table.addHeaderCell(new Cell().add(new Paragraph("Pagado")).setFontSize(8).setBold());

                for (RecaudacionDetalleDto item : medio.getItems()) {
                    table.addCell(new Cell().add(new Paragraph(item.getFactura().toString())).setFontSize(8));
                    table.addCell(new Cell().add(new Paragraph(item.getPeriodo())).setFontSize(8));
                    table.addCell(new Cell().add(new Paragraph(item.getLegajo().toString())).setFontSize(8));
                    table.addCell(new Cell().add(new Paragraph(item.getNombre())).setFontSize(8));
                    table.addCell(new Cell().add(new Paragraph(item.getFecha().format(fmtFecha))).setFontSize(8));
                    table.addCell(new Cell().add(new Paragraph(fmtMoneda.format(item.getPagado()))).setFontSize(8).setTextAlignment(TextAlignment.RIGHT));
                }
                document.add(table);

                // SUBTOTAL DEL MEDIO (Ej: Cantidad de Pagos: 22 - $ 400.698,00)
                document.add(new Paragraph("Cantidad de Pagos: " + medio.getCantidadPagos() + " - " + fmtMoneda.format(medio.getSubtotal()))
                        .setTextAlignment(TextAlignment.RIGHT).setBold().setFontSize(9));
            }

            // TOTAL DEL ESTABLECIMIENTO
            int totalPagosEst = est.getMedios().stream().mapToInt(RecaudacionMedioDto::getCantidadPagos).sum();
            document.add(new Paragraph("Cantidad de Pagos: " + totalPagosEst + " - " + fmtMoneda.format(est.getTotalEstablecimiento()))
                    .setTextAlignment(TextAlignment.RIGHT).setBold().setFontSize(9).setItalic());
        }

        // PIE DE REPORTE: EL TOTAL DE TODOS LOS ESTABLECIMIENTOS (Imagen 6)
        document.add(new Paragraph("\nCantidad de Pagos: " + datos.getCantidadTotalPagos() + " - " + fmtMoneda.format(datos.getGranTotal()))
                .setTextAlignment(TextAlignment.RIGHT).setBold().setFontSize(11).setBorderTop(new SolidBorder(1)));

        document.close();
        return out.toByteArray();
    }
}