package com.odontogest.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionASEJ {

    @Id
    @Column(name = "clave")
    private String claveASEJ;

    @Column(name = "valor")
    private String valorASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoConfiguracionASEJ tipoASEJ;

    @Column(name = "grupo", nullable = false)
    private String grupoASEJ;

    @Column(name = "descripcion")
    private String descripcionASEJ;

    @Column(name = "editable", nullable = false)
    private boolean editableASEJ;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEnASEJ;
}
