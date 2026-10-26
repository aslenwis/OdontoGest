package com.odontogest.modelo;

import com.odontogest.dominio.odontograma.ConteoIndiceASEJ;
import com.odontogest.dominio.odontograma.ReglaColorASEJ;
import com.odontogest.dominio.odontograma.RepresentacionASEJ;
import com.odontogest.dominio.odontograma.SimboloHallazgoASEJ;
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

@Entity
@Table(name = "hallazgo_tipo")
@Getter
@Setter
@NoArgsConstructor
public class HallazgoTipoASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @Column(name = "codigo", nullable = false, unique = true)
    private String codigoASEJ;

    @Column(name = "nombre", nullable = false)
    private String nombreASEJ;

    @Column(name = "sigla")
    private String siglaASEJ;

    @Column(name = "siglas_opciones")
    private String siglasOpcionesASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "representacion", nullable = false)
    private RepresentacionASEJ representacionASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "simbolo", nullable = false)
    private SimboloHallazgoASEJ simboloASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "regla_color", nullable = false)
    private ReglaColorASEJ reglaColorASEJ;

    @Column(name = "grupo_exclusion", nullable = false)
    private String grupoExclusionASEJ;

    @Column(name = "oculta_superficies", nullable = false)
    private boolean ocultaSuperficiesASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "conteo_cpod", nullable = false)
    private ConteoIndiceASEJ conteoCpodASEJ = ConteoIndiceASEJ.NINGUNO;

    @Enumerated(EnumType.STRING)
    @Column(name = "conteo_ceod", nullable = false)
    private ConteoIndiceASEJ conteoCeodASEJ = ConteoIndiceASEJ.NINGUNO;

    @Column(name = "activo", nullable = false)
    private boolean activoASEJ = true;

    @Column(name = "orden", nullable = false)
    private int ordenASEJ;
}
