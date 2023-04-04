package com.example.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.example.entities.Mascota;

public interface MascotaService {
    public List<Mascota> findAll();
    public Mascota findById(long id);
    public Mascota save(Mascota mascota);
    public void delete(Mascota mascota);
}
