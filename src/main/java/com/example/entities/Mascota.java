package com.example.entities;

import java.io.Serializable;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Mascota implements Serializable {
    
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    @NotEmpty(message = "El nombre no puede estar vacío")
    @Size(min = 4, max = 25, message = "El nombre tiene que estar entre 4 y 25 caracteres")
    private String nombre;

    @NotEmpty(message = "La raza no puede estar vacío")
    @Size(min = 4, max = 25, message = "La raza tiene que estar entre 4 y 25 caracteres")
    private String raza;
    
   
   
    @Enumerated(EnumType.STRING)
   
    private Genero genero;
    public enum Genero {
        MACHO, HEMBRA
    }

    @PastOrPresent(message = "La fecha de nacimiento debe ser anterior al día de hoy.")
    private LocalDate fechaNacimiento;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JsonIgnore
   //@JsonManagedReference
    // Igual que Jsonignore tiene que ver con las referencias
    private Cliente cliente;

}
