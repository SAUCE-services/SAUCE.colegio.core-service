package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.Parentesco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IParentescoRepository extends JpaRepository<Parentesco, Long> {}