package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Transporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ITransporteRepository extends JpaRepository<Transporte, Long> {}