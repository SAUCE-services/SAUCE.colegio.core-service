package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.NivelEstudio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface INivelEstudioRepository extends JpaRepository<NivelEstudio, Long> {}