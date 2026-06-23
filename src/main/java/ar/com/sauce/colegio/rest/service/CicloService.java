package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.CicloLectivoDto;
import ar.com.sauce.colegio.rest.model.Ciclo;
import ar.com.sauce.colegio.rest.repository.ICicloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CicloService {

    private final ICicloRepository cicloRepository;

    public CicloService(ICicloRepository cicloRepository) {
        this.cicloRepository = cicloRepository;
    }

    @Transactional(readOnly = true)
    public List<CicloLectivoDto> listarTodos() {
        return cicloRepository.findAll().stream()
                .map(c -> new CicloLectivoDto(c.getCicloId(), c.getNombre(), c.getDesde(), c.getHasta()))
                .collect(Collectors.toList());
    }

    @Transactional
    public CicloLectivoDto guardarOCorregir(CicloLectivoDto dto) {
        // CASO 1: MODIFICACIÓN (Botón Actualizar)
        if (dto.getCicloId() != null && dto.getCicloId() > 0) {
            Ciclo ciclo = cicloRepository.findById(dto.getCicloId())
                    .orElseThrow(() -> new RuntimeException("Ciclo lectivo no encontrado con ID: " + dto.getCicloId()));

            ciclo.setNombre(dto.getNombre());
            ciclo.setDesde(dto.getDesde());
            ciclo.setHasta(dto.getHasta());

            Ciclo guardado = cicloRepository.save(ciclo);
            return new CicloLectivoDto(guardado.getCicloId(), guardado.getNombre(), guardado.getDesde(), guardado.getHasta());
        }

        // CASO 2: ALTA ABSOLUTA (Botón Agregar)
        else {
            // Calcular los IDs de forma manual e independiente
            Long proximoId = cicloRepository.obtenerProximoId();
            String uuidLimpio = UUID.randomUUID().toString().replace("-", "");

            // Ejecutamos la inserción SQL nativa saltándonos los problemas de ciclo de vida de Hibernate
            cicloRepository.insertarCicloNativo(
                    proximoId,
                    proximoId, // auto_id
                    dto.getNombre(),
                    dto.getDesde(),
                    dto.getHasta(),
                    uuidLimpio
            );

            return new CicloLectivoDto(proximoId, dto.getNombre(), dto.getDesde(), dto.getHasta());
        }
    }
}