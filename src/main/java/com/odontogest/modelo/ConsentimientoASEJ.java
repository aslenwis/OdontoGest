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
@Table(name = "consentimiento")
@Getter
@Setter
@NoArgsConstructor
public class ConsentimientoASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private PacienteASEJ pacienteASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoConsentimientoASEJ tipoASEJ;

    @Column(name = "plan_id")
    private Long planIdASEJ;

    @Column(name = "aceptado_en", nullable = false)
    private LocalDateTime aceptadoEnASEJ;

    @Column(name = "firmado_por", nullable = false)
    private String firmadoPorASEJ;

    @Column(name = "registrado_por", nullable = false)
    private Long registradoPorASEJ;
}
