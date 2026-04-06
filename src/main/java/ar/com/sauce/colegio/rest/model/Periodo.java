package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "periodos")
@AttributeOverride(name = "updated",
        column = @Column(name = "created", insertable = false, updatable = false))
public class Periodo extends Auditable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_periodo")
    private Long idPeriodo;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "mes")
    private LocalDate mes;

    @Column(name = "fecha_segundo")
    private LocalDate fechaSegundo;

    @ManyToOne
    @JoinColumn(name = "ciclo_id")
    private Ciclo ciclo;
}
