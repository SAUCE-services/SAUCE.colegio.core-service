/**
 * 
 */
package ar.com.sauce.colegio.rest.model;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;

import jakarta.persistence.*;

import lombok.*;

/**
 * @author daniel
 *
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Anotador extends Auditable implements Serializable {
	/**
	* 
	*/
	@Serial
    private static final long serialVersionUID = 7483831893675520963L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "anotador_id")
	private Long anotadorId;
	
	private Long alumnoId;
	
	private Timestamp fecha;
	
	private String anotacion;
	
	private Long transaccionId;

	// AGREGA ESTE CONSTRUCTOR MANUAL
	public Anotador(Long alumnoId, String anotacion, Long transaccionId) {
		this.alumnoId = alumnoId;
		this.anotacion = anotacion;
		this.transaccionId = transaccionId;
	}

}
