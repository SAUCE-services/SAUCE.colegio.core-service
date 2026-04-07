package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "conf_establecimiento")
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Establecimiento extends AuditableSimple implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_establecimiento") // <--- Agrega esta línea
    private Long establecimientoId;

    private String nombre;

    private String nombreCorto;

    private String direccion;

    private String telefono;

    private String correo;

    private String fax;

    private String leyenda;

    private BigDecimal recargoDiario;

}
