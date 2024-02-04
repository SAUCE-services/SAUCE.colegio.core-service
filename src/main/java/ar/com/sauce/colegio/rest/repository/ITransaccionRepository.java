/**
 * 
 */
package ar.com.sauce.colegio.rest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.sauce.colegio.rest.model.Transaccion;

/**
 * @author daniel
 *
 */
public interface ITransaccionRepository extends JpaRepository<Transaccion, Long> {

}
