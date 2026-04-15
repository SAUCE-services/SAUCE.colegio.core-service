package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IDepartamentoRepository extends JpaRepository<Departamento, Long> {}