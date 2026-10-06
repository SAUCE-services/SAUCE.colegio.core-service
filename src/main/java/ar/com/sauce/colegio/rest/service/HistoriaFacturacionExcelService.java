package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.FacturaDetalleDto;
import ar.com.sauce.colegio.rest.dto.HistoriaFacturacionDto;
import ar.com.sauce.colegio.rest.dto.LineaDetalleDto;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Genera el Excel de "Historia Facturación" con lo mismo que muestra la pantalla:
 *  - Hoja "Facturas": grilla principal con saldo progresivo (Debe - Haber acumulado).
 *  - Hoja "Detalle":  conceptos de cada factura (lo que se ve al expandir una fila).
 */
@Service
@RequiredArgsConstructor
public class HistoriaFacturacionExcelService {

    private final FacturaService facturaService;

    public byte[] generarExcelHistoria(Long alumnoId) {
        HistoriaFacturacionDto historia = facturaService.obtenerHistoriaPorAlumno(alumnoId);
        List<FacturaDetalleDto> facturas = historia.getFacturas();

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Estilos st = new Estilos(wb);

            hojaFacturas(wb, st, historia, facturas);
            hojaDetalle(wb, st, facturas);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo generar el Excel de historia de facturación", e);
        }
    }

    // ---------------------------------------------------------------- Hoja 1
    private void hojaFacturas(Workbook wb, Estilos st, HistoriaFacturacionDto historia,
                              List<FacturaDetalleDto> facturas) {
        Sheet sh = wb.createSheet("Facturas");

        Row r0 = sh.createRow(0);
        celda(r0, 0, "Alumno:", st.negrita);
        r0.createCell(1).setCellValue(historia.getNombreCompleto());
        Row r1 = sh.createRow(1);
        celda(r1, 0, "Legajo:", st.negrita);
        r1.createCell(1).setCellValue(historia.getLegajo());

        String[] cols = {"Factura", "Estado", "F.Estado", "1er Venc", "F.Pago",
                "Imp.Adeudado", "Imp.Pagado", "Saldo"};
        Row head = sh.createRow(3);
        for (int i = 0; i < cols.length; i++) celda(head, i, cols[i], st.encabezado);

        int fila = 4;
        BigDecimal acumulado = BigDecimal.ZERO; // mismo cálculo que la pantalla: acumulado += (debe - haber)
        for (FacturaDetalleDto f : facturas) {
            BigDecimal debe = nz(f.getImpAdeudado());
            BigDecimal haber = nz(f.getImpPagado());
            acumulado = acumulado.add(debe).subtract(haber);

            Row r = sh.createRow(fila++);
            r.createCell(0).setCellValue(f.getNroFactura());
            r.createCell(1).setCellValue(f.getEstado() != null ? f.getEstado() : "");
            fecha(r, 2, f.getFechaEstado(), st.fecha);
            fecha(r, 3, f.getPrimerVenc(), st.fecha);
            fecha(r, 4, f.getFechaPago(), st.fecha);
            monto(r, 5, debe, st.monto);
            monto(r, 6, haber, st.monto);
            monto(r, 7, acumulado, st.monto);
        }

        Row total = sh.createRow(fila + 1);
        celda(total, 0, "Saldo final", st.negrita);
        monto(total, 7, acumulado, st.montoNegrita);

        for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
        sh.createFreezePane(0, 4);
    }

    // ---------------------------------------------------------------- Hoja 2
    private void hojaDetalle(Workbook wb, Estilos st, List<FacturaDetalleDto> facturas) {
        Sheet sh = wb.createSheet("Detalle");

        String[] cols = {"Factura", "F.Estado", "Concepto", "Estado", "Importe", "F.Registro", "Periodo"};
        Row head = sh.createRow(0);
        for (int i = 0; i < cols.length; i++) celda(head, i, cols[i], st.encabezado);

        int fila = 1;
        for (FacturaDetalleDto f : facturas) {
            List<LineaDetalleDto> lineas = facturaService.obtenerDetalleDeFactura(f.getNroFactura());
            for (LineaDetalleDto l : lineas) {
                Row r = sh.createRow(fila++);
                r.createCell(0).setCellValue(f.getNroFactura());
                fecha(r, 1, l.getFechaEstado(), st.fecha);
                r.createCell(2).setCellValue(l.getConcepto() != null ? l.getConcepto() : "");
                r.createCell(3).setCellValue(l.getEstado() != null ? l.getEstado() : "");
                monto(r, 4, nz(l.getImporte()), st.monto);
                fecha(r, 5, l.getFechaRegistro(), st.fecha);
                r.createCell(6).setCellValue(l.getPeriodo() != null ? l.getPeriodo() : "");
            }
        }

        for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
        sh.createFreezePane(0, 1);
    }

    // ---------------------------------------------------------------- helpers
    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static void celda(Row r, int col, String texto, CellStyle estilo) {
        Cell c = r.createCell(col);
        c.setCellValue(texto);
        c.setCellStyle(estilo);
    }

    private static void fecha(Row r, int col, LocalDate d, CellStyle estilo) {
        Cell c = r.createCell(col);
        if (d != null) c.setCellValue(d);
        c.setCellStyle(estilo);
    }

    private static void monto(Row r, int col, BigDecimal v, CellStyle estilo) {
        Cell c = r.createCell(col);
        c.setCellValue(v.doubleValue());
        c.setCellStyle(estilo);
    }

    /** Estilos creados una sola vez por libro (Excel limita la cantidad de estilos). */
    private static final class Estilos {
        final CellStyle encabezado, negrita, fecha, monto, montoNegrita;

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
        }
    }
}