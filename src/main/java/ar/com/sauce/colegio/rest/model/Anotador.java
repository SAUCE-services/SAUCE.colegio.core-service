/**
 * 
 */
package ar.com.sauce.colegio.rest.model;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

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
	private Long anotadorId;
	
	private Long alumnoId;
	
	private Timestamp fecha;
	
	private String anotacion;
	
	private Long transaccionId;

}
