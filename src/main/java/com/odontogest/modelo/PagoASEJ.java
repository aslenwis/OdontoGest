package com.odontogest.modelo;

import com.odontogest.persistencia.conversor.DineroConverterASEJ;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
public class PagoASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @Column(name = "numero_recibo", nullable = false, unique = true, updatable = false)
    private String numeroReciboASEJ;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false, updatable = false)
    private PacienteASEJ pacienteASEJ;

    @Column(name = "plan_id", updatable = false)
    private Long planIdASEJ;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fechaASEJ;

    @Convert(converter = DineroConverterASEJ.class)
    @Column(name = "monto_cent", nullable = false, updatable = false)
    private BigDecimal montoASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio", nullable = false, updatable = false)
    private MedioPagoASEJ medioASEJ;

    @Column(name = "referencia", updatable = false)
    private String referenciaASEJ;

    @Column(name = "registrado_por", nullable = false, updatable = false)
    private Long registradoPorASEJ;

    @Column(name = "anulado", nullable = false)
    private boolean anuladoASEJ;

    @Column(name = "motivo_anulacion")
    private String motivoAnulacionASEJ;

    @Column(name = "anulado_en")
    private LocalDateTime anuladoEnASEJ;
}
