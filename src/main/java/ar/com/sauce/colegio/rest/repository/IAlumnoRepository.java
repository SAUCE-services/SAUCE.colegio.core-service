package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Alumno;
import ar.com.sauce.colegio.rest.model.Curso;
import ar.com.sauce.colegio.rest.repository.projection.DeudaCursoProjection;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface IAlumnoRepository extends JpaRepository<Alumno, Long> {
    // Filtra alumnos por el campo de texto 'curso'
    List<Alumno> findAllByCursoIgnoreCase(String cursoNombre);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO alumnos_ciclo (alumno_id, curso_id) VALUES (:alumnoId, :cursoId) " +
            "ON DUPLICATE KEY UPDATE curso_id = :cursoId", nativeQuery = true)
    void asignarCursoRelacional(@Param("alumnoId") Long alumnoId, @Param("cursoId") Long cursoId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM alumnos_ciclo WHERE alumno_id = :alumnoId", nativeQuery = true)
    void quitarCursoRelacional(@Param("alumnoId") Long alumnoId);

    // ✅ Buscamos por la descripción real de la tabla de cursos mediante LIKE
    @Query(value = "SELECT " +
            "  cur.id_cursos AS idCurso, " +
            "  cur.descripcion AS curso, " +
            "  a.id_alumno AS legajo, " +
            "  a.nro_documento AS dni, " +
            "  CONCAT(a.apellido, ' ', a.nombre) AS alumno, " +
            "  f.nro_factura AS factura, " +
            "  p.descripcion AS periodo, " +
            "  f.pri_venc AS vencimiento, " +
            "  f.importe_adeudado AS importe " +
            "FROM factura f " +
            "INNER JOIN alumnos_facturas af ON f.id_facturas = af.id_factura " +
            "INNER JOIN alumnos a ON af.id_alumno = a.id_alumno " +
            "INNER JOIN alumnos_ciclo ac ON a.id_alumno = ac.alumno_id " +
            "INNER JOIN cursos cur ON ac.curso_id = cur.id_cursos " +
            "INNER JOIN periodos p ON f.id_periodo = p.id_periodo " +
            "WHERE f.id_estado = 2 " +
            "  AND UPPER(TRIM(cur.descripcion)) LIKE UPPER(CONCAT('%', :cursoNombre, '%')) " + // 👈 Filtrado por Nombre del Curso
            "ORDER BY alumno ASC, f.pri_venc ASC", nativeQuery = true)
    List<DeudaCursoProjection> findDeudaAlumnosByCursoNombre(@Param("cursoNombre") String cursoNombre);

    @Modifying
    @Transactional
    @Query("UPDATE Alumno a SET a.curso = :curso WHERE a.alumnoId = :alumnoId")
    void actualizarCurso(@Param("alumnoId") Long alumnoId, @Param("curso") String curso);
}