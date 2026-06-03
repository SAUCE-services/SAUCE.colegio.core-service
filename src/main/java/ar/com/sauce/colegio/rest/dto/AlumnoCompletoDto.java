package ar.com.sauce.colegio.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;

@Data
public class AlumnoCompletoDto {
    // Datos del Alumno
    private Long alumnoId;
    private String apellido;
    private String nombre;
    private String nroDocumento;
    private String curso; // 👈 AGREGA ESTA LÍNEA
    private String establecimiento; // 👈 AGREGA ESTA LÍNEA
    private String uuid; // 👈 AGREGA ESTA LÍNEA
    private Long tipoDocumentoId;
    private Long nacionalidadId;
    private Long transporteId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaNacimiento;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaIngreso;

    // Datos del Padre
    private String apellidoPadre;
    private String nombrePadre;
    private Long nroDocumentoPadre;
    private Long tipoDocumentoPadreId;
    private String callePadre;
    private String nroPadre;
    private String pisoPadre;
    private String deptoPadre;
    private String telFijoPadre;
    private String telCelPadre;
    private Long nivelEstudioPadreId;
    private Long departamentoPadreId;
    private Long localidadPadreId;
    private Long actividadPadreId;
    private Long parentescoPadreId;
    private boolean presentePadre;

    // Datos de la Madre (repetir estructura del padre)
    private String apellidoMadre;
    private String nombreMadre;
    private Long nroDocumentoMadre;
    private Long tipoDocumentoMadreId;
    private String calleMadre;
    private String nroMadre;
    private String pisoMadre;
    private String deptoMadre;
    private String telFijoMadre;
    private String telCelMadre;
    private Long nivelEstudioMadreId;
    private Long departamentoMadreId;
    private Long localidadMadreId;
    private Long actividadMadreId;
    private Long parentescoMadreId;
    private boolean presenteMadre;

    // Datos de Salud (Carta Médica)
    private String enfermedades;
    private boolean padeceEnfermedad;
    private String tomaMedicamentos;
    private String medicamentosAlergia;
    private Long grupoSanguineoId;
    private Long obraSocialId;
    private String telEmergencia1;
    private String telEmergencia2;
}