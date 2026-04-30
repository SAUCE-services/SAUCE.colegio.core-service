package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.dto.HistoriaFacturacionDto;
import ar.com.sauce.colegio.rest.dto.LineaDetalleDto;
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

    @GetMapping("/detalle/{facturaId}")
    public ResponseEntity<List<LineaDetalleDto>> getDetalle(@PathVariable Long facturaId) {
        return ResponseEntity.ok(facturaService.obtenerDetalleDeFactura(facturaId));
    }

    @GetMapping("/recaudacion-diaria")
    public ResponseEntity<ReporteRecaudacionDto> getRecaudacion(
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate fecha) {

        // El service ahora devuelve el DTO estructurado con totales y agrupamientos
        ReporteRecaudacionDto reporte = facturaService.obtenerRecaudacionEstructurada(fecha);

        return ResponseEntity.ok(reporte);
    }

// FacturaController.java

    @GetMapping("/recaudacion-pdf")
    public ResponseEntity<byte[]> descargarPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        byte[] pdfContents = facturaService.generarPdfRecaudacion(fecha);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        // 'inline' hace que el navegador intente abrirlo. 'attachment' obligaría a la descarga inmediata.
        headers.add("Content-Disposition", "inline; filename=recaudacion_" + fecha + ".pdf");

        return new ResponseEntity<>(pdfContents, headers, HttpStatus.OK);
    }
}