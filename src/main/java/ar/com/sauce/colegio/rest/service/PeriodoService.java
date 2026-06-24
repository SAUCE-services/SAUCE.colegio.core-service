package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.PeriodoDto;
import ar.com.sauce.colegio.rest.model.Periodo;
import ar.com.sauce.colegio.rest.repository.IPeriodoRepository;
import jakarta.transaction.Transactional;
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
    private IPeriodoRepository periodoRepository;

    public Page<PeriodoDto> findAllPaged(Pageable pageable) {
        // repository.findAll(pageable) ya maneja el orden y el límite de registros
        return periodoRepository.findAll(pageable)
                .map(this::convertToDto);
    }

    public Page<PeriodoDto> buscar(LocalDate primerVenc, LocalDate segundoVenc, String ciclo, Pageable pageable) {
        // Si todos los filtros son nulos, podrías usar el findAll estándar o dejar que la query maneje los NULLs
        return periodoRepository.buscarConFiltros(primerVenc, segundoVenc, ciclo, pageable)
                .map(this::convertToDto);
    }

    @Transactional
    public Periodo save(Periodo periodo) {
        // Al usar IDENTITY, si periodoId viene null o 0 de Angular,
        // forzamos null para que JPA ejecute un INSERT limpito y autoincremente
        if (periodo.getPeriodoId() != null && periodo.getPeriodoId() == 0) {
            periodo.setPeriodoId(null);
        }

        // Aquí puedes aplicar filtros o validaciones de negocio previas si fuesen necesarias
        // Ej: Validar que no exista el mismo mes/año para el ciclo lectivo actual

        return periodoRepository.save(periodo);
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