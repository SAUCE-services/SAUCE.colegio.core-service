package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "cartas_medicas")
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class CartaMedica extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carta_medica")
    private Long cartaMedicaId;

    private String descripcionEnfermedad;

    private String medicamentosToma;

    private String medicamentosAlergia;

    @Column(name = "tel_emergencia")
    private String telefonoEmergencia;

    @Column(name = "tel_emergencia2")
    private String telefonoEmergencia2;

    private String padeceEnfermedad;

    @OneToOne
    @JoinColumn(name = "id_alumno")
    private Alumno alumno;

    @ManyToOne
    @JoinColumn(name = "id_osocial")
    private ObraSocial obraSocial;

    @ManyToOne
    @JoinColumn(name = "id_grupo_san")
    private GrupoSanguineo grupoSanguineo;

}
