/**
 * 
 */
package ar.com.sauce.colegio.rest.model;

import java.io.Serializable;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Type;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author daniel
 *
 */
@Data
@Entity
@Table(name = "anotador")
@EqualsAndHashCode(callSuper = false)
public class Anotador extends Auditable implements Serializable {
	/**
	* 
	*/
	private static final long serialVersionUID = 7483831893675520963L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "anotador_id")
	private Long anotadorId;
	
	@Column(name = "alumno_id")
	private Long alumnoId;
	
	@Column(name = "fecha")
	private Timestamp fecha;
	
	@Column(name = "anotacion")
	private String anotacion;
	
	@Column(name = "transaccion_id")
	private Long transaccionId;

	/**
	 * 
	 */
	public Anotador() {
	}

	/**
	 * @param alumnoId
	 * @param anotacion
	 * @param transaccionId
	 */
	public Anotador(Long alumnoId, String anotacion, Long transaccionId) {
		this.alumnoId = alumnoId;
		this.anotacion = anotacion;
		this.transaccionId = transaccionId;
	}

}
