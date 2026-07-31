package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.dto.*;
import ar.com.sauce.colegio.rest.model.Factura;
import ar.com.sauce.colegio.rest.service.FacturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/facturacion")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class FacturaController {

    private final FacturaService facturaService;

    @GetMapping("/historia/{alumnoId}")
    public ResponseEntity<HistoriaFacturacionDto> getHistoria(@PathVariable Long alumnoId) {
        return ResponseEntity.ok(facturaService.obtenerHistoriaPorAlumno(alumnoId));
    }

    @GetMapping("/detalle/{nroFactura}") // 1. Cambiamos la variable en la ruta para que sea semántica
    public ResponseEntity<List<LineaDetalleDto>> getDetalle(@PathVariable Long nroFactura) {
        // 2. Ahora coincide exactamente con el @PathVariable y con lo que espera tu Service
        return ResponseEntity.ok(facturaService.obtenerDetalleDeFactura(nroFactura));
    }

    @GetMapping("/alumno/{alumnoId}/deuda-individual")
    public ResponseEntity<DeudaIndividualResponseDto> getDeudaIndividual(@PathVariable Long alumnoId) {
        return ResponseEntity.ok(facturaService.obtenerDeudaIndividualConTotal(alumnoId));
    }

    @GetMapping("/alumno/{alumnoId}/deuda-individual-pdf")
    public ResponseEntity<byte[]> descargarPdfDeudaIndividual(@PathVariable Long alumnoId) {
        // 👈 El service ahora solo necesita el alumnoId
        byte[] pdfContents = facturaService.generarPdfDeudaIndividual(alumnoId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add("Content-Disposition", "inline; filename=deuda_individual_" + alumnoId + ".pdf");

        return new ResponseEntity<>(pdfContents, headers, HttpStatus.OK);
    }

    @GetMapping("/recaudacion-diaria")
    public ResponseEntity<ReporteRecaudacionDto> getRecaudacion(
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate fecha) {

        ReporteRecaudacionDto reporte = facturaService.obtenerRecaudacionEstructurada(fecha);

        return ResponseEntity.ok(reporte);
    }

    @GetMapping("/recaudacion-diaria-pdf")
    public ResponseEntity<byte[]> descargarPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        byte[] pdfContents = facturaService.generarPdfRecaudacion(fecha);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        // 'inline' hace que el navegador intente abrirlo. 'attachment' obligaría a la descarga inmediata.
        headers.add("Content-Disposition", "inline; filename=recaudacion_" + fecha + ".pdf");

        return new ResponseEntity<>(pdfContents, headers, HttpStatus.OK);
    }

    @GetMapping("/facturas-periodo")
    public ResponseEntity<ReporteFacturaPeriodoDto> getFacturasPeriodo(@RequestParam String periodo) {
        // Retorna el JSON estructurado con establecimientos, items y totales
        ReporteFacturaPeriodoDto reporte = facturaService.obtenerFacturasPeriodoEstructurada(periodo);
        return ResponseEntity.ok(reporte);
    }

    @GetMapping("/facturas-periodo-pdf")
    public ResponseEntity<byte[]> descargarPdfFacturasPeriodo(@RequestParam String periodo) {
        byte[] pdfContents = facturaService.generarPdfFacturasPeriodo(periodo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add("Content-Disposition", "inline; filename=facturas_periodo_" + periodo + ".pdf");

        return new ResponseEntity<>(pdfContents, headers, HttpStatus.OK);
    }

    @GetMapping("/recaudacion-periodo")
    public ResponseEntity<ReporteRecaudacionDto> getRecaudacionPeriodo(@RequestParam String periodo) {
        return ResponseEntity.ok(facturaService.obtenerRecaudacionPeriodoCompleta(periodo));
    }

    @GetMapping("/recaudacion-periodo-pdf")
    public ResponseEntity<byte[]> descargarPdfRecaudacionPeriodo(@RequestParam String periodo) {
        byte[] pdfContents = facturaService.generarPdfRecaudacionPeriodoFinal(periodo);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add("Content-Disposition", "inline; filename=recaudacion_periodo_" + periodo + ".pdf");
        return new ResponseEntity<>(pdfContents, headers, HttpStatus.OK);
    }

    @GetMapping("/recaudacion-fechas")
    public ResponseEntity<ReporteRecaudacionDto> getRecaudacionFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(facturaService.obtenerRecaudacionPorFechas(desde, hasta));
    }

    @GetMapping("/recaudacion-fechas-pdf")
    public ResponseEntity<byte[]> descargarPdfRecaudacionFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        // Aquí llamarías a tu método de generación de PDF pasando el DTO
        // que ya contiene la 'cantidadTotalPagos'.
        byte[] pdfContents = facturaService.generarPdfRecaudacionPorFechas(desde, hasta);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add("Content-Disposition", "inline; filename=recaudacion_fechas.pdf");
        return new ResponseEntity<>(pdfContents, headers, HttpStatus.OK);
    }

    @GetMapping("/reportes/deuda-general")
    public ResponseEntity<List<DeudaGeneralDto>> getDeudaGeneral() {
        return ResponseEntity.ok(facturaService.obtenerDeudaGeneral());
    }

    @GetMapping("/reportes/deuda-general-pdf")
    public ResponseEntity<byte[]> descargarPdfDeudaGeneral() {
        byte[] pdfContents = facturaService.generarPdfDeudaGeneral();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        // Cambiar a "attachment" si preferís que se descargue directo en vez de abrirse en el navegador
        headers.add("Content-Disposition", "inline; filename=deuda_general.pdf");

        return new ResponseEntity<>(pdfContents, headers, HttpStatus.OK);
    }

    @PostMapping("/registrar-pago")
    public ResponseEntity<?> registrarPago(@RequestBody PagoCargaDto dto) {
        try {
            Factura facturaPagada = facturaService.registrarPagoFactura(dto);
            return new ResponseEntity<>(facturaPagada, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al procesar el pago: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/anular-pago/{nroFactura}")
    public ResponseEntity<?> anularPago(@PathVariable Long nroFactura) {
        try {
            Factura facturaRevertida = facturaService.anularPagoFactura(nroFactura);
            return new ResponseEntity<>(facturaRevertida, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al anular el pago: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/buscar-para-pago")
    public ResponseEntity<?> buscarParaPago(@RequestParam Long alumnoId, @RequestParam String periodo) {
        try {
            Map<String, Object> facturaData = facturaService.buscarFacturaParaPago(alumnoId, periodo);
            return ResponseEntity.ok(facturaData);
        } catch (Exception e) {
            // 🌟 DEVOLVEMOS UN 200 OK CON EL CUERPO VACÍO (NULL) DE FORMA FLUIDA
            return ResponseEntity.ok().build();
        }
    }

    // 🌟 Vista previa de "Factura por Alumno": mismo formato que la de curso, para un solo alumno
    @GetMapping("/preview-alumno")
    public ResponseEntity<?> previewFacturaAlumno(@RequestParam Long alumnoId, @RequestParam Long periodoId) {
        try {
            PreviewFacturaCursoAlumnoDto resultado = facturaService.previewFacturaAlumno(alumnoId, periodoId);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al obtener la vista previa: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🌟 "Factura por Alumno": agrupa los conceptos pendientes de un alumno en una factura
    // nueva, sumando además un recargo manual opcional (solo se suma al total)
    @PostMapping("/facturar-alumno")
    public ResponseEntity<?> facturarAlumno(@RequestBody FacturarAlumnoRequestDto dto) {
        try {
            FacturaCursoAlumnoResultadoDto resultado = facturaService.facturarAlumno(
                    dto.getAlumnoId(), dto.getPeriodoId(), dto.getFechaVencimiento(), dto.getRecargo());
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al facturar el alumno: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🌟 PDF de "Factura por Alumno" (dos copias, misma plantilla que "Factura por Curso")
    @GetMapping("/imprimir-alumno")
    public ResponseEntity<?> imprimirFacturaAlumno(@RequestParam Long alumnoId, @RequestParam Long periodoId) {
        try {
            byte[] pdfContents = facturaService.generarPdfFacturaAlumno(alumnoId, periodoId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add("Content-Disposition", "inline; filename=factura_alumno_" + alumnoId + "_" + periodoId + ".pdf");

            return new ResponseEntity<>(pdfContents, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al generar el PDF: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🌟 Vista previa: todos los alumnos del curso, marcados como facturado/pendiente
    @GetMapping("/preview-curso")
    public ResponseEntity<?> previewFacturaCurso(@RequestParam Long cursoId, @RequestParam Long periodoId) {
        try {
            List<PreviewFacturaCursoAlumnoDto> resultado = facturaService.previewFacturaCurso(cursoId, periodoId);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al obtener la vista previa: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🌟 PDF de "Facturar por Curso": una página por cada alumno ya facturado en el período
    @GetMapping("/imprimir-curso")
    public ResponseEntity<?> imprimirFacturaCurso(@RequestParam Long cursoId, @RequestParam Long periodoId) {
        try {
            byte[] pdfContents = facturaService.generarPdfFacturaCurso(cursoId, periodoId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add("Content-Disposition", "inline; filename=facturas_curso_" + cursoId + "_" + periodoId + ".pdf");

            return new ResponseEntity<>(pdfContents, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al generar el PDF: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🌟 "Facturar por Curso": agrupa las novedades pendientes de cada alumno del curso
    // en una factura nueva por alumno, para el período y vencimiento indicados.
    @PostMapping("/facturar-curso")
    public ResponseEntity<?> facturarCurso(@RequestBody FacturarCursoRequestDto dto) {
        try {
            List<FacturaCursoAlumnoResultadoDto> resultado = facturaService.facturarCurso(dto);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al facturar el curso: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🌟 Anula la factura completa (id_estado = 6). Solo si NO está pagada.
    @PostMapping("/anular-factura/{nroFactura}")
    public ResponseEntity<?> anularFactura(@PathVariable Long nroFactura) {
        try {
            Factura facturaAnulada = facturaService.anularFactura(nroFactura);
            return new ResponseEntity<>(facturaAnulada, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al anular la factura: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🌟 Facturación por Concepto/Rubro — filtrado por Período
    @GetMapping("/concepto/periodo")
    public ResponseEntity<?> getFacturacionPorConceptoYPeriodo(@RequestParam String periodo) {
        try {
            return ResponseEntity.ok(facturaService.obtenerFacturacionPorConceptoYPeriodo(periodo));
        } catch (Exception e) {
            return new ResponseEntity<>("Error al obtener la facturación por concepto: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/concepto/periodo-pdf")
    public ResponseEntity<?> descargarPdfFacturacionPorConceptoYPeriodo(@RequestParam String periodo) {
        try {
            ReporteFacturacionConceptoDto datos = facturaService.obtenerFacturacionPorConceptoYPeriodo(periodo);
            byte[] pdfContents = facturaService.generarPdfFacturacionConcepto(datos);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add("Content-Disposition", "inline; filename=facturacion_concepto_" + periodo + ".pdf");

            return new ResponseEntity<>(pdfContents, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al generar el PDF: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}