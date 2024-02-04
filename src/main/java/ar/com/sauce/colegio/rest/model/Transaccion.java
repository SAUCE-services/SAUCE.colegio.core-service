/**
 * 
 */
package ar.com.sauce.colegio.rest.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

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
