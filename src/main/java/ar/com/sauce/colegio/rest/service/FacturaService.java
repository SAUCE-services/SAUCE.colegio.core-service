package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.*;
import ar.com.sauce.colegio.rest.model.*;
import ar.com.sauce.colegio.rest.repository.*;
import ar.com.sauce.colegio.rest.repository.projection.ConceptoConEstadoProjection;
import ar.com.sauce.colegio.rest.repository.projection.ConceptoDetalleProjection;
import ar.com.sauce.colegio.rest.repository.projection.FacturacionConceptoProjection;

import ar.com.sauce.colegio.rest.repository.projection.DeudaGeneralProjection;
import jakarta.transaction.Transactional;
import org.openpdf.text.*;
import org.openpdf.text.pdf.Barcode39;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;

import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.draw.LineSeparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


import java.io.ByteArrayOutputStream;
import java.awt.Color;
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
    private PdfLogoService pdfLogoService;
    @Autowired
    private IConceptoRepository conceptoRepository;
    @Autowired
    private IPeriodoRepository periodoRepository;
    @Autowired
    private ICursoRepository cursoRepository;
    @Autowired
    private ConceptoService conceptoService;
    @Autowired
    private TransaccionService transaccionService;

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

        // 🌟 Establecimiento actual del alumno, solo para saber qué logo mostrar
        String nombreEstablecimiento = cursoRepository.findCursoActualDeAlumno(alumnoId)
                .map(c -> c.getEstablecimiento() != null ? c.getEstablecimiento().getNombre() : null)
                .orElse(null);

        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new java.util.Locale("es", "AR"));
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");
        DateTimeFormatter dtfTablas = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            // 🌟 Acá armamos el Document a mano (en vez de iniciarDocumentoPdf) porque
            // necesitamos guardar el PdfWriter para poder colocar el logo
            Document document = new Document(PageSize.A4, 40, 20, 20, 20);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            FuentesReporte fu = crearFuentesReporte();
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Font fontNormal = fu.f9;
            Font fontBold = fu.f9B;

            pdfLogoService.colocarArribaDerecha(document, writer, nombreEstablecimiento);

            // ENCABEZADO
            Paragraph fecha = new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion), fontNormal);
            fecha.setAlignment(Element.ALIGN_LEFT);
            document.add(fecha);

            Paragraph titulo = new Paragraph("Unión Vecinal de Servicios Públicos El Sauce - Colegio", fu.f11B);
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
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de deuda individual", e);
        }

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
            Field field = ar.com.sauce.colegio.rest.model.Auditable.class.getDeclaredField("created");
            field.setAccessible(true);
            LocalDateTime fecha = (LocalDateTime) field.get(factura);

            // 🌟 CORRECCIÓN: Si el campo en la BD es null, devolvemos la fecha del estado o la actual
            if (fecha == null) {
                return factura.getFechaEstado() != null ? factura.getFechaEstado().atStartOfDay() : LocalDateTime.now();
            }
            return fecha;
        } catch (Exception e) {
            // Fallback si falla el Reflection
            return factura.getFechaEstado() != null ? factura.getFechaEstado().atStartOfDay() : LocalDateTime.now();
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
        int cantidadTotalPagosGral = 0;

        for (var entryEst : agrupado.entrySet()) {
            RecaudacionEstablecimientoDto estDto = new RecaudacionEstablecimientoDto();
            estDto.setNombre(entryEst.getKey());
            BigDecimal totalEst = BigDecimal.ZERO;
            int cantidadPagosEst = 0;

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
                cantidadPagosEst += medioDto.getCantidadPagos();
            }

            estDto.setTotalEstablecimiento(totalEst);
            estDto.setCantidadPagos(cantidadPagosEst);
            reporte.getEstablecimientos().add(estDto);
            granTotal = granTotal.add(totalEst);
            cantidadTotalPagosGral += cantidadPagosEst;
        }

        reporte.setGranTotal(granTotal);
        reporte.setCantidadTotalPagos(cantidadTotalPagosGral);
        return reporte;
    }

    public byte[] generarPdfRecaudacion(LocalDate fecha) {
        ReporteRecaudacionDto datos = obtenerRecaudacionEstructurada(fecha);
        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = iniciarDocumentoPdf(out);
            FuentesReporte fu = crearFuentesReporte();
            Font fontNormal = fu.f9;
            Font fontBold = fu.f9B;
            Font fontTitulo = fu.f11B;

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
                document.add(new Paragraph(est.getNombre(), fontTitulo));

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

                // 🌟 Total del establecimiento (suma de todos sus medios de pago)
                Paragraph totalEstablecimiento = new Paragraph(
                        "Total " + ": Cantidad de Pagos: " + est.getCantidadPagos() +
                                " - Subtotal: " + formatoMoneda.format(est.getTotalEstablecimiento()), fontTitulo);
                totalEstablecimiento.setAlignment(Element.ALIGN_RIGHT);
                totalEstablecimiento.setSpacingAfter(10f);
                document.add(totalEstablecimiento);
            }

            // TOTAL GENERAL
            Paragraph totalGral = new Paragraph(
                    "Cantidad Total de Pagos: " + datos.getCantidadTotalPagos() +
                            " - TOTAL GENERAL: " + formatoMoneda.format(datos.getGranTotal()), fontTitulo);
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

    // 🌟 Facturación por Concepto/Rubro — por Período
    public ReporteFacturacionConceptoDto obtenerFacturacionPorConceptoYPeriodo(String periodo) {
        List<FacturacionConceptoProjection> filas = conceptoRepository.findFacturacionPorConceptoYPeriodo(periodo);
        return construirReporteFacturacionConcepto(filas, periodo);
    }

    // 🌟 Agrupa las filas planas (alumno + concepto + factura) en un reporte por concepto,
    // separando totales FACTURADO (todo lo que se facturó, esté pagado o no) y
    // COBRADO (solo lo que corresponde a facturas ya pagadas)
    private ReporteFacturacionConceptoDto construirReporteFacturacionConcepto(
            List<FacturacionConceptoProjection> filas, String filtroDescripcion) {

        ReporteFacturacionConceptoDto reporte = new ReporteFacturacionConceptoDto();
        reporte.setFiltroDescripcion(filtroDescripcion);
        reporte.setFechaGeneracion(LocalDateTime.now());

        // Mantiene el orden de aparición (la query ya viene ordenada por concepto)
        java.util.LinkedHashMap<String, FacturacionConceptoDto> agrupado = new java.util.LinkedHashMap<>();

        for (FacturacionConceptoProjection fila : filas) {
            FacturacionConceptoDto conceptoDto = agrupado.computeIfAbsent(fila.getConcepto(), nombre -> {
                FacturacionConceptoDto nuevo = new FacturacionConceptoDto();
                nuevo.setNombreConcepto(nombre);
                nuevo.setTotalFacturado(BigDecimal.ZERO);
                nuevo.setTotalCobrado(BigDecimal.ZERO);
                nuevo.setCantidadFacturado(0);
                nuevo.setCantidadCobrado(0);
                return nuevo;
            });

            boolean pagado = fila.getPagado() != null && fila.getPagado() == 1;
            BigDecimal importe = fila.getImporte() != null ? fila.getImporte() : BigDecimal.ZERO;

            conceptoDto.getDetalles().add(new ConceptoDetalleAlumnoDto(
                    fila.getLegajo(), fila.getNombreAlumno(), fila.getNroFactura(), importe,
                    fila.getFechaFactura(), fila.getFechaPago(), fila.getPeriodo(), pagado
            ));

            conceptoDto.setTotalFacturado(conceptoDto.getTotalFacturado().add(importe));
            conceptoDto.setCantidadFacturado(conceptoDto.getCantidadFacturado() + 1);

            if (pagado) {
                conceptoDto.setTotalCobrado(conceptoDto.getTotalCobrado().add(importe));
                conceptoDto.setCantidadCobrado(conceptoDto.getCantidadCobrado() + 1);
            }
        }

        BigDecimal granTotalFacturado = BigDecimal.ZERO;
        BigDecimal granTotalCobrado = BigDecimal.ZERO;
        int cantidadTotalFacturado = 0;
        int cantidadTotalCobrado = 0;

        for (FacturacionConceptoDto c : agrupado.values()) {
            granTotalFacturado = granTotalFacturado.add(c.getTotalFacturado());
            granTotalCobrado = granTotalCobrado.add(c.getTotalCobrado());
            cantidadTotalFacturado += c.getCantidadFacturado();
            cantidadTotalCobrado += c.getCantidadCobrado();
        }

        reporte.setConceptos(new java.util.ArrayList<>(agrupado.values()));
        reporte.setGranTotalFacturado(granTotalFacturado.setScale(2, RoundingMode.HALF_UP));
        reporte.setGranTotalCobrado(granTotalCobrado.setScale(2, RoundingMode.HALF_UP));
        reporte.setCantidadTotalFacturado(cantidadTotalFacturado);
        reporte.setCantidadTotalCobrado(cantidadTotalCobrado);
        return reporte;
    }

    // 🌟 PDF de "Facturación por Concepto/Rubro"
    // 🌟 Agrupa las fuentes que se repiten en varios reportes PDF (evita declarar las
    // mismas 8-9 fuentes una y otra vez en cada método generarPdfXxx)
    private static class FuentesReporte {
        Font f7, f8, f8B, f9, f9B, f10B, f11B, f12B, f14B;
    }

    private FuentesReporte crearFuentesReporte() {
        FuentesReporte f = new FuentesReporte();
        f.f7 = FontFactory.getFont(FontFactory.HELVETICA, 7);
        f.f8 = FontFactory.getFont(FontFactory.HELVETICA, 8);
        f.f8B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        f.f9 = FontFactory.getFont(FontFactory.HELVETICA, 9);
        f.f9B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        f.f10B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        f.f11B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        f.f12B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        f.f14B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        return f;
    }

    // 🌟 Arma el Document + PdfWriter con la configuración estándar (A4, márgenes) y ya
    // lo deja abierto, listo para escribir
    private Document iniciarDocumentoPdf(ByteArrayOutputStream out) throws Exception {
        Document doc = new Document(PageSize.A4, 40, 20, 20, 20);
        PdfWriter.getInstance(doc, out);
        doc.open();
        return doc;
    }

    public byte[] generarPdfFacturacionConcepto(ReporteFacturacionConceptoDto datos) {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document doc = iniciarDocumentoPdf(out);
            FuentesReporte fu = crearFuentesReporte();

            Paragraph pGen = new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion), fu.f8);
            pGen.setAlignment(Element.ALIGN_RIGHT);
            doc.add(pGen);

            Paragraph pTit = new Paragraph("Facturación por Concepto / Rubro", fu.f14B);
            pTit.setAlignment(Element.ALIGN_CENTER);
            doc.add(pTit);

            Paragraph pFiltro = new Paragraph("Filtro: " + datos.getFiltroDescripcion(), FontFactory.getFont(FontFactory.HELVETICA, 10));
            pFiltro.setAlignment(Element.ALIGN_RIGHT);
            doc.add(pFiltro);

            for (FacturacionConceptoDto concepto : datos.getConceptos()) {
                doc.add(new Paragraph("\n" + concepto.getNombreConcepto(), fu.f10B));

                PdfPTable table = new PdfPTable(new float[]{1.2f, 2f, 4.5f, 1.8f, 1.8f, 2.3f});
                table.setWidthPercentage(100);
                table.setSpacingBefore(5f);

                String[] headers = {"Factura", "Legajo", "Apellido, Nombre", "F. Factura", "F. Pago", "Importe"};
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, fu.f8B));
                    cell.setBorder(PdfPCell.NO_BORDER);
                    table.addCell(cell);
                }

                for (ConceptoDetalleAlumnoDto item : concepto.getDetalles()) {
                    table.addCell(new PdfPCell(new Phrase(item.getNroFactura().toString(), fu.f7)) {{ setBorder(PdfPCell.NO_BORDER); }});
                    table.addCell(new PdfPCell(new Phrase(item.getLegajo().toString(), fu.f7)) {{ setBorder(PdfPCell.NO_BORDER); }});
                    table.addCell(new PdfPCell(new Phrase(item.getNombreAlumno(), fu.f7)) {{ setBorder(PdfPCell.NO_BORDER); }});
                    table.addCell(new PdfPCell(new Phrase(item.getFechaFactura() != null ? item.getFechaFactura().format(dtf) : "-", fu.f7)) {{ setBorder(PdfPCell.NO_BORDER); }});
                    table.addCell(new PdfPCell(new Phrase(item.isPagado() && item.getFechaPago() != null ? item.getFechaPago().format(dtf) : "-", fu.f7)) {{ setBorder(PdfPCell.NO_BORDER); }});

                    PdfPCell cellImp = new PdfPCell(new Phrase(fmt.format(item.getImporte()), fu.f7));
                    cellImp.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    cellImp.setBorder(PdfPCell.NO_BORDER);
                    table.addCell(cellImp);
                }
                doc.add(table);

                Paragraph pSub = new Paragraph(
                        "Facturado: " + concepto.getCantidadFacturado() + " - " + fmt.format(concepto.getTotalFacturado()) +
                                "     |     Cobrado: " + concepto.getCantidadCobrado() + " - " + fmt.format(concepto.getTotalCobrado()),
                        fu.f9B);
                pSub.setAlignment(Element.ALIGN_RIGHT);
                pSub.setSpacingBefore(3f);
                pSub.setSpacingAfter(10f);
                doc.add(pSub);
            }

            // PÁGINA FINAL
            doc.newPage();
            doc.add(new Paragraph("Facturación por Concepto / Rubro", fu.f14B));
            doc.add(new Paragraph("Filtro: " + datos.getFiltroDescripcion(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            doc.add(Chunk.NEWLINE);
            doc.add(new Chunk(new org.openpdf.text.pdf.draw.LineSeparator(0.5f, 100, null, Element.ALIGN_CENTER, -2)));

            Paragraph pFinal = new Paragraph(
                    "TOTAL FACTURADO\nCantidad: " + datos.getCantidadTotalFacturado() +
                            "   |   " + fmt.format(datos.getGranTotalFacturado()) +
                            "\n\nTOTAL COBRADO\nCantidad: " + datos.getCantidadTotalCobrado() +
                            "   |   " + fmt.format(datos.getGranTotalCobrado()),
                    fu.f12B);
            pFinal.setAlignment(Element.ALIGN_RIGHT);
            pFinal.setSpacingBefore(10f);
            doc.add(pFinal);

            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de Facturación por Concepto", e);
        }

        return out.toByteArray();
    }

    // 2. Método para generar el PDF tal cual la imagen
    public byte[] generarPdfFacturasPeriodo(String descripcion) {
        ReporteFacturaPeriodoDto datos = obtenerFacturasPeriodoEstructurada(descripcion);

        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = iniciarDocumentoPdf(out);
            FuentesReporte fu = crearFuentesReporte();
            Font fontNormal = fu.f9;
            Font fontBold = fu.f9B;
            Font fontTitulo = fu.f14B;
            Font fontEncabezado = fu.f12B;
            Font fontTotalGral = fu.f11B;

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

        try {
            Document doc = iniciarDocumentoPdf(out);
            FuentesReporte fu = crearFuentesReporte();

            // ENCABEZADO
            Paragraph pGen = new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion), fu.f8);
            pGen.setAlignment(Element.ALIGN_RIGHT);
            doc.add(pGen);

            Paragraph pTit = new Paragraph("Recaudación por Período", fu.f14B);
            pTit.setAlignment(Element.ALIGN_CENTER);
            doc.add(pTit);

            Paragraph pPer = new Paragraph("Período: " + periodo, FontFactory.getFont(FontFactory.HELVETICA, 10));
            pPer.setAlignment(Element.ALIGN_RIGHT);
            doc.add(pPer);

            // LISTADO
            for (RecaudacionEstablecimientoDto est : datos.getEstablecimientos()) {
                doc.add(new Paragraph("\n" + est.getNombre(), fu.f10B));

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
                        PdfPCell cell = new PdfPCell(new Phrase(h, fu.f8B));
                        cell.setBorder(PdfPCell.NO_BORDER);
                        table.addCell(cell);
                    }

                    // Datos
                    for (RecaudacionDetalleDto item : medio.getItems()) {
                        table.addCell(new PdfPCell(new Phrase(item.getFactura().toString(), fu.f7)) {{ setBorder(PdfPCell.NO_BORDER); }});
                        table.addCell(new PdfPCell(new Phrase(item.getPeriodo(), fu.f7)) {{ setBorder(PdfPCell.NO_BORDER); }});
                        table.addCell(new PdfPCell(new Phrase(item.getLegajo().toString(), fu.f7)) {{ setBorder(PdfPCell.NO_BORDER); }});
                        table.addCell(new PdfPCell(new Phrase(item.getNombre(), fu.f7)) {{ setBorder(PdfPCell.NO_BORDER); }});
                        table.addCell(new PdfPCell(new Phrase(item.getFecha() != null ? item.getFecha().format(dtf) : "", fu.f7)) {{ setBorder(PdfPCell.NO_BORDER); }});

                        PdfPCell cellImp = new PdfPCell(new Phrase(fmt.format(item.getPagado()), fu.f7));
                        cellImp.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        cellImp.setBorder(PdfPCell.NO_BORDER);
                        table.addCell(cellImp);
                    }
                    doc.add(table);

                    Paragraph pSub = new Paragraph(
                            "Cantidad de Pagos: " + medio.getCantidadPagos() + "   |   " + fmt.format(medio.getSubtotal()),
                            fu.f9B);
                    pSub.setAlignment(Element.ALIGN_RIGHT);
                    pSub.setSpacingBefore(3f);
                    doc.add(pSub);
                }

                int pagosEst = est.getMedios().stream().mapToInt(RecaudacionMedioDto::getCantidadPagos).sum();

                Paragraph pEstTotal = new Paragraph(
                        "TOTAL " + est.getNombre() + "   —   Cantidad de Pagos: " + pagosEst +
                                "   |   " + fmt.format(est.getTotalEstablecimiento()),
                        fu.f11B);
                pEstTotal.setAlignment(Element.ALIGN_RIGHT);
                pEstTotal.setSpacingBefore(4f);
                pEstTotal.setSpacingAfter(14f);
                doc.add(pEstTotal);
            }

            // PÁGINA FINAL
            doc.newPage();
            doc.add(new Paragraph("Recaudación por Período", fu.f14B));
            doc.add(new Paragraph("Período: " + periodo, FontFactory.getFont(FontFactory.HELVETICA, 10)));
            doc.add(Chunk.NEWLINE);

            doc.add(new Chunk(new org.openpdf.text.pdf.draw.LineSeparator(0.5f, 100, null, Element.ALIGN_CENTER, -2)));

            Paragraph pFinal = new Paragraph(
                    "TOTAL GENERAL DEL PERÍODO\nCantidad de Pagos: " + datos.getCantidadTotalPagos() +
                            "   |   " + fmt.format(datos.getGranTotal()),
                    fu.f12B);
            pFinal.setAlignment(Element.ALIGN_RIGHT);
            pFinal.setSpacingBefore(10f);
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

        try {
            Document document = iniciarDocumentoPdf(out);
            FuentesReporte fu = crearFuentesReporte();

            // CABECERA
            Paragraph pGen = new Paragraph("Generado el: " + LocalDateTime.now().format(dtfGeneracion), fu.f8);
            pGen.setAlignment(Element.ALIGN_RIGHT);
            document.add(pGen);

            Paragraph pTit = new Paragraph("Recaudación por Fechas", fu.f14B);
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
                    Paragraph pMedio = new Paragraph(medio.getNombre(), fu.f9B);
                    pMedio.setIndentationLeft(20);
                    document.add(pMedio);

                    // TABLA DE DETALLE (6 columnas)
                    PdfPTable table = new PdfPTable(new float[]{2, 2, 2, 5, 2, 2});
                    table.setWidthPercentage(100);
                    table.setSpacingBefore(5f);

                    // Cabeceras
                    String[] headers = {"Factura", "Período", "Legajo", "Apellido, Nombre", "Fecha", "Pagado"};
                    for (String h : headers) {
                        table.addCell(new PdfPCell(new Phrase(h, fu.f8B)) {{ setBorder(PdfPCell.BOTTOM); }});
                    }

                    // Filas
                    for (RecaudacionDetalleDto item : medio.getItems()) {
                        table.addCell(new PdfPCell(new Phrase(item.getFactura().toString(), fu.f8)) {{ setBorder(PdfPCell.NO_BORDER); }});
                        table.addCell(new PdfPCell(new Phrase(item.getPeriodo(), fu.f8)) {{ setBorder(PdfPCell.NO_BORDER); }});
                        table.addCell(new PdfPCell(new Phrase(item.getLegajo().toString(), fu.f8)) {{ setBorder(PdfPCell.NO_BORDER); }});
                        table.addCell(new PdfPCell(new Phrase(item.getNombre(), fu.f8)) {{ setBorder(PdfPCell.NO_BORDER); }});
                        table.addCell(new PdfPCell(new Phrase(item.getFecha().format(fmtFecha), fu.f8)) {{ setBorder(PdfPCell.NO_BORDER); }});

                        PdfPCell cellImp = new PdfPCell(new Phrase(fmtMoneda.format(item.getPagado()), fu.f8));
                        cellImp.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        cellImp.setBorder(PdfPCell.NO_BORDER);
                        table.addCell(cellImp);
                    }
                    document.add(table);

                    Paragraph pSub = new Paragraph("Cantidad de Pagos: " + medio.getCantidadPagos() + " - " + fmtMoneda.format(medio.getSubtotal()), fu.f9B);
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
            Paragraph pFinal = new Paragraph("\nCantidad de Pagos: " + datos.getCantidadTotalPagos() + " - " + fmtMoneda.format(datos.getGranTotal()), fu.f11B);
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

        try {
            Document document = iniciarDocumentoPdf(out);
            FuentesReporte fu = crearFuentesReporte();
            Font font8 = fu.f8;
            Font font8B = fu.f8B;
            Font font9B = fu.f9B;
            Font font11B = fu.f11B;
            Font font14B = fu.f14B;

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

    @Transactional
    public Factura registrarPagoFactura(PagoCargaDto dto) {
        // 1. Buscamos la factura por su número comercial (ej: 720) como se ve en tu pantalla
        Factura factura = facturaRepository.findByNroFactura(dto.getNroFactura())
                .orElseThrow(() -> new RuntimeException("Factura Nro " + dto.getNroFactura() + " no encontrada"));

        // 2. Insertamos de manera real la transacción para que crezca el ID correlativo en la base
        Transaccion nuevaTransaccion = transaccionService.add();

        // 3. Cambiamos el estado de la factura a Pagada (id_estado = 1)
        TipoEstado estadoPagado = new TipoEstado();
        estadoPagado.setEstadoId(1L); // 1 = Pagada / Cancelada en tu sistema
        factura.setTipoEstado(estadoPagado);

        // 4. Asentamos los importes y fechas de la recaudación
        factura.setFechaPago(dto.getFechaPago());
        factura.setImportePagado(dto.getImportePagado());

        // Restamos del adeudo el monto que pagó el alumno
        BigDecimal nuevoAdeudo = factura.getImporteAdeudado().subtract(dto.getImportePagado());
        factura.setImporteAdeudado(nuevoAdeudo.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : nuevoAdeudo);

        if (factura.getImporteAdeudado().compareTo(BigDecimal.ZERO) == 0) {
            factura.setFechaCancelacion(dto.getFechaPago());
        }

        // 5. Amarramos el ID de la transacción generada a la factura
        factura.setCajaMovimientoId(nuevaTransaccion.getTransaccionId());

        // 6. Asignamos el medio de pago si se seleccionó del combo
        if (dto.getTipoPagoId() != null) {
            TipoPago tp = new TipoPago();
            tp.setTipoId(dto.getTipoPagoId());
            factura.setTipoPago(tp);
        }

        return facturaRepository.save(factura);
    }

    /**
     * Anula el pago de una factura revirtiendo sus importes y devolviéndola al estado pendiente
     */
    @Transactional
    public Factura anularPagoFactura(Long nroFactura) {
        // 1. Buscamos la factura por su número comercial
        Factura factura = facturaRepository.findByNroFactura(nroFactura)
                .orElseThrow(() -> new RuntimeException("Factura Nro " + nroFactura + " no encontrada"));

        // 2. Validamos que la factura realmente esté pagada para poder anularla
        if (factura.getTipoEstado() == null || factura.getTipoEstado().getEstadoId() != 1L) {
            throw new RuntimeException("La factura Nro " + nroFactura + " no se encuentra en estado PAGADA.");
        }

        // 3. Revertimos los importes contables
        BigDecimal importeRevertido = factura.getImporteAdeudado().add(factura.getImportePagado());
        factura.setImporteAdeudado(importeRevertido);
        factura.setImportePagado(BigDecimal.ZERO);

        // 4. Limpiamos las fechas de control de caja y de cancelación
        factura.setFechaPago(null);
        factura.setFechaCancelacion(null);

        // ✅ CORRECCIÓN 1: Evita el error 'cajamovimiento_id' cannot be null
        factura.setCajaMovimientoId(0L);

        // 🌟 CORRECCIÓN 2: Evita el error 'tipo_id' cannot be null asignando el ID 0L (o el genérico de tu base)
        TipoPago tipoPagoPorDefecto = new TipoPago();
        tipoPagoPorDefecto.setTipoId(0L);
        factura.setTipoPago(tipoPagoPorDefecto);

        // 5. Devolvemos la factura al estado "No pagada" (id_estado = 2)
        TipoEstado estadoPendiente = new TipoEstado();
        estadoPendiente.setEstadoId(2L);
        factura.setTipoEstado(estadoPendiente);

        return facturaRepository.save(factura);
    }

    /**
     * 🌟 Vista previa para "Facturar por Curso": TODOS los alumnos del curso, indicando
     * si ya tienen una Factura para ese período (facturado = true, sin importar si está
     * pagada) o si todavía no (facturado = false), junto con lo que se les facturaría.
     */
    public List<PreviewFacturaCursoAlumnoDto> previewFacturaCurso(Long cursoId, Long periodoId) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado (id " + cursoId + ")"));

        Periodo periodo = periodoRepository.findById(periodoId)
                .orElseThrow(() -> new RuntimeException("Período no encontrado (id " + periodoId + ")"));

        List<Alumno> alumnos = alumnoRepository.findAllByCursoRelacionalId(curso.getCursoId(), curso.getDescripcion());

        List<PreviewFacturaCursoAlumnoDto> resultado = new ArrayList<>();
        for (Alumno alumno : alumnos) {
            resultado.add(construirPreviewAlumno(alumno, periodo));
        }
        return resultado;
    }

    /**
     * 🌟 Vista previa de "Factura por Alumno": mismo formato que la de curso, pero para
     * UN solo alumno (buscado por legajo o por nombre desde el frontend).
     */
    public PreviewFacturaCursoAlumnoDto previewFacturaAlumno(Long alumnoId, Long periodoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado (id " + alumnoId + ")"));

        Periodo periodo = periodoRepository.findById(periodoId)
                .orElseThrow(() -> new RuntimeException("Período no encontrado (id " + periodoId + ")"));

        return construirPreviewAlumno(alumno, periodo);
    }

    // 🌟 Lógica compartida: arma el preview (facturado/pendiente + detalle de conceptos)
    // de un alumno en un período. La usan tanto "Facturar por Curso" (por cada alumno del
    // curso) como "Factura por Alumno" (para uno solo).
    private PreviewFacturaCursoAlumnoDto construirPreviewAlumno(Alumno alumno, Periodo periodo) {
        Optional<Map<String, Object>> facturaExistente =
                facturaRepository.findFacturaConAlumnoPorPeriodo(alumno.getAlumnoId(), periodo.getDescripcion());

        // 🌟 TODOS los conceptos del alumno en este período (facturados y pendientes),
        // igual que muestra el ejecutable original
        List<ConceptoConEstadoProjection> todos =
                conceptoRepository.findTodosPorAlumnoYPeriodo(alumno.getAlumnoId(), periodo.getPeriodoId());

        boolean tienePendientes = todos.stream().anyMatch(c -> c.getFacturado() == null || c.getFacturado() == 0L);

        PreviewFacturaCursoAlumnoDto item = new PreviewFacturaCursoAlumnoDto();
        item.setLegajo(alumno.getAlumnoId());
        item.setNombreCompleto(alumno.getApellido() + ", " + alumno.getNombre());
        item.setFacturado(facturaExistente.isPresent());
        item.setTienePendientes(tienePendientes);

        if (facturaExistente.isPresent()) {
            Map<String, Object> f = facturaExistente.get();
            Object nro = f.get("nroFactura");
            Object importe = f.get("importeAdeudado");
            item.setNroFactura(nro != null ? ((Number) nro).longValue() : null);
            item.setImporteFactura(importe != null ? new BigDecimal(importe.toString()) : null);
        }

        item.setConceptos(todos.stream()
                .map(c -> {
                    boolean estaFacturado = c.getFacturado() != null && c.getFacturado() == 1L;
                    return new LineaDetalleDto(
                            estaFacturado ? c.getFechaEstado() : null,
                            c.getDescripcion(),
                            estaFacturado ? "Concepto FACTURADO" : "PENDIENTE DE FACTURAR",
                            c.getImporte(),
                            c.getFechaRegistro(),
                            periodo.getDescripcion()
                    );
                })
                .collect(Collectors.toList()));

        return item;
    }

    /**
     * 🌟 "Factura por Alumno": agrupa los conceptos pendientes de UN alumno en una
     * factura nueva, para el período y vencimiento indicados, sumando además un
     * recargo manual opcional (que solo se suma al total, no genera un concepto aparte).
     */
    @Transactional
    public FacturaCursoAlumnoResultadoDto facturarAlumno(Long alumnoId, Long periodoId, LocalDate fechaVencimiento, BigDecimal recargo) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado (id " + alumnoId + ")"));

        Periodo periodo = periodoRepository.findById(periodoId)
                .orElseThrow(() -> new RuntimeException("Período no encontrado (id " + periodoId + ")"));

        if (fechaVencimiento == null) {
            throw new RuntimeException("Debe indicar la fecha de vencimiento.");
        }

        List<ConceptoDetalleProjection> pendientes =
                conceptoRepository.findPendientesPorAlumnoYPeriodo(alumnoId, periodoId);

        if (pendientes.isEmpty()) {
            throw new RuntimeException("Este alumno no tiene conceptos pendientes de facturar para este período.");
        }

        BigDecimal recargoAplicado = recargo != null ? recargo : BigDecimal.ZERO;

        BigDecimal totalConceptos = pendientes.stream()
                .map(ConceptoDetalleProjection::getImporte)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = totalConceptos.add(recargoAplicado);

        long siguienteNroFactura = facturaRepository.findMaxNroFactura() + 1;
        LocalDate hoy = LocalDate.now();

        TipoEstado estadoNoPagada = new TipoEstado();
        estadoNoPagada.setEstadoId(2L); // 2 = Factura NO pagada

        Factura factura = new Factura();
        factura.setNroFactura(siguienteNroFactura);
        factura.setFechaEstado(hoy);
        factura.setPrimerVencimiento(fechaVencimiento);
        factura.setImporteAdeudado(total); // Incluye el recargo, sin línea de concepto propia
        factura.setImportePagado(BigDecimal.ZERO);
        factura.setTipoEstado(estadoNoPagada);
        factura.setPeriodo(periodo);
        factura.setCajaMovimientoId(0L);
        factura.setImpresa(0);
        factura.setPfBarras("");
        factura.setPfCodigo("");

        TipoPago tipoPagoPorDefecto = new TipoPago();
        tipoPagoPorDefecto.setTipoId(0L);
        factura.setTipoPago(tipoPagoPorDefecto);

        factura = facturaRepository.save(factura);

        facturaRepository.vincularAlumnoConFactura(alumnoId, factura.getFacturaId());
        conceptoRepository.marcarComoFacturados(alumnoId, periodoId, factura.getFacturaId());

        List<LineaDetalleDto> conceptos = pendientes.stream()
                .map(p -> new LineaDetalleDto(
                        hoy,
                        p.getDescripcion(),
                        "Concepto FACTURADO",
                        p.getImporte(),
                        p.getFechaRegistro(),
                        periodo.getDescripcion()
                ))
                .collect(Collectors.toList());

        FacturaCursoAlumnoResultadoDto resultado = new FacturaCursoAlumnoResultadoDto();
        resultado.setLegajo(alumnoId);
        resultado.setNombreCompleto(alumno.getApellido() + ", " + alumno.getNombre());
        resultado.setNroFactura(factura.getNroFactura());
        resultado.setImporteTotal(total);
        resultado.setConceptos(conceptos);
        return resultado;
    }

    /**
     * 🌟 PDF de "Facturar por Curso": una página por cada alumno que YA tiene una
     * Factura para este período (los pendientes de facturar no tienen nada que imprimir
     * todavía). Donde va el código de barras se deja un placeholder de texto — la
     * generación real de código de barras queda pendiente para más adelante.
     */
    // 🌟 Arma las 2 copias de UNA factura (vencimiento/fecha/código de barras/conceptos/
    // otras deudas + las dos llamadas a dibujarUnaCopiaFactura con su separador punteado
    // en el medio). Compartido entre "Factura por Curso" (una vez por alumno) y
    // "Factura por Alumno" (una sola vez).
    private void escribirFacturaConDosCopias(
            Document document, PdfWriter writer, PreviewFacturaCursoAlumnoDto item, Curso curso, Periodo periodo,
            String establecimiento, String direccion, String cicloNombre, String dni,
            NumberFormat formatoMoneda, DateTimeFormatter dtfGeneracion, DateTimeFormatter dtfTablas
    ) throws Exception {
        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
        Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font fontPlaceholder = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

        // Vencimiento, fecha de emisión y código de barras real de ESA factura (si existe)
        Optional<Factura> facturaEntidad = facturaRepository.findByNroFactura(item.getNroFactura());
        LocalDate vencimiento = facturaEntidad.map(Factura::getPrimerVencimiento).orElse(null);
        LocalDate fechaFactura = facturaEntidad.map(Factura::getFechaEstado).orElse(LocalDate.now());
        String codigoBarras = facturaEntidad.map(Factura::getPfBarras)
                .filter(s -> s != null && !s.isBlank())
                .orElse(null);

        // Se calculan UNA sola vez por alumno y se reusan en las dos copias impresas
        List<ConceptoConEstadoProjection> conceptosCompletos =
                conceptoRepository.findTodosPorAlumnoYPeriodo(item.getLegajo(), periodo.getPeriodoId());

        List<Factura> otrasFacturasPendientes = facturaRepository.findByAlumnoId(item.getLegajo()).stream()
                .filter(f -> f.getTipoEstado() != null
                        && f.getTipoEstado().getEstadoId() != null
                        && f.getTipoEstado().getEstadoId() != 1L  // 1 = Pagada
                        && f.getTipoEstado().getEstadoId() != 6L) // 6 = Factura Anulada
                .filter(f -> !f.getNroFactura().equals(item.getNroFactura())) // no repetir esta misma
                .sorted(Comparator.comparing(Factura::getPrimerVencimiento,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        // --- DOS COPIAS DE LA MISMA FACTURA EN LA MISMA HOJA (talón), como el sistema anterior ---
        // La primera copia es completa (conceptos + resumen de deuda);
        // la segunda es resumida (solo encabezado + datos + total + código de barras)
        dibujarUnaCopiaFactura(document, writer, item, curso, periodo, establecimiento, direccion,
                cicloNombre, vencimiento, fechaFactura, codigoBarras, dni, conceptosCompletos, otrasFacturasPendientes,
                formatoMoneda, dtfGeneracion, dtfTablas, fontTitulo, fontSubtitulo, fontBold, fontNormal, fontPlaceholder,
                false);

        document.add(Chunk.NEWLINE);
        document.add(new Chunk(new org.openpdf.text.pdf.draw.DottedLineSeparator()));
        document.add(Chunk.NEWLINE);

        dibujarUnaCopiaFactura(document, writer, item, curso, periodo, establecimiento, direccion,
                cicloNombre, vencimiento, fechaFactura, codigoBarras, dni, conceptosCompletos, otrasFacturasPendientes,
                formatoMoneda, dtfGeneracion, dtfTablas, fontTitulo, fontSubtitulo, fontBold, fontNormal, fontPlaceholder,
                true);
    }

    public byte[] generarPdfFacturaCurso(Long cursoId, Long periodoId) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado (id " + cursoId + ")"));

        Periodo periodo = periodoRepository.findById(periodoId)
                .orElseThrow(() -> new RuntimeException("Período no encontrado (id " + periodoId + ")"));

        List<PreviewFacturaCursoAlumnoDto> preview = previewFacturaCurso(cursoId, periodoId);
        List<PreviewFacturaCursoAlumnoDto> facturados = preview.stream()
                .filter(PreviewFacturaCursoAlumnoDto::isFacturado)
                .collect(Collectors.toList());

        if (facturados.isEmpty()) {
            throw new RuntimeException("Ningún alumno de este curso tiene una factura generada para este período todavía.");
        }

        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");
        DateTimeFormatter dtfTablas = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String establecimiento = curso.getEstablecimiento() != null ? curso.getEstablecimiento().getNombre() : "";
        String direccion = curso.getEstablecimiento() != null ? curso.getEstablecimiento().getDireccion() : "";
        String cicloNombre = curso.getCiclo() != null ? curso.getCiclo().getNombre() : "";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 20, 20, 20);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            for (int i = 0; i < facturados.size(); i++) {
                PreviewFacturaCursoAlumnoDto item = facturados.get(i);

                // DNI del alumno (no viene en el preview, lo buscamos puntualmente)
                String dni = alumnoRepository.findById(item.getLegajo())
                        .map(Alumno::getNroDocumento)
                        .orElse("");

                escribirFacturaConDosCopias(document, writer, item, curso, periodo, establecimiento, direccion,
                        cicloNombre, dni, formatoMoneda, dtfGeneracion, dtfTablas);

                if (i < facturados.size() - 1) {
                    document.newPage();
                }
            }

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de facturación por curso", e);
        }

        return out.toByteArray();
    }

    /**
     * Dibuja UNA copia completa de la factura de un alumno (encabezado, datos, tabla de
     * conceptos, resumen de deuda y código de barras) en la posición actual del documento.
     * Se usa dos veces por alumno para imprimir dos copias en la misma hoja (talón), como
     * hacía el sistema anterior.
     */
    private void dibujarUnaCopiaFactura(
            Document document, PdfWriter writer, PreviewFacturaCursoAlumnoDto item, Curso curso,
            Periodo periodo, String establecimiento, String direccion, String cicloNombre,
            LocalDate vencimiento, LocalDate fechaFactura, String codigoBarras, String dni,
            List<ConceptoConEstadoProjection> conceptosCompletos, List<Factura> otrasFacturasPendientes,
            NumberFormat formatoMoneda, DateTimeFormatter dtfGeneracion, DateTimeFormatter dtfTablas,
            Font fontTitulo, Font fontSubtitulo, Font fontBold, Font fontNormal, Font fontPlaceholder,
            boolean resumido
    ) throws Exception {
        // --- ENCABEZADO ---
        pdfLogoService.colocarArribaDerecha(document, writer, establecimiento);

        Paragraph pEstablecimiento = new Paragraph(establecimiento, fontTitulo);
        document.add(pEstablecimiento);
        if (direccion != null && !direccion.isBlank()) {
            document.add(new Paragraph(direccion, fontSubtitulo));
        }
        document.add(Chunk.NEWLINE);

        PdfPTable tablaEncabezado = new PdfPTable(2);
        tablaEncabezado.setWidthPercentage(100);
        try { tablaEncabezado.setWidths(new float[]{6f, 3f}); } catch (Exception ignored) {}

        PdfPCell celdaIzq = new PdfPCell();
        celdaIzq.setBorder(Rectangle.NO_BORDER);
        celdaIzq.addElement(new Paragraph("Alumno", fontBold));
        celdaIzq.addElement(new Paragraph(
                "(" + dni + ") " + item.getNombreCompleto() + " - " + item.getLegajo(), fontNormal));
        celdaIzq.addElement(new Paragraph("Curso", fontBold));
        celdaIzq.addElement(new Paragraph(curso.getDescripcion(), fontNormal));
        tablaEncabezado.addCell(celdaIzq);

        PdfPCell celdaDer = new PdfPCell();
        celdaDer.setBorder(Rectangle.NO_BORDER);
        celdaDer.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph pFecha = new Paragraph("Fecha: " + fechaFactura.format(dtfGeneracion), fontNormal);
        pFecha.setAlignment(Element.ALIGN_RIGHT);
        celdaDer.addElement(pFecha);
        Paragraph pNumero = new Paragraph("Número: " + item.getNroFactura(), fontBold);
        pNumero.setAlignment(Element.ALIGN_RIGHT);
        celdaDer.addElement(pNumero);
        tablaEncabezado.addCell(celdaDer);
        document.add(tablaEncabezado);
        document.add(Chunk.NEWLINE);

        // Período / Ciclo / Vencimiento (+ total debajo del vencimiento, como en el original)
        PdfPTable tablaDatos = new PdfPTable(3);
        tablaDatos.setWidthPercentage(100);
        addCell(tablaDatos, "Período", fontBold);
        addCell(tablaDatos, "Ciclo", fontBold);

        PdfPCell celdaVencHeader = new PdfPCell(new Phrase("Vencimiento", fontBold));
        celdaVencHeader.setBorder(Rectangle.NO_BORDER);
        celdaVencHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tablaDatos.addCell(celdaVencHeader);

        addCell(tablaDatos, periodo.getDescripcion(), fontNormal);
        addCell(tablaDatos, cicloNombre, fontNormal);

        PdfPCell celdaVencValor = new PdfPCell();
        celdaVencValor.setBorder(Rectangle.NO_BORDER);
        celdaVencValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph pVencimiento = new Paragraph(vencimiento != null ? vencimiento.format(dtfTablas) : "-", fontNormal);
        pVencimiento.setAlignment(Element.ALIGN_RIGHT);
        Paragraph pTotalArriba = new Paragraph(
                item.getImporteFactura() != null ? formatoMoneda.format(item.getImporteFactura()) : "$ 0,00",
                fontBold);
        pTotalArriba.setAlignment(Element.ALIGN_RIGHT);
        celdaVencValor.addElement(pVencimiento);
        celdaVencValor.addElement(pTotalArriba);
        tablaDatos.addCell(celdaVencValor);

        document.add(tablaDatos);
        document.add(Chunk.NEWLINE);

        if (!resumido) {
            // --- CONCEPTOS FACTURADOS (con código, como en el original) ---
            PdfPTable tablaConceptos = new PdfPTable(new float[]{1.2f, 6f, 2f});
            tablaConceptos.setWidthPercentage(100);

            PdfPCell headerCodigo = new PdfPCell(new Phrase("Código", fontBold));
            headerCodigo.setBorder(Rectangle.BOTTOM);
            tablaConceptos.addCell(headerCodigo);

            PdfPCell headerConcepto = new PdfPCell(new Phrase("Concepto", fontBold));
            headerConcepto.setBorder(Rectangle.BOTTOM);
            tablaConceptos.addCell(headerConcepto);

            PdfPCell headerImporte = new PdfPCell(new Phrase("Subtotal", fontBold));
            headerImporte.setBorder(Rectangle.BOTTOM);
            headerImporte.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tablaConceptos.addCell(headerImporte);

            for (ConceptoConEstadoProjection c : conceptosCompletos) {
                if (c.getFacturado() == null || c.getFacturado() != 1L) {
                    continue; // Solo los conceptos que quedaron facturados en ESTA factura
                }
                addCell(tablaConceptos, c.getIdConcepto() != null ? c.getIdConcepto().toString() : "", fontNormal);
                addCell(tablaConceptos, c.getDescripcion(), fontNormal);
                PdfPCell celdaImporte = new PdfPCell(new Phrase(
                        c.getImporte() != null ? formatoMoneda.format(c.getImporte()) : "$ 0,00", fontNormal));
                celdaImporte.setBorder(Rectangle.NO_BORDER);
                celdaImporte.setHorizontalAlignment(Element.ALIGN_RIGHT);
                tablaConceptos.addCell(celdaImporte);
            }
            document.add(tablaConceptos);

            // --- RESUMEN DE DEUDA (otras facturas de este alumno, pendientes de pago) ---
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);
            Paragraph pResumenTitulo = new Paragraph(
                    "RESUMEN DE DEUDA AL: " + fechaFactura.format(dtfTablas), fontBold);
            pResumenTitulo.setSpacingBefore(40f);
            pResumenTitulo.setAlignment(Element.ALIGN_CENTER);
            document.add(pResumenTitulo);

            PdfPTable tablaResumen = new PdfPTable(new float[]{2f, 3f, 2.5f, 2.5f});
            tablaResumen.setWidthPercentage(100);
            tablaResumen.setSpacingBefore(5f);
            addCell(tablaResumen, "Factura", fontBold);
            addCell(tablaResumen, "Periodo", fontBold);
            addCell(tablaResumen, "Vencimiento", fontBold);
            PdfPCell headerImporteResumen = new PdfPCell(new Phrase("Importe", fontBold));
            headerImporteResumen.setBorder(Rectangle.NO_BORDER);
            headerImporteResumen.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tablaResumen.addCell(headerImporteResumen);

            if (otrasFacturasPendientes.isEmpty()) {
                PdfPCell vacio = new PdfPCell(new Phrase("El alumno no registra otras deudas pendientes.", fontNormal));
                vacio.setColspan(4);
                vacio.setBorder(Rectangle.NO_BORDER);
                vacio.setHorizontalAlignment(Element.ALIGN_CENTER);
                tablaResumen.addCell(vacio);
            } else {
                for (Factura f : otrasFacturasPendientes) {
                    addCell(tablaResumen, f.getNroFactura().toString(), fontNormal);
                    addCell(tablaResumen, f.getPeriodo() != null ? f.getPeriodo().getDescripcion() : "", fontNormal);
                    addCell(tablaResumen, f.getPrimerVencimiento() != null ? f.getPrimerVencimiento().format(dtfTablas) : "-", fontNormal);
                    PdfPCell celdaImp = new PdfPCell(new Phrase(
                            f.getImporteAdeudado() != null ? formatoMoneda.format(f.getImporteAdeudado()) : "$ 0,00", fontNormal));
                    celdaImp.setBorder(Rectangle.NO_BORDER);
                    celdaImp.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    tablaResumen.addCell(celdaImp);
                }
            }
            document.add(tablaResumen);
        }

        // --- CÓDIGO DE BARRAS ---
        // Si la factura YA tiene un valor cargado en pf_barras (por ejemplo, facturas
        // viejas migradas del sistema anterior), lo dibujamos como código real (Code 39).
        // Si no se puede interpretar como Code 39 válido, mostramos el texto crudo.
        // Si no hay nada cargado (caso de las facturas nuevas creadas por este sistema),
        // dejamos el placeholder de siempre.
        document.add(Chunk.NEWLINE);

        if (codigoBarras != null) {
            try {
                Barcode39 barcode39 = new Barcode39();
                barcode39.setCode(codigoBarras);
                barcode39.setStartStopText(false);
                Image imagenBarcode = barcode39.createImageWithBarcode(writer.getDirectContent(), null, null);
                imagenBarcode.setAlignment(Element.ALIGN_CENTER);
                imagenBarcode.setSpacingBefore(10f);
                document.add(imagenBarcode);
            } catch (Exception exBarcode) {
                // El valor guardado no es un Code 39 válido: mostramos el texto tal cual
                Paragraph pTextoCrudo = new Paragraph(codigoBarras, fontNormal);
                pTextoCrudo.setAlignment(Element.ALIGN_CENTER);
                pTextoCrudo.setSpacingBefore(10f);
                document.add(pTextoCrudo);
            }
        } else {
            Paragraph pBarrasLabel = new Paragraph("[ Aquí va el código de barras ]", fontPlaceholder);
            pBarrasLabel.setAlignment(Element.ALIGN_CENTER);
            pBarrasLabel.setSpacingBefore(20f);
            document.add(pBarrasLabel);

            PdfPTable cajaBarras = new PdfPTable(1);
            cajaBarras.setWidthPercentage(60);
            cajaBarras.setHorizontalAlignment(Element.ALIGN_CENTER);
            PdfPCell celdaBarras = new PdfPCell(new Phrase(" ", fontNormal));
            celdaBarras.setFixedHeight(40f);
            celdaBarras.setBorder(Rectangle.BOX);
            cajaBarras.addCell(celdaBarras);
            document.add(cajaBarras);
        }

    }

    /**
     * 🌟 PDF de "Factura por Alumno": misma plantilla que "Factura por Curso" (dos
     * copias, resumen de deuda, etc.) pero para un solo alumno/período.
     */
    public byte[] generarPdfFacturaAlumno(Long alumnoId, Long periodoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado (id " + alumnoId + ")"));

        Periodo periodo = periodoRepository.findById(periodoId)
                .orElseThrow(() -> new RuntimeException("Período no encontrado (id " + periodoId + ")"));

        PreviewFacturaCursoAlumnoDto item = construirPreviewAlumno(alumno, periodo);
        if (!item.isFacturado()) {
            throw new RuntimeException("Este alumno no tiene una factura generada para este período todavía.");
        }

        // El curso actual del alumno es solo para el encabezado (nombre/dirección del
        // establecimiento y la línea "Curso"); si no se puede resolver, se deja en blanco.
        Curso curso = cursoRepository.findCursoActualDeAlumno(alumnoId).orElse(null);
        String establecimiento = (curso != null && curso.getEstablecimiento() != null) ? curso.getEstablecimiento().getNombre() : "";
        String direccion = (curso != null && curso.getEstablecimiento() != null) ? curso.getEstablecimiento().getDireccion() : "";
        String cicloNombre = (curso != null && curso.getCiclo() != null)
                ? curso.getCiclo().getNombre()
                : (periodo.getCiclo() != null ? periodo.getCiclo().getNombre() : "");

        if (curso == null) {
            curso = new Curso();
            curso.setDescripcion("-");
        }

        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        DateTimeFormatter dtfGeneracion = DateTimeFormatter.ofPattern("d/M/yyyy");
        DateTimeFormatter dtfTablas = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 20, 20, 20);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            String dni = alumno.getNroDocumento();

            escribirFacturaConDosCopias(document, writer, item, curso, periodo, establecimiento, direccion,
                    cicloNombre, dni, formatoMoneda, dtfGeneracion, dtfTablas);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de la factura del alumno", e);
        }

        return out.toByteArray();
    }

    public Map<String, Object> buscarFacturaParaPago(Long alumnoId, String periodoNombre) {
        return facturaRepository.findFacturaConAlumnoPorPeriodo(alumnoId, periodoNombre.trim())
                .orElseThrow(() -> new RuntimeException("No se encontró factura pendiente para el período " + periodoNombre));
    }

    /**
     * 🌟 "Facturar por Curso": agrupa, para cada alumno del curso, todos los conceptos
     * (novedades) todavía no facturados de ese período en UNA factura nueva, y los marca
     * como facturados. Alumnos sin conceptos pendientes se omiten (no se les genera nada).
     */
    @Transactional
    public List<FacturaCursoAlumnoResultadoDto> facturarCurso(FacturarCursoRequestDto dto) {
        Curso curso = cursoRepository.findById(dto.getCursoId())
                .orElseThrow(() -> new RuntimeException("Curso no encontrado (id " + dto.getCursoId() + ")"));

        Periodo periodo = periodoRepository.findById(dto.getPeriodoId())
                .orElseThrow(() -> new RuntimeException("Período no encontrado (id " + dto.getPeriodoId() + ")"));

        if (dto.getFechaVencimiento() == null) {
            throw new RuntimeException("Debe indicar la fecha de vencimiento.");
        }

        // Alumnos del curso (misma fuente que usa la ficha del curso: relación real + texto de respaldo)
        List<Alumno> alumnos = alumnoRepository.findAllByCursoRelacionalId(curso.getCursoId(), curso.getDescripcion());

        List<FacturaCursoAlumnoResultadoDto> resultado = new ArrayList<>();
        LocalDate hoy = LocalDate.now();

        // Numeración correlativa para este lote, arrancando desde el próximo número libre
        long siguienteNroFactura = facturaRepository.findMaxNroFactura() + 1;

        for (Alumno alumno : alumnos) {
            List<ConceptoDetalleProjection> pendientes =
                    conceptoRepository.findPendientesPorAlumnoYPeriodo(alumno.getAlumnoId(), periodo.getPeriodoId());

            if (pendientes.isEmpty()) {
                continue; // Este alumno no tiene novedades cargadas para este período: no se le factura nada
            }

            BigDecimal total = pendientes.stream()
                    .map(ConceptoDetalleProjection::getImporte)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            TipoEstado estadoNoPagada = new TipoEstado();
            estadoNoPagada.setEstadoId(2L); // 2 = Factura NO pagada

            Factura factura = new Factura();
            factura.setNroFactura(siguienteNroFactura++);
            factura.setFechaEstado(hoy);
            factura.setPrimerVencimiento(dto.getFechaVencimiento());
            factura.setImporteAdeudado(total);
            factura.setImportePagado(BigDecimal.ZERO);
            factura.setTipoEstado(estadoNoPagada);
            factura.setPeriodo(periodo);
            factura.setCajaMovimientoId(0L);
            factura.setImpresa(0); // Columna NOT NULL en la tabla factura
            factura.setPfBarras(""); // Columna NOT NULL
            factura.setPfCodigo(""); // Columna NOT NULL (misma familia que pf_barras)

            TipoPago tipoPagoPorDefecto = new TipoPago();
            tipoPagoPorDefecto.setTipoId(0L); // Columna tipo_id también es NOT NULL
            factura.setTipoPago(tipoPagoPorDefecto);

            factura = facturaRepository.save(factura);

            facturaRepository.vincularAlumnoConFactura(alumno.getAlumnoId(), factura.getFacturaId());
            conceptoRepository.marcarComoFacturados(alumno.getAlumnoId(), periodo.getPeriodoId(), factura.getFacturaId());

            List<LineaDetalleDto> conceptos = pendientes.stream()
                    .map(p -> new LineaDetalleDto(
                            hoy,
                            p.getDescripcion(),
                            "Concepto FACTURADO",
                            p.getImporte(),
                            p.getFechaRegistro(),
                            periodo.getDescripcion()
                    ))
                    .collect(Collectors.toList());

            FacturaCursoAlumnoResultadoDto item = new FacturaCursoAlumnoResultadoDto();
            item.setLegajo(alumno.getAlumnoId());
            item.setNombreCompleto(alumno.getApellido() + ", " + alumno.getNombre());
            item.setNroFactura(factura.getNroFactura());
            item.setImporteTotal(total);
            item.setConceptos(conceptos);
            resultado.add(item);
        }

        return resultado;
    }

    /**
     * 🌟 "Anular Factura": anula la factura COMPLETA (id_estado = 6, "Factura Anulada"),
     * a diferencia de anularPagoFactura que solo revierte un pago. Solo se permite si la
     * factura NO está pagada — si ya está pagada, primero hay que anular el pago.
     */
    @Transactional
    public Factura anularFactura(Long nroFactura) {
        Factura factura = facturaRepository.findByNroFactura(nroFactura)
                .orElseThrow(() -> new RuntimeException("Factura Nro " + nroFactura + " no encontrada"));

        Long estadoActual = factura.getTipoEstado() != null ? factura.getTipoEstado().getEstadoId() : null;

        if (estadoActual != null && estadoActual == 1L) {
            throw new RuntimeException(
                    "La factura Nro " + nroFactura + " está pagada. Primero hay que anular el pago antes de anular la factura.");
        }

        if (estadoActual != null && estadoActual == 6L) {
            throw new RuntimeException("La factura Nro " + nroFactura + " ya está anulada.");
        }

        TipoEstado estadoAnulada = new TipoEstado();
        estadoAnulada.setEstadoId(6L); // 6 = Factura Anulada
        factura.setTipoEstado(estadoAnulada);

        Factura facturaAnulada = facturaRepository.save(factura);

        // 🌟 Los conceptos que estaban en esta factura vuelven a quedar pendientes,
        // para poder agruparlos en una factura nueva junto con otras cosas si hace falta
        conceptoRepository.liberarConceptosDeFactura(factura.getFacturaId());

        return facturaAnulada;
    }

}