package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.dto.MaestroDto;
import ar.com.sauce.colegio.rest.dto.MaestroRequestDto;
import ar.com.sauce.colegio.rest.model.*;
import ar.com.sauce.colegio.rest.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MaestroService {

    @Autowired
    private IMaestroRepository maestroRepository;
    @Autowired
    private ILocalidadRepository localidadRepository;
    @Autowired
    private IActividadRepository actividadRepository;
    @Autowired
    private IEstablecimientoRepository establecimientoRepository;
    @Autowired
    private ITipoDocumentoRepository tipoDocumentoRepository;

    public Page<MaestroDto> findAllPaged(Pageable pageable) {
        return maestroRepository.findAll(pageable).map(this::convertToDto);
    }

    // 🌟 Un solo método para alta y edición: si id es null, crea; si no, actualiza
    public Maestro guardarMaestro(Long id, MaestroRequestDto dto) {
        Maestro maestro = (id != null)
                ? maestroRepository.findById(id).orElseThrow(() -> new RuntimeException("Maestro no encontrado (id " + id + ")"))
                : new Maestro();

        maestro.setApellido(dto.getApellido());
        maestro.setNombre(dto.getNombre());
        maestro.setNroDocumento(dto.getNroDocumento());
        maestro.setDirCalle(dto.getDirCalle());
        maestro.setDirNumero(dto.getDirNumero());
        maestro.setDirPiso(dto.getDirPiso());
        maestro.setDirDepto(dto.getDirDepto());
        maestro.setTelefonoFijo(dto.getTelefonoFijo());
        maestro.setTelefonoCelular(dto.getTelefonoCelular());

        if (dto.getTipoDocumentoId() != null) {
            TipoDocumento td = tipoDocumentoRepository.findById(dto.getTipoDocumentoId())
                    .orElseThrow(() -> new RuntimeException("Tipo de documento no encontrado"));
            maestro.setTipoDocumento(td);
        } else {
            maestro.setTipoDocumento(null);
        }

        if (dto.getLocalidadId() != null) {
            Localidad localidad = localidadRepository.findById(dto.getLocalidadId())
                    .orElseThrow(() -> new RuntimeException("Localidad no encontrada"));
            maestro.setLocalidad(localidad);
        } else {
            maestro.setLocalidad(null);
        }

        if (dto.getActividadId() != null) {
            Actividad actividad = actividadRepository.findById(dto.getActividadId())
                    .orElseThrow(() -> new RuntimeException("Actividad no encontrada"));
            maestro.setActividad(actividad);
        } else {
            maestro.setActividad(null);
        }

        if (dto.getEstablecimientoId() != null) {
            Establecimiento establecimiento = establecimientoRepository.findById(dto.getEstablecimientoId())
                    .orElseThrow(() -> new RuntimeException("Establecimiento no encontrado"));
            maestro.setEstablecimiento(establecimiento);
        } else {
            maestro.setEstablecimiento(null);
        }

        return maestroRepository.save(maestro);
    }

    public MaestroDto obtenerPorId(Long id) {
        Maestro maestro = maestroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maestro no encontrado (id " + id + ")"));
        return convertToDto(maestro);
    }

    private MaestroDto convertToDto(Maestro m) {
        return new MaestroDto(
                m.getMaestroId(),
                m.getApellido(),
                m.getNombre(),
                m.getNroDocumento(),
                m.getTipoDocumento() != null ? m.getTipoDocumento().getTipoDocumentoId() : null,
                m.getTipoDocumento() != null ? m.getTipoDocumento().getDescripcion() : null,
                m.getDirCalle(),
                m.getDirNumero(),
                m.getDirPiso(),
                m.getDirDepto(),
                m.getTelefonoFijo(),
                m.getTelefonoCelular(),
                m.getLocalidad() != null ? m.getLocalidad().getLocalidadId() : null,
                m.getLocalidad() != null ? m.getLocalidad().getDescripcion() : null,
                m.getActividad() != null ? m.getActividad().getActividadId() : null,
                m.getActividad() != null ? m.getActividad().getDescripcion() : null,
                m.getEstablecimiento() != null ? m.getEstablecimiento().getEstablecimientoId() : null,
                m.getEstablecimiento() != null ? m.getEstablecimiento().getNombre() : null
        );
    }
}