package ar.com.sauce.colegio.rest.controller;

import ar.com.sauce.colegio.rest.dto.ComboDto;
import ar.com.sauce.colegio.rest.model.*;
import ar.com.sauce.colegio.rest.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/combos")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CombosController {

    private final ITipoDocumentoRepository tipoDocumentoRepository;
    private final ITipoNacionalidadRepository tipoNacionalidadRepository;
    private final ITransporteRepository transporteRepository;
    private final INivelEstudioRepository nivelEstudioRepository;
    private final IDepartamentoRepository departamentoRepository;
    private final ILocalidadRepository localidadRepository;
    private final IActividadRepository actividadRepository;
    private final IParentescoRepository parentescoRepository;
    private final IGrupoSanguineoRepository grupoSanguineoRepository;
    private final IObraSocialRepository obraSocialRepository;

    // --- SECCIÓN ALUMNO ---
    @GetMapping("/documentos")
    public List<TipoDocumento> getDocumentos() {
        return tipoDocumentoRepository.findAll();
    }

    @GetMapping("/nacionalidades")
    public List<TipoNacionalidad> getNacionalidades() {
        return tipoNacionalidadRepository.findAll();
    }

    @GetMapping("/transportes")
    public List<ComboDto> getTransportes() {
        return transporteRepository.findAll().stream()
                .map(t -> new ComboDto(t.getTransporteId(), t.getApellido() + " " + t.getNombre()))
                .collect(Collectors.toList());
    }

    // --- SECCIÓN PADRES (PADRE Y MADRE) ---
    @GetMapping("/niveles-estudio")
    public List<NivelEstudio> getNivelesEstudio() {
        return nivelEstudioRepository.findAll();
    }

    @GetMapping("/departamentos")
    public List<ComboDto> getDepartamentos() {
        return departamentoRepository.findAll().stream()
                .map(d -> new ComboDto(d.getDepartamentoId(), d.getDescripcion()))
                .collect(Collectors.toList());
    }

    @GetMapping("/localidades")
    public List<ComboDto> getLocalidades() {
        return localidadRepository.findAll().stream()
                .map(l -> new ComboDto(l.getLocalidadId(), l.getDescripcion()))
                .collect(Collectors.toList());
    }

    @GetMapping("/actividades")
    public List<Actividad> getActividades() {
        return actividadRepository.findAll();
    }

    @GetMapping("/parentescos")
    public List<Parentesco> getParentescos() {
        return parentescoRepository.findAll();
    }

    // --- SECCIÓN SALUD ---
    @GetMapping("/grupos-sanguineos")
    public List<GrupoSanguineo> getGruposSanguineos() {
        return grupoSanguineoRepository.findAll();
    }

    @GetMapping("/obras-sociales")
    public List<ObraSocial> getObrasSociales() {
        return obraSocialRepository.findAll();
    }
}