package ar.com.sauce.colegio.rest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ar.com.sauce.colegio.rest.model.Madre;

@Repository
public interface IMadreRepository extends JpaRepository<Madre, Long> {
    // Retorna la madre asociada a un alumno específico
    Madre findByAlumnoAlumnoId(Long alumnoId);
}