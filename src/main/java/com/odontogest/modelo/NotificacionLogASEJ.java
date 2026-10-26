package com.odontogest.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "notificacion_log")
@Getter
@Setter
@NoArgsConstructor
public class NotificacionLogASEJ {

    public enum EstadoASEJ {
        ENVIADO, FALLIDO, OMITIDO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @Column(name = "cita_id")
    private Long citaIdASEJ;

    @Column(name = "canal", nullable = false)
    private String canalASEJ = "EMAIL";

    @Column(name = "destino", nullable = false)
    private String destinoASEJ;

    @Column(name = "estado", nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoASEJ estadoASEJ;

    @Column(name = "detalle")
    private String detalleASEJ;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fechaASEJ;

    public static NotificacionLogASEJ deASEJ(Long citaIdASEJ, String destinoASEJ, EstadoASEJ estadoASEJ, String detalleASEJ, LocalDateTime fechaASEJ) {
        NotificacionLogASEJ nASEJ = new NotificacionLogASEJ();
        nASEJ.citaIdASEJ = citaIdASEJ;
        nASEJ.destinoASEJ = destinoASEJ;
        nASEJ.estadoASEJ = estadoASEJ;
        nASEJ.detalleASEJ = detalleASEJ == null || detalleASEJ.length() <= 500 ? detalleASEJ : detalleASEJ.substring(0, 500);
        nASEJ.fechaASEJ = fechaASEJ;
        return nASEJ;
    }
}
