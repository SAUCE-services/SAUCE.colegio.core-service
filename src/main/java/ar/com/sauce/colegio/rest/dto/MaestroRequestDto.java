package ar.com.sauce.colegio.rest.dto;

import lombok.Data;

@Data
public class MaestroRequestDto {
    private String apellido;
    private String nombre;
    private String nroDocumento;

    private Long tipoDocumentoId;

    private String dirCalle;
    private String dirNumero;
    private String dirPiso;
    private String dirDepto;

    private String telefonoFijo;
    private String telefonoCelular;

    private Long localidadId;
    private Long actividadId;
    private Long establecimientoId;
}