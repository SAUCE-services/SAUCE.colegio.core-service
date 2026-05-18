package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.ConceptoDto;
import ar.com.sauce.colegio.rest.dto.LineaDetalleDto;
import ar.com.sauce.colegio.rest.model.Concepto;
import ar.com.sauce.colegio.rest.repository.IConceptoRepository;
import ar.com.sauce.colegio.rest.repository.projection.DeudaIndividualProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConceptoService {

    @Autowired
    private IConceptoRepository conceptoRepository;

    public Page<ConceptoDto> findAllPaged(Pageable pageable) {
        return conceptoRepository.findAll(pageable).map(this::convertToDto);
    }

    public Concepto save(Concepto concepto) {
        return conceptoRepository.save(concepto);
    }

    private ConceptoDto convertToDto(Concepto concepto) {
        return new ConceptoDto(
                concepto.getConceptoId(),
                concepto.getDescripcion(),
                concepto.getImporte()
        );
    }

    public List<LineaDetalleDto> obtenerDeudaIndividual(Long alumnoId) {
        List<DeudaIndividualProjection> historial = conceptoRepository.findDeudaIndividualByAlumnoId(alumnoId);

        List<LineaDetalleDto> detalles = new ArrayList<>();

        for (DeudaIndividualProjection hp : historial) {
            detalles.add(new LineaDetalleDto(
                    hp.getFechaEstado(),   // 1. fechaEstado
                    hp.getConcepto(),      // 2. concepto
                    hp.getEstado(),        // 3. estado
                    hp.getImporte(),       // 4. importe
                    hp.getFechaRegistro(), // 5. fechaRegistro
                    hp.getPeriodo()        // 6. periodo
            ));
        }

        return detalles;
    }
}