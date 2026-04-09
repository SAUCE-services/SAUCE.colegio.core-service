package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // ✅ Import necesario
import org.springframework.data.repository.query.Param; // ✅ Import necesario
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ICursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findAllByCursoIdIn(List<Long> cursoId);

    Optional<Curso> findByDescripcion(String descripcion);

    // ✅ La consulta debe estar dentro de la interfaz
    @Query("SELECT c FROM Curso c " +
            "JOIN FETCH c.maestro " +
            "JOIN FETCH c.turno " +
            "JOIN FETCH c.establecimiento " +
            "JOIN FETCH c.ciclo " +
            "WHERE c.descripcion = :descripcion")
    Optional<Curso> findByDescripcionConDetalles(@Param("descripcion") String descripcion);
} // ✅ Asegúrate de que esta llave cierre TODO el archivo