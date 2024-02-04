/**
 * 
 */
package ar.com.sauce.colegio.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.sauce.colegio.rest.model.Transaccion;
import ar.com.sauce.colegio.rest.service.TransaccionService;

/**
 * @author daniel
 *
 */
@RestController
@RequestMapping("/transaccion")
public class TransaccionController {
	@Autowired
	private TransaccionService service;
	
	@PostMapping("/")
	public ResponseEntity<Transaccion> add() {
		return new ResponseEntity<Transaccion>(service.add(), HttpStatus.OK);
	}
}
