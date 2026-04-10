package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.PeriodoDto;
import ar.com.sauce.colegio.rest.model.Periodo;
import ar.com.sauce.colegio.rest.repository.IPeriodoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PeriodoService {
    @Autowired
    private IPeriodoRepository repository;

    public Page<PeriodoDto> findAllPaged(Pageable pageable) {
        // repository.findAll(pageable) ya maneja el orden y el límite de registros
        return repository.findAll(pageable)
                .map(this::convertToDto);
    }

    public Page<PeriodoDto> findByFiltroFechas(LocalDate primerVenc, LocalDate segundoVenc, Pageable pageable) {
        Page<Periodo> resultados;

        if (primerVenc != null && segundoVenc != null) {
            // Debes agregar estos métodos con Pageable en tu IPeriodoRepository
            resultados = repository.findAllByMesAndFechaSegundo(primerVenc, segundoVenc, pageable);
        } else if (primerVenc != null) {
            resultados = repository.findAllByMes(primerVenc, pageable);
        } else if (segundoVenc != null) {
            resultados = repository.findAllByFechaSegundo(segundoVenc, pageable);
        } else {
            // Si no hay filtros, llama al método paged que hicimos antes
            return repository.findAll(pageable).map(this::convertToDto);
        }

        return resultados.map(this::convertToDto);
    }

    public Periodo save(Periodo periodo) {
        return repository.save(periodo);
    }

    private PeriodoDto convertToDto(Periodo p) {
        PeriodoDto dto = new PeriodoDto();
        dto.setPeriodoId(p.getPeriodoId());
        dto.setDescripcion(p.getDescripcion());
        dto.setMes(p.getMes());
        dto.setFechaSegundo(p.getFechaSegundo());
        if (p.getCiclo() != null) {
            dto.setNombreCiclo(p.getCiclo().getNombre());
        }
        return dto;
    }
}