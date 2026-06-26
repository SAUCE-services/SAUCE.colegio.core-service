package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.*;
import ar.com.sauce.colegio.rest.model.*;
import ar.com.sauce.colegio.rest.repository.*;
import ar.com.sauce.colegio.rest.repository.projection.ConceptoDetalleProjection;

import ar.com.sauce.colegio.rest.repository.projection.DeudaGeneralProjection;
import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;

import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.draw.LineSeparator;
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
import java.util.List;
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
    @Autowired
    private ConceptoService conceptoService;

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
                    cp.getFechaRegistro(),
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

    public DeudaIndividualResponseDto obtenerDeudaIndividualConTotal(Long alumnoId) {
        // 1. Obtenemos las líneas de detalle usando tu servicio de conceptos existente
        List<LineaDetalleDto> detalles = conceptoService.obtenerDeudaIndividual(alumnoId);

        // 2. Sumamos matemáticamente todos los importes usando reduce con BigDecimal
        BigDecimal totalDeuda = detalles.stream()
                .map(LineaDetalleDto::getImporte)
                .filter(Objects::nonNull) // Evitamos NullPointerException si algún importe viene null
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Envolvemos todo en el nuevo DTO
        return new DeudaIndividualResponseDto(detalles, totalDeuda);
    }

    public byte[] generarPdfDeudaIndividual(Long alumnoId) {
        DeudaIndividualResponseDto datosDeuda = obtenerDeudaIndividualConTotal(alumnoId);

        AlumnoCompletoDto alumnoDto = alumnoRepository.findById(alumnoId)
                .map(a -> {
                    AlumnoCompletoDto dto = new AlumnoCompletoDto();
                    dto.setApellido(a.getApellido());
                    dto.setNombre(a.getNombre());
                    dto.setNroDocumento(a.getNroDocumento());
                    dto.setCurso(a.getCurso());
                    return dto;
                }).orElseThrow(() -> new RuntimeException("Alumno no encontrado: " + alumnoId));

        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new java.util.Locale("es", "AR"));
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");
        DateTimeFormatter dtfTablas = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Constructor estándar de OpenPDF: Document(Rectangle, marginLeft, marginRight, marginTop, marginBottom)
        Document document = new Document(PageSize.A4, 40, 20, 20, 20);
        PdfWriter.getInstance(document, out);
        document.open();

        // FUENTES EXPLÍCITAS
        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

        // ENCABEZADO
        Paragraph fecha = new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion), fontNormal);
        fecha.setAlignment(Element.ALIGN_RIGHT);
        document.add(fecha);

        Paragraph titulo = new Paragraph("Unión Vecinal de Servicios Públicos El Sauce - Colegio", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);

        Paragraph subtitulo = new Paragraph("Reporte de Deuda Individual", fontHeader);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        document.add(subtitulo);
        document.add(Chunk.NEWLINE);

        // TABLA ALUMNO
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        try { infoTable.setWidths(new float[]{1.5f, 4.5f, 1f, 3f}); } catch (Exception ignored) {}

        addCell(infoTable, "Legajo:", fontBold);
        addCell(infoTable, alumnoId.toString(), fontNormal);
        addCell(infoTable, "Curso:", fontBold);
        addCell(infoTable, alumnoDto.getCurso() != null ? alumnoDto.getCurso() : "Sin Asignar", fontNormal);
        addCell(infoTable, "Alumno:", fontBold);
        addCell(infoTable, alumnoDto.getApellido() + ", " + alumnoDto.getNombre(), fontNormal);
        addCell(infoTable, "DNI:", fontBold);
        addCell(infoTable, alumnoDto.getNroDocumento(), fontNormal);

        document.add(infoTable);
        document.add(Chunk.NEWLINE);

        // GRILLA CONCEPTOS
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{1.8f, 4.2f, 2.2f, 1.8f, 1.8f, 2.2f}); } catch (Exception ignored) {}

        String[] headers = {"F.Estado", "Concepto", "Estado", "Importe", "F.Registro", "Periodo"};
        for (String h : headers) {
            table.addCell(new PdfPCell(new Phrase(h, fontBold)));
        }

        if (datosDeuda.getDetalles().isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("El alumno no registra deudas pendientes.", fontNormal));
            empty.setColspan(6);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(empty);
        } else {
            for (LineaDetalleDto item : datosDeuda.getDetalles()) {
                table.addCell(new Phrase(item.getFechaEstado() != null ? item.getFechaEstado().format(dtfTablas) : "", fontNormal));
                table.addCell(new Phrase(item.getConcepto(), fontNormal));
                table.addCell(new Phrase(item.getEstado(), fontNormal));

                PdfPCell imp = new PdfPCell(new Phrase(item.getImporte() != null ? formatoMoneda.format(item.getImporte()) : "$ 0,00", fontNormal));
                imp.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(imp);

                table.addCell(new Phrase(item.getFechaRegistro() != null ? item.getFechaRegistro().format(dtfTablas) : "", fontNormal));
                table.addCell(new Phrase(item.getPeriodo(), fontNormal));
            }
        }
        document.add(table);

        // TOTAL
        Paragraph total = new Paragraph("TOTAL DEUDA: " + formatoMoneda.format(datosDeuda.getTotalDeuda()), fontBold);
        total.setAlignment(Element.ALIGN_RIGHT);
        document.add(total);

        document.close();
        return out.toByteArray();
    }

    // Método auxiliar para simplificar celdas sin bordes
    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
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
        ReporteRecaudacionDto datos = obtenerRecaudacionEstructurada(fecha);
        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Constructor de OpenPDF: Document(PageSize, marginLeft, marginRight, marginTop, marginBottom)
        Document document = new Document(PageSize.A4, 40, 20, 20, 20);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // FUENTES
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

            // ENCABEZADO
            Paragraph pFecha = new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion), fontNormal);
            pFecha.setAlignment(Element.ALIGN_RIGHT);
            document.add(pFecha);

            Paragraph pTitulo = new Paragraph("Unión Vecinal de Servicios Públicos El Sauce - Colegio", fontTitulo);
            pTitulo.setAlignment(Element.ALIGN_CENTER);
            document.add(pTitulo);

            Paragraph pSub = new Paragraph("Recaudación Diaria", fontNormal);
            pSub.setAlignment(Element.ALIGN_CENTER);
            document.add(pSub);

            Paragraph pFechaPago = new Paragraph("Fecha Pago: " + fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontBold);
            pFechaPago.setAlignment(Element.ALIGN_RIGHT);
            document.add(pFechaPago);
            document.add(Chunk.NEWLINE);

            // CONTENIDO
            for (RecaudacionEstablecimientoDto est : datos.getEstablecimientos()) {
                document.add(new Paragraph(est.getNombre(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));

                for (RecaudacionMedioDto medio : est.getMedios()) {
                    document.add(new Paragraph(medio.getNombre(), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9)));

                    PdfPTable table = new PdfPTable(5);
                    table.setWidthPercentage(100);
                    table.setSpacingBefore(5f);

                    // Encabezados
                    String[] headers = {"Factura", "Período", "Legajo", "Apellido, Nombre", "Pagado"};
                    for (String h : headers) {
                        PdfPCell cell = new PdfPCell(new Phrase(h, fontBold));
                        table.addCell(cell);
                    }

                    // Datos
                    for (RecaudacionDetalleDto item : medio.getItems()) {
                        table.addCell(new Phrase(item.getFactura().toString(), fontNormal));
                        table.addCell(new Phrase(item.getPeriodo(), fontNormal));
                        table.addCell(new Phrase(item.getLegajo().toString(), fontNormal));
                        table.addCell(new Phrase(item.getNombre(), fontNormal));

                        PdfPCell cellImp = new PdfPCell(new Phrase(formatoMoneda.format(item.getPagado()), fontNormal));
                        cellImp.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        table.addCell(cellImp);
                    }
                    document.add(table);

                    // Subtotal
                    Paragraph subtotal = new Paragraph("Cantidad de Pagos: " + medio.getCantidadPagos() +
                            " - Subtotal: " + formatoMoneda.format(medio.getSubtotal()), fontBold);
                    subtotal.setAlignment(Element.ALIGN_RIGHT);
                    document.add(subtotal);
                    document.add(Chunk.NEWLINE);
                }
            }

            // TOTAL GENERAL
            Paragraph totalGral = new Paragraph("TOTAL GENERAL: " + formatoMoneda.format(datos.getGranTotal()), fontTitulo);
            totalGral.setAlignment(Element.ALIGN_RIGHT);
            document.add(totalGral);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF", e);
        }

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
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Constructor de OpenPDF: Document(PageSize, marginLeft, marginRight, marginTop, marginBottom)
        // El margen izquierdo (40) se mantiene para el encarpetado
        Document document = new Document(PageSize.A4, 40, 20, 20, 20);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // FUENTES
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font fontEncabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontTotalGral = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

            // --- ENCABEZADO ---
            Paragraph pTitulo = new Paragraph("Unión Vecinal de Servicios Públicos El Sauce - Colegio", fontEncabezado);
            pTitulo.setAlignment(Element.ALIGN_CENTER);
            document.add(pTitulo);

            Paragraph pFecha = new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion), fontNormal);
            pFecha.setAlignment(Element.ALIGN_RIGHT);
            document.add(pFecha);

            Paragraph pReporte = new Paragraph("Facturas por Período", fontTitulo);
            pReporte.setAlignment(Element.ALIGN_CENTER);
            document.add(pReporte);

            Paragraph pPeriodo = new Paragraph("Período: " + datos.getDescripcionPeriodo(), fontBold);
            pPeriodo.setAlignment(Element.ALIGN_RIGHT);
            document.add(pPeriodo);
            document.add(Chunk.NEWLINE);

            // --- LISTADO POR ESTABLECIMIENTOS ---
            for (FacturaPeriodoEstablecimientoDto est : datos.getEstablecimientos()) {
                Paragraph pEst = new Paragraph(est.getNombre(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
                pEst.setSpacingBefore(5f);
                document.add(pEst);

                // Tabla con 5 columnas
                PdfPTable table = new PdfPTable(new float[]{1.5f, 2.5f, 1.5f, 5f, 2.5f});
                table.setWidthPercentage(100);
                table.setSpacingBefore(5f);

                // Encabezados
                String[] headers = {"Factura", "Período", "Legajo", "Apellido, Nombre", "Facturado"};
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, fontBold));
                    cell.setBorder(PdfPCell.NO_BORDER);
                    table.addCell(cell);
                }

                // Datos
                for (RecaudacionDetalleDto item : est.getItems()) {
                    table.addCell(new PdfPCell(new Phrase(item.getFactura().toString(), fontNormal)) {{ setBorder(PdfPCell.NO_BORDER); }});
                    table.addCell(new PdfPCell(new Phrase(item.getPeriodo(), fontNormal)) {{ setBorder(PdfPCell.NO_BORDER); }});
                    table.addCell(new PdfPCell(new Phrase(item.getLegajo().toString(), fontNormal)) {{ setBorder(PdfPCell.NO_BORDER); }});
                    table.addCell(new PdfPCell(new Phrase(item.getNombre(), fontNormal)) {{ setBorder(PdfPCell.NO_BORDER); }});

                    PdfPCell cellImp = new PdfPCell(new Phrase(formatoMoneda.format(item.getPagado()), fontNormal));
                    cellImp.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    cellImp.setBorder(PdfPCell.NO_BORDER);
                    table.addCell(cellImp);
                }
                document.add(table);

                // Subtotal
                Paragraph subtotal = new Paragraph("Cantidad de Facturas: " + est.getCantidadFacturas() +
                        " - Total: " + formatoMoneda.format(est.getTotalEstablecimiento()), fontBold);
                subtotal.setAlignment(Element.ALIGN_RIGHT);
                subtotal.setSpacingBefore(5f);
                document.add(subtotal);
            }

            // 🌟 --- SECCIÓN ADICIONADA: TOTAL DE TODO TODO ---
            document.add(new Chunk(new org.openpdf.text.pdf.draw.LineSeparator(0.5f, 100, null, Element.ALIGN_CENTER, -2)));

            Paragraph pFinal = new Paragraph(
                    "\nTOTAL GENERAL DEL PERÍODO (" + datos.getDescripcionPeriodo() + ")\n" +
                            "Cantidad Total de Facturas: " + datos.getCantidadTotalFacturas() + " - " +
                            "Monto Consolidado: " + formatoMoneda.format(datos.getGranTotal()),
                    fontTotalGral
            );
            pFinal.setAlignment(Element.ALIGN_RIGHT);
            pFinal.setSpacingBefore(8f);
            document.add(pFinal);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de facturas por periodo", e);
        }

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

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 20, 20, 20);

        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            // FUENTES
            Font font7 = FontFactory.getFont(FontFactory.HELVETICA, 7);
            Font font8 = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font font8B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font font9 = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font font9B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font font10B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font font11B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font font14B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);

            // ENCABEZADO
            Paragraph pGen = new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion), font8);
            pGen.setAlignment(Element.ALIGN_RIGHT);
            doc.add(pGen);

            Paragraph pTit = new Paragraph("Recaudación por Período", font14B);
            pTit.setAlignment(Element.ALIGN_CENTER);
            doc.add(pTit);

            Paragraph pPer = new Paragraph("Período: " + periodo, FontFactory.getFont(FontFactory.HELVETICA, 10));
            pPer.setAlignment(Element.ALIGN_RIGHT);
            doc.add(pPer);

            // LISTADO
            for (RecaudacionEstablecimientoDto est : datos.getEstablecimientos()) {
                doc.add(new Paragraph("\n" + est.getNombre(), font10B));

                for (RecaudacionMedioDto medio : est.getMedios()) {
                    Paragraph pMedio = new Paragraph(medio.getNombre(), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9));
                    pMedio.setIndentationLeft(20);
                    doc.add(pMedio);

                    PdfPTable table = new PdfPTable(new float[]{1.2f, 2f, 1.2f, 4.5f, 1.8f, 2.3f});
                    table.setWidthPercentage(100);
                    table.setSpacingBefore(5f);

                    // Encabezados
                    String[] headers = {"Factura", "Período", "Legajo", "Apellido, Nombre", "Fecha", "Pagado"};
                    for (String h : headers) {
                        PdfPCell cell = new PdfPCell(new Phrase(h, font8B));
                        cell.setBorder(PdfPCell.NO_BORDER);
                        table.addCell(cell);
                    }

                    // Datos
                    for (RecaudacionDetalleDto item : medio.getItems()) {
                        table.addCell(new PdfPCell(new Phrase(item.getFactura().toString(), font7)) {{ setBorder(PdfPCell.NO_BORDER); }});
                        table.addCell(new PdfPCell(new Phrase(item.getPeriodo(), font7)) {{ setBorder(PdfPCell.NO_BORDER); }});
                        table.addCell(new PdfPCell(new Phrase(item.getLegajo().toString(), font7)) {{ setBorder(PdfPCell.NO_BORDER); }});
                        table.addCell(new PdfPCell(new Phrase(item.getNombre(), font7)) {{ setBorder(PdfPCell.NO_BORDER); }});
                        table.addCell(new PdfPCell(new Phrase(item.getFecha() != null ? item.getFecha().format(dtf) : "", font7)) {{ setBorder(PdfPCell.NO_BORDER); }});

                        PdfPCell cellImp = new PdfPCell(new Phrase(fmt.format(item.getPagado()), font7));
                        cellImp.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        cellImp.setBorder(PdfPCell.NO_BORDER);
                        table.addCell(cellImp);
                    }
                    doc.add(table);

                    Paragraph pSub = new Paragraph("Cantidad de Pagos: " + medio.getCantidadPagos() + " - " + fmt.format(medio.getSubtotal()), font9B);
                    pSub.setAlignment(Element.ALIGN_RIGHT);
                    doc.add(pSub);
                }

                int pagosEst = est.getMedios().stream().mapToInt(RecaudacionMedioDto::getCantidadPagos).sum();
                Paragraph pEstTotal = new Paragraph("Cantidad de Pagos: " + pagosEst + " - " + fmt.format(est.getTotalEstablecimiento()), FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 9));
                pEstTotal.setAlignment(Element.ALIGN_RIGHT);
                pEstTotal.setSpacingAfter(10f);
                doc.add(pEstTotal);
            }

            // PÁGINA FINAL
            doc.newPage();
            doc.add(new Paragraph("Recaudación por Período", font14B));
            doc.add(new Paragraph("Período: " + periodo, FontFactory.getFont(FontFactory.HELVETICA, 10)));

            Paragraph pFinal = new Paragraph("\nCantidad de Pagos: " + datos.getCantidadTotalPagos() + " - " + fmt.format(datos.getGranTotal()), font11B);
            pFinal.setAlignment(Element.ALIGN_RIGHT);
            // OpenPDF no tiene setBorderTop directo en Paragraph, dibujamos una línea simple
            doc.add(new Chunk(new org.openpdf.text.pdf.draw.LineSeparator(0.5f, 100, null, Element.ALIGN_CENTER, -2)));
            doc.add(pFinal);

            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de recaudación final", e);
        }
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
        ReporteRecaudacionDto datos = obtenerRecaudacionPorFechas(desde, hasta);

        // Formateadores
        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");
        NumberFormat fmtMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Documento con margen izquierdo de 40 para encuadernación
        Document document = new Document(PageSize.A4, 40, 20, 20, 20);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fuentes
            Font font8 = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font font8B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font font9B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font font11B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font font14B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);

            // CABECERA
            Paragraph pGen = new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion), font8);
            pGen.setAlignment(Element.ALIGN_RIGHT);
            document.add(pGen);

            Paragraph pTit = new Paragraph("Recaudación por Fechas", font14B);
            pTit.setAlignment(Element.ALIGN_CENTER);
            document.add(pTit);

            Paragraph pFechas = new Paragraph("Fechas: " + desde.format(fmtFecha) + " - " + hasta.format(fmtFecha), FontFactory.getFont(FontFactory.HELVETICA, 10));
            pFechas.setAlignment(Element.ALIGN_RIGHT);
            document.add(pFechas);
            document.add(Chunk.NEWLINE);

            // ITERACIÓN POR ESTABLECIMIENTO
            for (RecaudacionEstablecimientoDto est : datos.getEstablecimientos()) {
                document.add(new Paragraph(est.getNombre(), FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 11)));

                // ITERACIÓN POR MEDIO DE PAGO
                for (RecaudacionMedioDto medio : est.getMedios()) {
                    Paragraph pMedio = new Paragraph(medio.getNombre(), font9B);
                    pMedio.setIndentationLeft(20);
                    document.add(pMedio);

                    // TABLA DE DETALLE (6 columnas)
                    PdfPTable table = new PdfPTable(new float[]{2, 2, 2, 5, 2, 2});
                    table.setWidthPercentage(100);
                    table.setSpacingBefore(5f);

                    // Cabeceras
                    String[] headers = {"Factura", "Período", "Legajo", "Apellido, Nombre", "Fecha", "Pagado"};
                    for (String h : headers) {
                        table.addCell(new PdfPCell(new Phrase(h, font8B)) {{ setBorder(PdfPCell.BOTTOM); }});
                    }

                    // Filas
                    for (RecaudacionDetalleDto item : medio.getItems()) {
                        table.addCell(new PdfPCell(new Phrase(item.getFactura().toString(), font8)) {{ setBorder(PdfPCell.NO_BORDER); }});
                        table.addCell(new PdfPCell(new Phrase(item.getPeriodo(), font8)) {{ setBorder(PdfPCell.NO_BORDER); }});
                        table.addCell(new PdfPCell(new Phrase(item.getLegajo().toString(), font8)) {{ setBorder(PdfPCell.NO_BORDER); }});
                        table.addCell(new PdfPCell(new Phrase(item.getNombre(), font8)) {{ setBorder(PdfPCell.NO_BORDER); }});
                        table.addCell(new PdfPCell(new Phrase(item.getFecha().format(fmtFecha), font8)) {{ setBorder(PdfPCell.NO_BORDER); }});

                        PdfPCell cellImp = new PdfPCell(new Phrase(fmtMoneda.format(item.getPagado()), font8));
                        cellImp.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        cellImp.setBorder(PdfPCell.NO_BORDER);
                        table.addCell(cellImp);
                    }
                    document.add(table);

                    Paragraph pSub = new Paragraph("Cantidad de Pagos: " + medio.getCantidadPagos() + " - " + fmtMoneda.format(medio.getSubtotal()), font9B);
                    pSub.setAlignment(Element.ALIGN_RIGHT);
                    document.add(pSub);
                }

                int totalPagosEst = est.getMedios().stream().mapToInt(RecaudacionMedioDto::getCantidadPagos).sum();
                Paragraph pEstTotal = new Paragraph("Cantidad de Pagos: " + totalPagosEst + " - " + fmtMoneda.format(est.getTotalEstablecimiento()), FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 9));
                pEstTotal.setAlignment(Element.ALIGN_RIGHT);
                pEstTotal.setSpacingAfter(10f);
                document.add(pEstTotal);
            }

            // PIE DE REPORTE (Gran Total)
            document.add(new Chunk(new org.openpdf.text.pdf.draw.LineSeparator(0.5f, 100, null, Element.ALIGN_CENTER, -2)));
            Paragraph pFinal = new Paragraph("\nCantidad de Pagos: " + datos.getCantidadTotalPagos() + " - " + fmtMoneda.format(datos.getGranTotal()), font11B);
            pFinal.setAlignment(Element.ALIGN_RIGHT);
            document.add(pFinal);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de recaudación por fechas", e);
        }

        return out.toByteArray();
    }

    public List<DeudaGeneralDto> obtenerDeudaGeneral() {
        List<DeudaGeneralProjection> filas = facturaRepository.findDeudaGeneralCompleta();

        // Agrupamos en un mapa para estructurar la jerarquía Alumno -> Facturas
        Map<Long, DeudaGeneralDto> mapaDeuda = new LinkedHashMap<>();

        for (DeudaGeneralProjection fila : filas) {
            DeudaGeneralDto dto = mapaDeuda.computeIfAbsent(fila.getIdAlumno(), id -> {
                DeudaGeneralDto nuevo = new DeudaGeneralDto();
                nuevo.setIdAlumno(fila.getIdAlumno());
                nuevo.setLegajo(fila.getLegajo());
                nuevo.setDni(fila.getDni());
                nuevo.setNombreAlumno(fila.getAlumno());
                nuevo.setFacturas(new ArrayList<>());
                nuevo.setTotalDeudaAlumno(BigDecimal.ZERO);
                return nuevo;
            });

            DeudaGeneralDto.FacturaPendienteDto factDto = new DeudaGeneralDto.FacturaPendienteDto();
            factDto.setNroFactura(fila.getFactura());
            factDto.setPeriodo(fila.getPeriodo());
            factDto.setFechaVencimiento(fila.getVencimiento());
            factDto.setImporte(fila.getImporte());

            dto.getFacturas().add(factDto);
            dto.setTotalDeudaAlumno(dto.getTotalDeudaAlumno().add(fila.getImporte()));
        }

        return new ArrayList<>(mapaDeuda.values());
    }

    public byte[] generarPdfDeudaGeneral() {
        // 1. Obtenemos los datos agrupados usando la lógica que armamos antes
        List<DeudaGeneralDto> deudas = obtenerDeudaGeneral();

        // Formateadores
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");
        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        NumberFormat fmtMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Formato A4 con márgenes idénticos a tu reporte de recaudación
        Document document = new Document(PageSize.A4, 40, 20, 20, 20);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fuentes estándar
            Font font8 = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font font8B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font font9B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font font11B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font font14B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);

            // Cabecera Principal del Documento
            Paragraph pInstitucion = new Paragraph("Unión Vecinal de Servicios Públicos El Sauce - Colegio", font9B);
            pInstitucion.setAlignment(Element.ALIGN_LEFT);
            document.add(pInstitucion);

            Paragraph pGen = new Paragraph(LocalDateTime.now().format(dtfGeneracion), font8);
            pGen.setAlignment(Element.ALIGN_RIGHT);
            document.add(pGen);

            Paragraph pTit = new Paragraph("Deuda General", font14B);
            pTit.setAlignment(Element.ALIGN_CENTER);
            document.add(pTit);
            document.add(Chunk.NEWLINE);

            BigDecimal granTotalDeuda = BigDecimal.ZERO;

            // Iteración sobre cada Alumno con Deuda Activa
            for (DeudaGeneralDto alumno : deudas) {

                // Línea informativa del alumno: Legajo (DNI) Apellido, Nombre
                String encabezadoAlumno = String.format("Alumno: %s (%s) %s",
                        alumno.getLegajo(),
                        alumno.getDni() != null ? alumno.getDni() : "S/D",
                        alumno.getNombreAlumno());

                Paragraph pAlumno = new Paragraph(encabezadoAlumno, font8B);
                pAlumno.setSpacingBefore(8f);
                document.add(pAlumno);

                // Tabla de Facturas Pendientes (4 columnas para coincidir con el diseño)
                // Proporciones: Factura (3), Período (4), Vencimiento (3), Importe (2)
                PdfPTable table = new PdfPTable(new float[]{3, 4, 3, 2});
                table.setWidthPercentage(90);
                table.setHorizontalAlignment(Element.ALIGN_RIGHT); // Indentada a la derecha
                table.setSpacingBefore(3f);

                // Encabezados de la tabla interna
                String[] cabeceras = {"Factura", "Período", "Vencimiento", "Importe"};
                for (String c : cabeceras) {
                    PdfPCell cellHeader = new PdfPCell(new Phrase(c, font8B));
                    cellHeader.setBorder(PdfPCell.BOTTOM);
                    if (c.equals("Importe")) {
                        cellHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    }
                    table.addCell(cellHeader);
                }

                // Filas de comprobantes adeudados
                for (DeudaGeneralDto.FacturaPendienteDto fac : alumno.getFacturas()) {
                    table.addCell(new PdfPCell(new Phrase(fac.getNroFactura(), font8)) {{ setBorder(PdfPCell.NO_BORDER); }});
                    table.addCell(new PdfPCell(new Phrase(fac.getPeriodo(), font8)) {{ setBorder(PdfPCell.NO_BORDER); }});

                    String fechaVenc = fac.getFechaVencimiento() != null ? fac.getFechaVencimiento().format(fmtFecha) : "-";
                    table.addCell(new PdfPCell(new Phrase(fechaVenc, font8)) {{ setBorder(PdfPCell.NO_BORDER); }});

                    PdfPCell cellImp = new PdfPCell(new Phrase(fmtMoneda.format(fac.getImporte()), font8));
                    cellImp.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    cellImp.setBorder(PdfPCell.NO_BORDER);
                    table.addCell(cellImp);
                }
                document.add(table);

                // Subtotal por Alumno
                Paragraph pSubTotal = new Paragraph("Total por Alumno: " + fmtMoneda.format(alumno.getTotalDeudaAlumno()), font8B);
                pSubTotal.setAlignment(Element.ALIGN_RIGHT);
                pSubTotal.setSpacingAfter(5f);
                document.add(pSubTotal);

                granTotalDeuda = granTotalDeuda.add(alumno.getTotalDeudaAlumno());
            }

            // Cierre del Reporte con el Gran Total Consolidado
            document.add(new Chunk(new LineSeparator(0.5f, 100, null, Element.ALIGN_CENTER, -2)));

            Paragraph pFinal = new Paragraph("\nTOTAL DEUDA GENERAL: " + fmtMoneda.format(granTotalDeuda), font11B);
            pFinal.setAlignment(Element.ALIGN_RIGHT);
            document.add(pFinal);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de deuda general", e);
        }

        return out.toByteArray();
    }
}