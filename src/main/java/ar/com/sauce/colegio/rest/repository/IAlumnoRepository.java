package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Alumno;
import ar.com.sauce.colegio.rest.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface IAlumnoRepository extends JpaRepository<Alumno, Long> {
    // Filtra alumnos por el campo de texto 'curso'
    List<Alumno> findAllByCurso(String cursoNombre);
}