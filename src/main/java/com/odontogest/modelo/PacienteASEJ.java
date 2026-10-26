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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "paciente")
@Getter
@Setter
@NoArgsConstructor
public class PacienteASEJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idASEJ;

    @Column(name = "numero_historia", nullable = false, unique = true, updatable = false)
    private String numeroHistoriaASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false)
    private TipoDocumentoASEJ tipoDocumentoASEJ;

    @Column(name = "numero_documento")
    private String numeroDocumentoASEJ;

    @Column(name = "nombres", nullable = false)
    private String nombresASEJ;

    @Column(name = "apellido_paterno", nullable = false)
    private String apellidoPaternoASEJ;

    @Column(name = "apellido_materno")
    private String apellidoMaternoASEJ;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimientoASEJ;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", nullable = false)
    private SexoASEJ sexoASEJ;

    @Column(name = "telefono")
    private String telefonoASEJ;

    @Column(name = "email")
    private String emailASEJ;

    @Column(name = "direccion")
    private String direccionASEJ;

    @Column(name = "ocupacion")
    private String ocupacionASEJ;

    @Column(name = "contacto_emergencia_nombre")
    private String contactoEmergenciaNombreASEJ;

    @Column(name = "contacto_emergencia_telefono")
    private String contactoEmergenciaTelefonoASEJ;

    @Column(name = "apoderado_nombre")
    private String apoderadoNombreASEJ;

    @Column(name = "apoderado_documento")
    private String apoderadoDocumentoASEJ;

    @Column(name = "apoderado_parentesco")
    private String apoderadoParentescoASEJ;

    @Column(name = "acepta_recordatorios", nullable = false)
    private boolean aceptaRecordatoriosASEJ = true;

    @Column(name = "activo", nullable = false)
    private boolean activoASEJ = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEnASEJ;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEnASEJ;

    public String getNombreCompletoASEJ() {
        String apellidosASEJ = apellidoMaternoASEJ == null || apellidoMaternoASEJ.isBlank()
                ? apellidoPaternoASEJ : apellidoPaternoASEJ + " " + apellidoMaternoASEJ;
        return apellidosASEJ + ", " + nombresASEJ;
    }
}
