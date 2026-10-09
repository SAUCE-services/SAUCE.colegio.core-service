package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.ConceptoDetalleAlumnoDto;
import ar.com.sauce.colegio.rest.dto.FacturacionConceptoDto;
import ar.com.sauce.colegio.rest.dto.ReporteFacturacionConceptoDto;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Excel de "Facturación por Concepto / Rubro" para un período.
 *  - Hoja "Resumen": lo mismo que muestra la pantalla (una fila por concepto + totales generales).
 *  - Hoja "Detalle": una fila por alumno/factura de cada concepto (el reporte ya trae esos datos).
 */
@Service
@RequiredArgsConstructor
public class FacturacionConceptoExcelService {

    private final FacturaService facturaService;

    public byte[] generarExcel(String periodo) {
        ReporteFacturacionConceptoDto reporte = facturaService.obtenerFacturacionPorConceptoYPeriodo(periodo);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Estilos st = new Estilos(wb);

            hojaResumen(wb, st, reporte);
            hojaDetalle(wb, st, reporte);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo generar el Excel de facturación por concepto", e);
        }
    }

    // ---------------------------------------------------------------- Hoja 1
    private void hojaResumen(Workbook wb, Estilos st, ReporteFacturacionConceptoDto reporte) {
        Sheet sh = wb.createSheet("Resumen");

        Row r0 = sh.createRow(0);
        celda(r0, 0, "Facturación por Concepto / Rubro", st.negrita);
        Row r1 = sh.createRow(1);
        celda(r1, 0, "Período:", st.negrita);
        r1.createCell(1).setCellValue(reporte.getFiltroDescripcion() != null ? reporte.getFiltroDescripcion() : "");

        String[] cols = {"Concepto / Rubro", "Cantidad Facturado", "Total Facturado",
                "Cantidad Cobrado", "Total Cobrado"};
        Row head = sh.createRow(3);
        for (int i = 0; i < cols.length; i++) celda(head, i, cols[i], st.encabezado);

        int fila = 4;
        for (FacturacionConceptoDto c : reporte.getConceptos()) {
            Row r = sh.createRow(fila++);
            r.createCell(0).setCellValue(c.getNombreConcepto() != null ? c.getNombreConcepto() : "");
            entero(r, 1, c.getCantidadFacturado(), st.entero);
            monto(r, 2, c.getTotalFacturado(), st.monto);
            entero(r, 3, c.getCantidadCobrado(), st.entero);
            monto(r, 4, c.getTotalCobrado(), st.monto);
        }

        Row total = sh.createRow(fila + 1);
        celda(total, 0, "TOTAL GENERAL", st.negrita);
        entero(total, 1, reporte.getCantidadTotalFacturado(), st.enteroNegrita);
        monto(total, 2, reporte.getGranTotalFacturado(), st.montoNegrita);
        entero(total, 3, reporte.getCantidadTotalCobrado(), st.enteroNegrita);
        monto(total, 4, reporte.getGranTotalCobrado(), st.montoNegrita);

        for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
        sh.createFreezePane(0, 4);
    }

    // ---------------------------------------------------------------- Hoja 2
    private void hojaDetalle(Workbook wb, Estilos st, ReporteFacturacionConceptoDto reporte) {
        Sheet sh = wb.createSheet("Detalle");

        String[] cols = {"Concepto / Rubro", "Legajo", "Alumno", "Factura", "Importe",
                "F.Factura", "F.Pago", "Período", "Estado"};
        Row head = sh.createRow(0);
        for (int i = 0; i < cols.length; i++) celda(head, i, cols[i], st.encabezado);

        int fila = 1;
        for (FacturacionConceptoDto c : reporte.getConceptos()) {
            if (c.getDetalles() == null) continue;
            for (ConceptoDetalleAlumnoDto d : c.getDetalles()) {
                Row r = sh.createRow(fila++);
                r.createCell(0).setCellValue(c.getNombreConcepto() != null ? c.getNombreConcepto() : "");
                if (d.getLegajo() != null) r.createCell(1).setCellValue(d.getLegajo());
                r.createCell(2).setCellValue(d.getNombreAlumno() != null ? d.getNombreAlumno() : "");
                if (d.getNroFactura() != null) r.createCell(3).setCellValue(d.getNroFactura());
                monto(r, 4, d.getImporte(), st.monto);
                fecha(r, 5, d.getFechaFactura(), st.fecha);
                fecha(r, 6, d.getFechaPago(), st.fecha);
                r.createCell(7).setCellValue(d.getPeriodo() != null ? d.getPeriodo() : "");
                r.createCell(8).setCellValue(d.isPagado() ? "Cobrado" : "Pendiente");
            }
        }

        for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
        sh.createFreezePane(0, 1);
        sh.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(fila - 1, 0), 0, cols.length - 1));
    }

    // ---------------------------------------------------------------- helpers
    private static void celda(Row r, int col, String texto, CellStyle estilo) {
        Cell c = r.createCell(col);
        c.setCellValue(texto);
        c.setCellStyle(estilo);
    }

    private static void entero(Row r, int col, Integer v, CellStyle estilo) {
        Cell c = r.createCell(col);
        c.setCellValue(v != null ? v : 0);
        c.setCellStyle(estilo);
    }

    private static void fecha(Row r, int col, LocalDate d, CellStyle estilo) {
        Cell c = r.createCell(col);
        if (d != null) c.setCellValue(d);
        c.setCellStyle(estilo);
    }

    private static void monto(Row r, int col, BigDecimal v, CellStyle estilo) {
        Cell c = r.createCell(col);
        c.setCellValue(v != null ? v.doubleValue() : 0d);
        c.setCellStyle(estilo);
    }

    /** Estilos creados una sola vez por libro (Excel limita la cantidad de estilos). */
    private static final class Estilos {
        final CellStyle encabezado, negrita, fecha, monto, montoNegrita, entero, enteroNegrita;

        Estilos(Workbook wb) {
            DataFormat df = wb.createDataFormat();

            Font bold = wb.createFont();
            bold.setBold(true);

            negrita = wb.createCellStyle();
            negrita.setFont(bold);

            encabezado = wb.createCellStyle();
            encabezado.setFont(bold);
            encabezado.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            encabezado.setAlignment(HorizontalAlignment.CENTER);

            fecha = wb.createCellStyle();
            fecha.setDataFormat(df.getFormat("dd/MM/yyyy"));
            fecha.setAlignment(HorizontalAlignment.CENTER);

            monto = wb.createCellStyle();
            monto.setDataFormat(df.getFormat("#,##0.00"));

            montoNegrita = wb.createCellStyle();
            montoNegrita.setDataFormat(df.getFormat("#,##0.00"));
            montoNegrita.setFont(bold);

            entero = wb.createCellStyle();
            entero.setDataFormat(df.getFormat("#,##0"));
            entero.setAlignment(HorizontalAlignment.CENTER);

            enteroNegrita = wb.createCellStyle();
            enteroNegrita.setDataFormat(df.getFormat("#,##0"));
            enteroNegrita.setAlignment(HorizontalAlignment.CENTER);
            enteroNegrita.setFont(bold);
        }
    }
}