/**
 * 
 */
package ar.com.sauce.colegio.rest.model.view.pk;

import java.io.Serializable;

import lombok.Data;

/**
 * @author daniel
 *
 */
@Data
public class FacturaParaInteresPk implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6666849349992414308L;

	private Long alumnoId;
	private Long facturaId;
}
