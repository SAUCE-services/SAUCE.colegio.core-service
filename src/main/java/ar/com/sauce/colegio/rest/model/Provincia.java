package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "provincias")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Provincia extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idProvincia;

    private String descripcion;

}
