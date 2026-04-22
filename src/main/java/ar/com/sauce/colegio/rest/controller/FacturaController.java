package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.dto.HistoriaFacturacionDto;
import ar.com.sauce.colegio.rest.dto.LineaDetalleDto;
import ar.com.sauce.colegio.rest.service.FacturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // ar.com.sauce.colegio.rest.controller.FacturaController

    @GetMapping("/detalle/{facturaId}")
    public ResponseEntity<List<LineaDetalleDto>> getDetalle(@PathVariable Long facturaId) {
        return ResponseEntity.ok(facturaService.obtenerDetalleDeFactura(facturaId));
    }
}