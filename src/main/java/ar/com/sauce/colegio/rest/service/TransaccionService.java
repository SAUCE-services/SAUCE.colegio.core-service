/**
 * 
 */
package ar.com.sauce.colegio.rest.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ar.com.sauce.colegio.rest.model.Transaccion;
import ar.com.sauce.colegio.rest.repository.ITransaccionRepository;

/**
 * @author daniel
 *
 */
@Service
public class TransaccionService {
	@Autowired
	private ITransaccionRepository repository;

	public Transaccion add() {
		Transaccion transaccion = new Transaccion();
		repository.save(transaccion);
		return transaccion;
	}	
}
