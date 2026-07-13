package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Curso;
import ar.com.sauce.colegio.rest.repository.projection.DeudaCursoProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // ✅ Import necesario
import org.springframework.data.repository.query.Param; // ✅ Import necesario
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ICursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findAllByCursoIdIn(List<Long> cursoId);

    @Query("SELECT c FROM Curso c WHERE UPPER(TRIM(c.descripcion)) LIKE UPPER(CONCAT('%', :descripcion, '%'))")
    Optional<Curso> findByDescripcion(@Param("descripcion") String descripcion);

    // 🌟 Resolución EXACTA (sin ambigüedad): TRIM + UPPER en ambos lados, sin LIKE ni wildcards.
    // Evita el problema de findByDescripcionConDetalles, que al usar LIKE '%texto%' puede
    // matchear más de un curso cuando un nombre es substring de otro, y quedarse con el
    // primero en orden arbitrario (mostrando el curso/alumnos equivocados).
    @Query("SELECT c FROM Curso c " +
            "JOIN FETCH c.maestro " +
            "JOIN FETCH c.turno " +
            "JOIN FETCH c.establecimiento " +
            "JOIN FETCH c.ciclo " +
            "WHERE TRIM(UPPER(c.descripcion)) = TRIM(UPPER(:descripcion))")
    Optional<Curso> findByDescripcionExacta(@Param("descripcion") String descripcion);

    // ✅ La consulta debe estar dentro de la interfaz
    @Query("SELECT c FROM Curso c " +
            "JOIN FETCH c.maestro " +
            "JOIN FETCH c.turno " +
            "JOIN FETCH c.establecimiento " +
            "JOIN FETCH c.ciclo " +
            "WHERE UPPER(TRIM(c.descripcion)) LIKE UPPER(CONCAT('%', :descripcion, '%'))")
    List<Curso> findByDescripcionConDetalles(@Param("descripcion") String descripcion);

    Page<Curso> findAllByCiclo_NombreContaining(String anio, Pageable pageable);

    // 🌟 Combo sin paginar: todos los cursos de un ciclo, para poblar un <select>
    List<Curso> findAllByCiclo_NombreOrderByDescripcionAsc(String cicloNombre);

    // 🌟 Resuelve el curso ACTUAL de un alumno vía la relación real (alumnos_ciclo),
    // usado para armar el encabezado del PDF de "Factura por Alumno"
    @Query(value = "SELECT c.* FROM cursos c " +
            "INNER JOIN alumnos_ciclo ac ON c.id_cursos = ac.curso_id " +
            "WHERE ac.alumno_id = :alumnoId LIMIT 1", nativeQuery = true)
    Optional<Curso> findCursoActualDeAlumno(@Param("alumnoId") Long alumnoId);

    // 🌟 Paginación separada por tipo de establecimiento (Jardín/Inicial vs Colegio),
    // con filtro opcional de ciclo. LOWER() + los dos patrones (con y sin tilde)
    // evitan el problema de "Jardín" vs "Jardin" en los datos reales.
    @Query("SELECT c FROM Curso c WHERE " +
            "(:anio IS NULL OR c.ciclo.nombre LIKE CONCAT('%', :anio, '%')) AND " +
            "(LOWER(c.establecimiento.nombre) LIKE '%jardin%' OR LOWER(c.establecimiento.nombre) LIKE '%jardín%' OR LOWER(c.establecimiento.nombre) LIKE '%inicial%')")
    Page<Curso> findJardinPaginado(@Param("anio") String anio, Pageable pageable);

    @Query("SELECT c FROM Curso c WHERE " +
            "(:anio IS NULL OR c.ciclo.nombre LIKE CONCAT('%', :anio, '%')) AND " +
            "NOT (LOWER(c.establecimiento.nombre) LIKE '%jardin%' OR LOWER(c.establecimiento.nombre) LIKE '%jardín%' OR LOWER(c.establecimiento.nombre) LIKE '%inicial%')")
    Page<Curso> findColegioPaginado(@Param("anio") String anio, Pageable pageable);
}