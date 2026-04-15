package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.GrupoSanguineo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IGrupoSanguineoRepository extends JpaRepository<GrupoSanguineo, Long> {}