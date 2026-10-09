package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.service.FacturacionConceptoExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/facturacion")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class FacturacionConceptoExcelController {

    private final FacturacionConceptoExcelService excelService;

    // 🌟 Facturación por Concepto/Rubro — Excel, filtrado por Período
    @GetMapping("/concepto/periodo-excel")
    public ResponseEntity<?> descargarExcelFacturacionPorConceptoYPeriodo(@RequestParam String periodo) {
        try {
            byte[] excel = excelService.generarExcel(periodo);

            // El período puede traer espacios o caracteres raros ("JUNIO - 2026"): se limpia para el nombre de archivo
            String nombreSeguro = periodo.replaceAll("[^A-Za-z0-9_-]", "_");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.add("Content-Disposition", "attachment; filename=facturacion_concepto_" + nombreSeguro + ".xlsx");

            return new ResponseEntity<>(excel, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al generar el Excel: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}