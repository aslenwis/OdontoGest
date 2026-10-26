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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "hallazgo_anulacion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HallazgoAnulacionASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hallazgo_id", nullable = false, unique = true)
    private HallazgoASEJ hallazgoASEJ;

    @Column(name = "motivo", nullable = false)
    private String motivoASEJ;

    @Column(name = "anulado_por", nullable = false)
    private Long anuladoPorASEJ;

    @Column(name = "anulado_en", nullable = false)
    private LocalDateTime anuladoEnASEJ;

    public static HallazgoAnulacionASEJ deASEJ(HallazgoASEJ hallazgoASEJ, String motivoASEJ, long anuladoPorASEJ, LocalDateTime anuladoEnASEJ) {
        HallazgoAnulacionASEJ aASEJ = new HallazgoAnulacionASEJ();
        aASEJ.hallazgoASEJ = hallazgoASEJ;
        aASEJ.motivoASEJ = motivoASEJ;
        aASEJ.anuladoPorASEJ = anuladoPorASEJ;
        aASEJ.anuladoEnASEJ = anuladoEnASEJ;
        return aASEJ;
    }
}
