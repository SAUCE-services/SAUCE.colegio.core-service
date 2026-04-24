package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.*;
import ar.com.sauce.colegio.rest.model.*;
import ar.com.sauce.colegio.rest.repository.*;
import ar.com.sauce.colegio.rest.repository.projection.ConceptoDetalleProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FacturaService {
    @Autowired
    private IFacturaRepository facturaRepository;
    @Autowired
    private IAlumnoRepository alumnoRepository;
    @Autowired
    private IConceptoRepository conceptoRepository;

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

    public List<LineaDetalleDto> obtenerDetalleDeFactura(Long facturaId) {
        Factura f = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

        List<LineaDetalleDto> detalles = new java.util.ArrayList<>();

        // Traemos todos los registros de alumnos_conceptos para esta factura
        List<ConceptoDetalleProjection> conceptos = conceptoRepository.findByFacturaId(facturaId);

        for (ConceptoDetalleProjection cp : conceptos) {
            detalles.add(new LineaDetalleDto(
                    f.getFechaEstado(),
                    cp.getDescripcion(), // Ahora traerá "Sin Asignar" o el nombre real
                    "Concepto FACTURADO",
                    cp.getImporte(),     // Traerá los 0.00 o los montos reales
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
    private java.time.LocalDateTime obtenerFechaCreacion(Factura factura) {
        try {
            java.lang.reflect.Field field = ar.com.sauce.colegio.rest.model.Auditable.class.getDeclaredField("created");
            field.setAccessible(true);
            return (java.time.LocalDateTime) field.get(factura);
        } catch (Exception e) {
            return java.time.LocalDateTime.now();
        }
    }
}