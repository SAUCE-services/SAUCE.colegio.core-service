package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "periodos")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Periodo extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPeriodo;

    private String descripcion;

    private LocalDate mes;

    private LocalDate fechaSegundo;

    @ManyToOne
    @JoinColumn(name = "ciclo_id")
    private Ciclo ciclo;

}
