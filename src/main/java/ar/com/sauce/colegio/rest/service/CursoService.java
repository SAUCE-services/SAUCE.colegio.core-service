package ar.com.sauce.colegio.rest.service;

import ar.com.sauce.colegio.rest.model.Curso;
import ar.com.sauce.colegio.rest.repository.ICursoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CursoService {

    private final ICursoRepository repository;

    @Autowired
    public CursoService(ICursoRepository repository) {
        this.repository = repository;
    }

    public List<Curso> findAll() {
        return repository.findAll();
    }
}
