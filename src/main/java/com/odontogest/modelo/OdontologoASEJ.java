package com.odontogest.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "odontologo")
@Getter
@Setter
@NoArgsConstructor
public class OdontologoASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @Column(name = "nombres", nullable = false)
    private String nombresASEJ;

    @Column(name = "apellidos", nullable = false)
    private String apellidosASEJ;

    @Column(name = "cop", nullable = false, unique = true)
    private String copASEJ;

    @Column(name = "rne")
    private String rneASEJ;

    @Column(name = "especialidad", nullable = false)
    private String especialidadASEJ;

    @Column(name = "telefono")
    private String telefonoASEJ;

    @Column(name = "email")
    private String emailASEJ;

    @Column(name = "color_agenda", nullable = false)
    private String colorAgendaASEJ;

    @Column(name = "activo", nullable = false)
    private boolean activoASEJ = true;

    public String getNombreCompletoASEJ() {
        return nombresASEJ + " " + apellidosASEJ;
    }
}
