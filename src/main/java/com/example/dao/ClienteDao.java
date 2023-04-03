package com.example.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.entities.Cliente;


public interface ClienteDao extends JpaRepository<Cliente, Long> {
   

   @Query(value = "select c from Cliente c left join c.hotel left join c.mascotas")
    public List<Cliente> findAll(Sort sort);
    
    @Query(value = "select c from Cliente c left join c.hotel left join c.mascotas", 
    countQuery = "select count(c) from Cliente c left join c.hotel left join c.mascotas")
    public Page<Cliente> findAll(Pageable pageable);

    @Query(value = "select c from Cliente c left join c.hotel left join c.mascotas where c.id = :id") //Consulta parametro con nombre
    public Cliente findById(long id);
 
}
