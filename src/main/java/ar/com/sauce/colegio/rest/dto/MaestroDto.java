package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaestroDto {
    private Long maestroId;
    private String apellido;
    private String nombre;
    private String nroDocumento;

    private Long tipoDocumentoId;
    private String tipoDocumentoDescripcion;

    private String dirCalle;
    private String dirNumero;
    private String dirPiso;
    private String dirDepto;

    private String telefonoFijo;
    private String telefonoCelular;

    private Long localidadId;
    private String localidadNombre;

    private Long actividadId;
    private String actividadNombre;

    private Long establecimientoId;
    private String establecimientoNombre;
}