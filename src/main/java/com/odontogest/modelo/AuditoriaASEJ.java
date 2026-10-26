package com.odontogest.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "auditoria")
@Getter
@NoArgsConstructor
public class AuditoriaASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @Column(name = "usuario_id")
    private Long usuarioIdASEJ;

    @Column(name = "accion", nullable = false)
    private String accionASEJ;

    @Column(name = "entidad")
    private String entidadASEJ;

    @Column(name = "entidad_id")
    private Long entidadIdASEJ;

    @Column(name = "detalle")
    private String detalleASEJ;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fechaASEJ;

    public static AuditoriaASEJ deASEJ(Long usuarioIdASEJ, String accionASEJ, String entidadASEJ, Long entidadIdASEJ, String detalleASEJ,
                               LocalDateTime fechaASEJ) {
        AuditoriaASEJ aASEJ = new AuditoriaASEJ();
        aASEJ.usuarioIdASEJ = usuarioIdASEJ;
        aASEJ.accionASEJ = accionASEJ;
        aASEJ.entidadASEJ = entidadASEJ;
        aASEJ.entidadIdASEJ = entidadIdASEJ;
        aASEJ.detalleASEJ = detalleASEJ;
        aASEJ.fechaASEJ = fechaASEJ;
        return aASEJ;
    }
}
