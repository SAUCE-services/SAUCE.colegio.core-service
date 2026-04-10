package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.ConceptoDto;
import ar.com.sauce.colegio.rest.model.Concepto;
import ar.com.sauce.colegio.rest.repository.IConceptoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ConceptoService {

    @Autowired
    private IConceptoRepository repository;

    public Page<ConceptoDto> findAllPaged(Pageable pageable) {
        return repository.findAll(pageable).map(this::convertToDto);
    }

    public Concepto save(Concepto concepto) {
        return repository.save(concepto);
    }

    private ConceptoDto convertToDto(Concepto concepto) {
        return new ConceptoDto(
                concepto.getConceptoId(),
                concepto.getDescripcion(),
                concepto.getImporte()
        );
    }
}