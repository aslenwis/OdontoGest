package com.odontogest.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @Column(name = "username", nullable = false, unique = true)
    private String usernameASEJ;

    @Column(name = "password_hash", nullable = false)
    private String passwordHashASEJ;

    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompletoASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private RolASEJ rolASEJ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontologo_id")
    private OdontologoASEJ odontologoASEJ;

    @Column(name = "activo", nullable = false)
    private boolean activoASEJ = true;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidosASEJ;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHastaASEJ;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAccesoASEJ;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEnASEJ;
}
