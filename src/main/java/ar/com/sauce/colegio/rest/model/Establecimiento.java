package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "conf_establecimiento")
public class Establecimiento extends Auditable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_establecimiento")
    private Long idEstablecimiento;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "nombre_corto")
    private String nombreCorto;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "correo")
    private String correo;

    @Column(name = "fax")
    private String fax;

    @Column(name = "leyenda")
    private String leyenda;

    @Column(name = "recargo_diario")
    private BigDecimal recargoDiario;
}
