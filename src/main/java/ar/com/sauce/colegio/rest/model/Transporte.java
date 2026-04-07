package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "transportes")
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Transporte extends AuditableSimple implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transporte")
    private Long transporteId;

    private String apellido;

    private String nombre;

    private Long nroDocumento;

    private String dirCalle;

    @Column(name = "dir_num")
    private String dirNumero;

    private String dirPiso;

    private String dirDepto;

    @Column(name = "tel_fijo")
    private String telefonoFijo;

    @Column(name = "tel_cel")
    private String telefonoCelular;

    @ManyToOne
    @JoinColumn(name = "id_tipo_doc")
    private TipoDocumento tipoDocumento;

    @ManyToOne
    @JoinColumn(name = "id_localidad")
    private Localidad localidad;

}
