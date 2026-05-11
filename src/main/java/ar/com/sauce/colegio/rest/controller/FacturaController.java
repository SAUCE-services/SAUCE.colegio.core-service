package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.dto.HistoriaFacturacionDto;
import ar.com.sauce.colegio.rest.dto.LineaDetalleDto;
import ar.com.sauce.colegio.rest.dto.ReporteFacturaPeriodoDto;
import ar.com.sauce.colegio.rest.dto.ReporteRecaudacionDto;
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

    @GetMapping("/detalle/{facturaId}")
    public ResponseEntity<List<LineaDetalleDto>> getDetalle(@PathVariable Long facturaId) {
        return ResponseEntity.ok(facturaService.obtenerDetalleDeFactura(facturaId));
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
}