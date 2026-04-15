package ar.com.sauce.colegio.rest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ar.com.sauce.colegio.rest.model.CartaMedica;

@Repository
public interface ICartaMedicaRepository extends JpaRepository<CartaMedica, Long> {
    // Retorna la carta médica de un alumno
    CartaMedica findByAlumnoAlumnoId(Long alumnoId);
}