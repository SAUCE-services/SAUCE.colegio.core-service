/**
 * 
 */
package ar.com.sauce.colegio.rest.model.view;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import ar.com.sauce.colegio.rest.model.view.pk.FacturaParaInteresPk;
import lombok.Data;

/**
 * @author daniel
 *
 */
@Data
@Entity
@Table(name = "vw_facturaparainteres")
@Immutable
@IdClass(value = FacturaParaInteresPk.class)
public class FacturaParaInteres implements Serializable {
	/**
	* 
	*/
	private static final long serialVersionUID = -429124673201935477L;

	@Id
	@Column(name = "alumno_id")
	private Long alumnoId;

	@Id
	@Column(name = "factura_id")
	private Long facturaId;

	@Column(name = "periodo_id")
	private Integer periodoId;
	
	@Column(name = "importe_adeudado")
	private BigDecimal importeAdeudado;

	@Column(name = "importe_pagado")
	private BigDecimal importePagado;

	@Column(name = "vencimiento")
	private Timestamp vencimiento;

	@Column(name = "pago")
	private Timestamp pago;

}
