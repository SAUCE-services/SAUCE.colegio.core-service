package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "cursos")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Curso extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cursos")
    private Long idCurso;

    private String descripcion;

    // --- CLAVES FORÁNEAS COMO RELACIONES ---
    @ManyToOne
    @JoinColumn(name = "id_turno")
    private Turno turno;

    @ManyToOne
    @JoinColumn(name = "id_maestro")
    private Maestro maestro;

    @ManyToOne // Un establecimiento puede tener muchos cursos
    @JoinColumn(name = "id_establecimiento")
    private Establecimiento establecimiento;

    @ManyToOne // Un ciclo puede tener muchos cursos
    @JoinColumn(name = "ciclo_id")
    private Ciclo ciclo;

}