package ar.com.sauce.colegio.rest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ar.com.sauce.colegio.rest.model.Padre;

@Repository
public interface IPadreRepository extends JpaRepository<Padre, Long> {
    // Retorna el padre asociado a un alumno específico
    Padre findByAlumnoAlumnoId(Long alumnoId);
}