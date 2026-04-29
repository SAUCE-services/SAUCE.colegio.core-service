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

    public Page<PeriodoDto> buscar(LocalDate primerVenc, LocalDate segundoVenc, String ciclo, Pageable pageable) {
        // Si todos los filtros son nulos, podrías usar el findAll estándar o dejar que la query maneje los NULLs
        return repository.buscarConFiltros(primerVenc, segundoVenc, ciclo, pageable)
                .map(this::convertToDto);
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