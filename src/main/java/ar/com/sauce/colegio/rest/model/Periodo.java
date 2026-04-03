package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "periodos")
public class Periodo {
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
    private Ciclo siclo;
}
