package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ICursoRepository extends JpaRepository<Curso, Long> {
    List<Curso> findAllByCursoIdIn (List<Long> cursoId);

    // Este método es el que usará el AlumnoService para obtener la cabecera
    Optional<Curso> findByDescripcion(String descripcion);
}
