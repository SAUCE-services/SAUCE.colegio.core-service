package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.ObraSocial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IObraSocialRepository extends JpaRepository<ObraSocial, Long> {}