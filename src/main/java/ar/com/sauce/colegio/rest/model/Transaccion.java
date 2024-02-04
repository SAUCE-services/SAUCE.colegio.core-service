/**
 * 
 */
package ar.com.sauce.colegio.rest.model;

import java.io.Serializable;

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
@Table(name = "transaccion")
@EqualsAndHashCode(callSuper = false)
public class Transaccion extends Auditable implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -141121123293855197L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "transaccion_id")
	@Type(type = "org.hibernate.type.LongType")
	private Long transaccionId;
}
