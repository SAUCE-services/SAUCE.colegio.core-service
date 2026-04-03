package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "cartas_medicas")
public class CartaMedica extends Auditable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carta_medica")
    private Long idCartaMedica;

    @Column(name = "descripcion_enfermedad")
    private String descripcionEnfermedad;

    @Column(name = "medicamentos_toma")
    private String medicamentosToma;

    @Column(name = "medicamentos_alergia")
    private Long medicamentosAlergia;

    @Column(name = "tel_emergencia")
    private String telefonoEmergencia;

    @Column(name = "tel_emergencia2")
    private String telefonoEmergencia2;

    @Column(name = "padece_enfermedad")
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
