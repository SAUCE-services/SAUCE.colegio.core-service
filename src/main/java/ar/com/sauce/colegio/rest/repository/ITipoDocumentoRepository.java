package ar.com.sauce.colegio.rest.repository;

import ar.com.sauce.colegio.rest.model.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ITipoDocumentoRepository extends JpaRepository<TipoDocumento, Long> {

}