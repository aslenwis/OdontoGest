package com.odontogest.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "anamnesis")
@Getter
@Setter
@NoArgsConstructor
public class AnamnesisASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false, unique = true)
    private PacienteASEJ pacienteASEJ;

    @Column(name = "antecedentes_medicos")
    private String antecedentesMedicosASEJ;

    @Column(name = "alergias")
    private String alergiasASEJ;

    @Column(name = "medicacion_actual")
    private String medicacionActualASEJ;

    @Column(name = "enfermedades_json", nullable = false)
    private String enfermedadesJsonASEJ = "{}";

    @Column(name = "habitos")
    private String habitosASEJ;

    @Column(name = "fuma", nullable = false)
    private boolean fumaASEJ;

    @Column(name = "gestante", nullable = false)
    private boolean gestanteASEJ;

    @Column(name = "presion_arterial")
    private String presionArterialASEJ;

    @Column(name = "observaciones")
    private String observacionesASEJ;

    @Column(name = "actualizado_por")
    private Long actualizadoPorASEJ;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEnASEJ;
}
