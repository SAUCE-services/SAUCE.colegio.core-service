/**
 * 
 */
package ar.com.sauce.colegio.rest.model;

import java.io.Serial;
import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.*;
import org.hibernate.annotations.Type;

/**
 * @author daniel
 *
 */
@Getter
@Setter
@Entity
@Table(name = "transaccion")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Transaccion extends Auditable implements Serializable {
	/**
	 * 
	 */
	@Serial
    private static final long serialVersionUID = -141121123293855197L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long transaccionId;

}
