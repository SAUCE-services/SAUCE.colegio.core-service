package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ICursoRepository extends JpaRepository<Curso, Long> {
    List<Curso> findAllByCursoIdIn (List<Long> cursoId);
}
