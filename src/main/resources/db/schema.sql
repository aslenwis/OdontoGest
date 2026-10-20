-- =====================================================================
-- OdontoGest - Esquema SQLite v1
-- Ejecutado en cada arranque por spring.sql.init (idempotente).
-- =====================================================================

-- ------------------------------------------------------------ SISTEMA
CREATE TABLE IF NOT EXISTS configuracion (
    clave           TEXT PRIMARY KEY,
    valor           TEXT,
    tipo            TEXT NOT NULL DEFAULT 'TEXTO'
                    CHECK (tipo IN ('TEXTO','ENTERO','BOOLEANO','SECRETO','JSON')),
    grupo           TEXT NOT NULL,
    descripcion     TEXT,
    editable        INTEGER NOT NULL DEFAULT 1 CHECK (editable IN (0,1)),
    actualizado_en  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS correlativo (
    serie   TEXT PRIMARY KEY,                      -- 'HC', 'RECIBO'
    ultimo  INTEGER NOT NULL DEFAULT 0 CHECK (ultimo >= 0)
);

-- ------------------------------------------------------------ PERSONAL
CREATE TABLE IF NOT EXISTS odontologo (
    id              INTEGER PRIMARY KEY,
    nombres         TEXT NOT NULL,
    apellidos       TEXT NOT NULL,
    cop             TEXT NOT NULL UNIQUE,          -- N° colegiatura (Colegio Odontológico del Perú)
    rne             TEXT,                          -- registro de especialidad
    especialidad    TEXT NOT NULL DEFAULT 'Odontología general',
    telefono        TEXT,
    email           TEXT,
    color_agenda    TEXT NOT NULL DEFAULT '#2563EB' CHECK (color_agenda GLOB '#[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]'),
    activo          INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1))
);

CREATE TABLE IF NOT EXISTS horario_atencion (
    id              INTEGER PRIMARY KEY,
    odontologo_id   INTEGER NOT NULL REFERENCES odontologo(id) ON DELETE CASCADE,
    dia_semana      INTEGER NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),   -- 1 = lunes (ISO)
    hora_inicio     TEXT NOT NULL,
    hora_fin        TEXT NOT NULL,
    CHECK (hora_fin > hora_inicio)
);
CREATE INDEX IF NOT EXISTS ix_horario_odontologo ON horario_atencion(odontologo_id, dia_semana);

CREATE TABLE IF NOT EXISTS usuario (
    id                  INTEGER PRIMARY KEY,
    username            TEXT NOT NULL UNIQUE COLLATE NOCASE,
    password_hash       TEXT NOT NULL,
    nombre_completo     TEXT NOT NULL,
    rol                 TEXT NOT NULL CHECK (rol IN ('ADMIN','ODONTOLOGO','RECEPCION')),
    odontologo_id       INTEGER REFERENCES odontologo(id) ON DELETE SET NULL,
    activo              INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1)),
    intentos_fallidos   INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta     TEXT,
    ultimo_acceso       TEXT,
    creado_en           TEXT NOT NULL,
    CHECK (rol <> 'ODONTOLOGO' OR odontologo_id IS NOT NULL)
);

-- ------------------------------------------------------------ PACIENTES
CREATE TABLE IF NOT EXISTS paciente (
    id                          INTEGER PRIMARY KEY,
    numero_historia             TEXT NOT NULL UNIQUE,              -- HC-000001
    tipo_documento              TEXT NOT NULL CHECK (tipo_documento IN ('DNI','CE','PASAPORTE','SIN_DOCUMENTO')),
    numero_documento            TEXT,
    nombres                     TEXT NOT NULL,
    apellido_paterno            TEXT NOT NULL,
    apellido_materno            TEXT,
    fecha_nacimiento            TEXT NOT NULL,
    sexo                        TEXT NOT NULL CHECK (sexo IN ('M','F')),
    telefono                    TEXT,
    email                       TEXT,
    direccion                   TEXT,
    ocupacion                   TEXT,
    contacto_emergencia_nombre  TEXT,
    contacto_emergencia_telefono TEXT,
    apoderado_nombre            TEXT,
    apoderado_documento         TEXT,
    apoderado_parentesco        TEXT,
    acepta_recordatorios        INTEGER NOT NULL DEFAULT 1 CHECK (acepta_recordatorios IN (0,1)),
    activo                      INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1)),
    creado_en                   TEXT NOT NULL,
    actualizado_en              TEXT NOT NULL,
    CHECK (tipo_documento = 'SIN_DOCUMENTO' OR numero_documento IS NOT NULL),
    CHECK (tipo_documento <> 'DNI' OR (length(numero_documento) = 8 AND numero_documento NOT GLOB '*[^0-9]*'))
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_paciente_documento
    ON paciente(tipo_documento, numero_documento) WHERE numero_documento IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_paciente_apellidos ON paciente(apellido_paterno, apellido_materno, nombres);

CREATE TABLE IF NOT EXISTS anamnesis (
    id                      INTEGER PRIMARY KEY,
    paciente_id             INTEGER NOT NULL UNIQUE REFERENCES paciente(id) ON DELETE CASCADE,
    antecedentes_medicos    TEXT,
    alergias                TEXT,
    medicacion_actual       TEXT,
    enfermedades_json       TEXT NOT NULL DEFAULT '{}',   -- {"diabetes":true,"hipertension":false,...}
    habitos                 TEXT,
    fuma                    INTEGER NOT NULL DEFAULT 0 CHECK (fuma IN (0,1)),
    gestante                INTEGER NOT NULL DEFAULT 0 CHECK (gestante IN (0,1)),
    presion_arterial        TEXT,                          -- "120/80"
    observaciones           TEXT,
    actualizado_por         INTEGER REFERENCES usuario(id),
    actualizado_en          TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS consentimiento (
    id              INTEGER PRIMARY KEY,
    paciente_id     INTEGER NOT NULL REFERENCES paciente(id) ON DELETE CASCADE,
    tipo            TEXT NOT NULL CHECK (tipo IN ('DATOS_PERSONALES','TRATAMIENTO')),
    plan_id         INTEGER REFERENCES plan_tratamiento(id) ON DELETE SET NULL,
    aceptado_en     TEXT NOT NULL,
    firmado_por     TEXT NOT NULL,                 -- paciente o apoderado
    registrado_por  INTEGER NOT NULL REFERENCES usuario(id)
);
CREATE INDEX IF NOT EXISTS ix_consentimiento_paciente ON consentimiento(paciente_id, tipo);

CREATE TABLE IF NOT EXISTS adjunto (
    id              INTEGER PRIMARY KEY,
    paciente_id     INTEGER NOT NULL REFERENCES paciente(id) ON DELETE CASCADE,
    atencion_id     INTEGER REFERENCES atencion(id) ON DELETE SET NULL,
    tipo            TEXT NOT NULL CHECK (tipo IN ('RADIOGRAFIA','FOTO','DOCUMENTO')),
    nombre_original TEXT NOT NULL,
    ruta_relativa   TEXT NOT NULL UNIQUE,          -- adjuntos/<pacienteId>/<uuid>.<ext>
    sha256          TEXT NOT NULL,
    tamano_bytes    INTEGER NOT NULL,
    descripcion     TEXT,
    creado_en       TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_adjunto_paciente ON adjunto(paciente_id);

-- ------------------------------------------------------------ ATENCIÓN CLÍNICA
CREATE TABLE IF NOT EXISTS atencion (
    id                  INTEGER PRIMARY KEY,
    paciente_id         INTEGER NOT NULL REFERENCES paciente(id),
    odontologo_id       INTEGER NOT NULL REFERENCES odontologo(id),
    cita_id             INTEGER UNIQUE REFERENCES cita(id) ON DELETE SET NULL,
    fecha               TEXT NOT NULL,
    motivo_consulta     TEXT NOT NULL,
    examen_clinico      TEXT,
    diagnostico         TEXT,
    observaciones       TEXT,
    estado              TEXT NOT NULL DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA','CERRADA')),
    cerrada_en          TEXT
);
CREATE INDEX IF NOT EXISTS ix_atencion_paciente ON atencion(paciente_id, fecha);

-- ------------------------------------------------------------ ODONTOGRAMA
CREATE TABLE IF NOT EXISTS odontograma (
    id                  INTEGER PRIMARY KEY,
    paciente_id         INTEGER NOT NULL REFERENCES paciente(id),
    atencion_id         INTEGER NOT NULL REFERENCES atencion(id),
    tipo                TEXT NOT NULL CHECK (tipo IN ('INICIAL','EVOLUCION')),
    denticion           TEXT NOT NULL CHECK (denticion IN ('PERMANENTE','DECIDUA','MIXTA')),
    especificaciones    TEXT,
    observaciones       TEXT,
    estado              TEXT NOT NULL DEFAULT 'BORRADOR' CHECK (estado IN ('BORRADOR','CERRADO')),
    creado_en           TEXT NOT NULL,
    cerrado_en          TEXT,
    cerrado_por         INTEGER REFERENCES usuario(id),
    hash_anterior       TEXT,                      -- hash del odontograma cerrado previo ('GENESIS' si es el primero)
    hash_cierre         TEXT,                      -- SHA-256 hex
    CHECK (estado = 'BORRADOR' OR (cerrado_en IS NOT NULL AND hash_cierre IS NOT NULL))
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_odontograma_inicial ON odontograma(paciente_id) WHERE tipo = 'INICIAL';
CREATE UNIQUE INDEX IF NOT EXISTS ux_odontograma_borrador ON odontograma(paciente_id) WHERE estado = 'BORRADOR';
CREATE INDEX IF NOT EXISTS ix_odontograma_paciente ON odontograma(paciente_id, creado_en);

CREATE TABLE IF NOT EXISTS hallazgo_tipo (
    id                  INTEGER PRIMARY KEY,
    codigo              TEXT NOT NULL UNIQUE,
    nombre              TEXT NOT NULL,
    sigla               TEXT,                      -- se dibuja en el recuadro de la pieza
    siglas_opciones     TEXT,                      -- JSON ["AM","R","IV"] si el usuario debe elegir
    representacion      TEXT NOT NULL CHECK (representacion IN ('SUPERFICIE','PIEZA','ENTRE_PIEZAS','RANGO_PIEZAS')),
    simbolo             TEXT NOT NULL CHECK (simbolo IN (
                            'RELLENO','CONTORNO_SUPERFICIE','ASPA','CIRCUNFERENCIA','LINEA_RAIZ',
                            'FLECHA_ARRIBA','FLECHA_ABAJO','FLECHA_GIRO','LINEA_FRACTURA',
                            'PARENTESIS','PUENTE','LLAVE','SOLO_SIGLA','RAIZ_TRIANGULO','ZIGZAG')),
    regla_color         TEXT NOT NULL CHECK (regla_color IN ('POR_ESTADO','SIEMPRE_ROJO','SIEMPRE_AZUL')),
    grupo_exclusion     TEXT NOT NULL,             -- capa: un nuevo hallazgo de la misma capa reemplaza al anterior
    oculta_superficies  INTEGER NOT NULL DEFAULT 0 CHECK (oculta_superficies IN (0,1)),  -- p. ej. pieza ausente
    conteo_cpod         TEXT NOT NULL DEFAULT 'NINGUNO' CHECK (conteo_cpod IN ('C','P','O','NINGUNO')),
    conteo_ceod         TEXT NOT NULL DEFAULT 'NINGUNO' CHECK (conteo_ceod IN ('C','E','O','NINGUNO')),
    activo              INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1)),
    orden               INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS hallazgo (
    id                  INTEGER PRIMARY KEY,
    odontograma_id      INTEGER NOT NULL REFERENCES odontograma(id) ON DELETE CASCADE,
    hallazgo_tipo_id    INTEGER NOT NULL REFERENCES hallazgo_tipo(id),
    pieza_fdi           INTEGER NOT NULL,
    pieza_fdi_fin       INTEGER,                   -- solo ENTRE_PIEZAS / RANGO_PIEZAS
    superficies         INTEGER NOT NULL DEFAULT 0 CHECK (superficies BETWEEN 0 AND 31),
    estado              TEXT CHECK (estado IN ('BUENO','MALO')),
    sigla               TEXT,                      -- sigla elegida (si el tipo tiene opciones)
    nota                TEXT,
    registrado_por      INTEGER NOT NULL REFERENCES usuario(id),
    registrado_en       TEXT NOT NULL,
    CHECK ((pieza_fdi / 10 BETWEEN 1 AND 4 AND pieza_fdi % 10 BETWEEN 1 AND 8)
        OR (pieza_fdi / 10 BETWEEN 5 AND 8 AND pieza_fdi % 10 BETWEEN 1 AND 5)),
    CHECK (pieza_fdi_fin IS NULL
        OR (pieza_fdi_fin / 10 BETWEEN 1 AND 4 AND pieza_fdi_fin % 10 BETWEEN 1 AND 8)
        OR (pieza_fdi_fin / 10 BETWEEN 5 AND 8 AND pieza_fdi_fin % 10 BETWEEN 1 AND 5))
);
CREATE INDEX IF NOT EXISTS ix_hallazgo_odontograma ON hallazgo(odontograma_id);
CREATE INDEX IF NOT EXISTS ix_hallazgo_pieza ON hallazgo(pieza_fdi);

CREATE TABLE IF NOT EXISTS hallazgo_anulacion (
    id              INTEGER PRIMARY KEY,
    hallazgo_id     INTEGER NOT NULL UNIQUE REFERENCES hallazgo(id),
    motivo          TEXT NOT NULL CHECK (length(trim(motivo)) >= 10),
    anulado_por     INTEGER NOT NULL REFERENCES usuario(id),
    anulado_en      TEXT NOT NULL
);

-- ------------------------------------------------------------ TARIFARIO Y PLAN
CREATE TABLE IF NOT EXISTS procedimiento (
    id              INTEGER PRIMARY KEY,
    codigo          TEXT NOT NULL UNIQUE,
    nombre          TEXT NOT NULL,
    categoria       TEXT NOT NULL CHECK (categoria IN ('DIAGNOSTICO','PREVENTIVO','OPERATORIA','ENDODONCIA',
                        'PERIODONCIA','CIRUGIA','PROTESIS','IMPLANTOLOGIA','ORTODONCIA','ESTETICA')),
    precio_cent     INTEGER NOT NULL CHECK (precio_cent >= 0),
    forma_cobro     TEXT NOT NULL CHECK (forma_cobro IN ('PIEZA','SUPERFICIE','ARCADA','BOCA','SESION')),
    duracion_min    INTEGER NOT NULL DEFAULT 30 CHECK (duracion_min > 0),
    activo          INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1))
);

CREATE TABLE IF NOT EXISTS regla_sugerencia (
    id                  INTEGER PRIMARY KEY,
    hallazgo_tipo_id    INTEGER REFERENCES hallazgo_tipo(id) ON DELETE CASCADE,  -- NULL = regla general (toda la boca)
    estado              TEXT CHECK (estado IN ('BUENO','MALO')),                 -- NULL = cualquiera
    tipo_radicular      TEXT CHECK (tipo_radicular IN ('UNIRRADICULAR','BIRRADICULAR','MULTIRRADICULAR')),
    min_superficies     INTEGER NOT NULL DEFAULT 0,
    max_superficies     INTEGER NOT NULL DEFAULT 5,
    procedimiento_id    INTEGER NOT NULL REFERENCES procedimiento(id),
    fase                INTEGER NOT NULL CHECK (fase BETWEEN 1 AND 4),
    prioridad           INTEGER NOT NULL DEFAULT 100,                            -- menor = se evalúa primero
    activo              INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1)),
    CHECK (min_superficies <= max_superficies)
);

CREATE TABLE IF NOT EXISTS plan_tratamiento (
    id                  INTEGER PRIMARY KEY,
    paciente_id         INTEGER NOT NULL REFERENCES paciente(id),
    odontologo_id       INTEGER NOT NULL REFERENCES odontologo(id),
    odontograma_id      INTEGER REFERENCES odontograma(id),   -- odontograma que originó las sugerencias
    fecha               TEXT NOT NULL,
    estado              TEXT NOT NULL DEFAULT 'BORRADOR'
                        CHECK (estado IN ('BORRADOR','PROPUESTO','ACEPTADO','EN_CURSO','FINALIZADO','ANULADO')),
    subtotal_cent       INTEGER NOT NULL DEFAULT 0 CHECK (subtotal_cent >= 0),
    descuento_cent      INTEGER NOT NULL DEFAULT 0 CHECK (descuento_cent >= 0),
    total_cent          INTEGER NOT NULL DEFAULT 0 CHECK (total_cent >= 0),
    observaciones       TEXT,
    aceptado_en         TEXT,
    CHECK (total_cent = subtotal_cent - descuento_cent)
);
CREATE INDEX IF NOT EXISTS ix_plan_paciente ON plan_tratamiento(paciente_id, fecha);

CREATE TABLE IF NOT EXISTS plan_item (
    id                  INTEGER PRIMARY KEY,
    plan_id             INTEGER NOT NULL REFERENCES plan_tratamiento(id) ON DELETE CASCADE,
    procedimiento_id    INTEGER NOT NULL REFERENCES procedimiento(id),
    hallazgo_id         INTEGER REFERENCES hallazgo(id),
    pieza_fdi           INTEGER,
    superficies         INTEGER NOT NULL DEFAULT 0 CHECK (superficies BETWEEN 0 AND 31),
    fase                INTEGER NOT NULL CHECK (fase BETWEEN 1 AND 4),
    cantidad            INTEGER NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    precio_unit_cent    INTEGER NOT NULL CHECK (precio_unit_cent >= 0),
    descuento_cent      INTEGER NOT NULL DEFAULT 0 CHECK (descuento_cent >= 0),
    total_cent          INTEGER NOT NULL CHECK (total_cent >= 0),
    estado              TEXT NOT NULL DEFAULT 'PENDIENTE'
                        CHECK (estado IN ('PENDIENTE','EN_PROCESO','REALIZADO','CANCELADO')),
    atencion_id         INTEGER REFERENCES atencion(id),      -- dónde se realizó
    realizado_en        TEXT,
    orden               INTEGER NOT NULL DEFAULT 0,
    CHECK (pieza_fdi IS NULL
        OR (pieza_fdi / 10 BETWEEN 1 AND 4 AND pieza_fdi % 10 BETWEEN 1 AND 8)
        OR (pieza_fdi / 10 BETWEEN 5 AND 8 AND pieza_fdi % 10 BETWEEN 1 AND 5)),
    CHECK (total_cent = cantidad * precio_unit_cent - descuento_cent)
);
CREATE INDEX IF NOT EXISTS ix_plan_item_plan ON plan_item(plan_id, fase, orden);

-- ------------------------------------------------------------ CAJA
CREATE TABLE IF NOT EXISTS cuota (
    id              INTEGER PRIMARY KEY,
    plan_id         INTEGER NOT NULL REFERENCES plan_tratamiento(id) ON DELETE CASCADE,
    numero          INTEGER NOT NULL CHECK (numero >= 0),            -- 0 = cuota inicial
    vencimiento     TEXT NOT NULL,
    monto_cent      INTEGER NOT NULL CHECK (monto_cent > 0),
    pagado_cent     INTEGER NOT NULL DEFAULT 0 CHECK (pagado_cent >= 0),
    UNIQUE (plan_id, numero),
    CHECK (pagado_cent <= monto_cent)
);
CREATE INDEX IF NOT EXISTS ix_cuota_vencimiento ON cuota(vencimiento) WHERE pagado_cent < monto_cent;

CREATE TABLE IF NOT EXISTS pago (
    id                  INTEGER PRIMARY KEY,
    numero_recibo       TEXT NOT NULL UNIQUE,                        -- R-00000001
    paciente_id         INTEGER NOT NULL REFERENCES paciente(id),
    plan_id             INTEGER REFERENCES plan_tratamiento(id),
    fecha               TEXT NOT NULL,
    monto_cent          INTEGER NOT NULL CHECK (monto_cent > 0),
    medio               TEXT NOT NULL CHECK (medio IN ('EFECTIVO','TARJETA','YAPE','PLIN','TRANSFERENCIA')),
    referencia          TEXT,
    registrado_por      INTEGER NOT NULL REFERENCES usuario(id),
    anulado             INTEGER NOT NULL DEFAULT 0 CHECK (anulado IN (0,1)),
    motivo_anulacion    TEXT,
    anulado_en          TEXT,
    CHECK (anulado = 0 OR (motivo_anulacion IS NOT NULL AND anulado_en IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS ix_pago_fecha ON pago(fecha);
CREATE INDEX IF NOT EXISTS ix_pago_paciente ON pago(paciente_id);

CREATE TABLE IF NOT EXISTS pago_aplicacion (
    id          INTEGER PRIMARY KEY,
    pago_id     INTEGER NOT NULL REFERENCES pago(id) ON DELETE CASCADE,
    cuota_id    INTEGER NOT NULL REFERENCES cuota(id),
    monto_cent  INTEGER NOT NULL CHECK (monto_cent > 0),
    UNIQUE (pago_id, cuota_id)
);

-- ------------------------------------------------------------ AGENDA
CREATE TABLE IF NOT EXISTS cita (
    id                      INTEGER PRIMARY KEY,
    paciente_id             INTEGER NOT NULL REFERENCES paciente(id),
    odontologo_id           INTEGER NOT NULL REFERENCES odontologo(id),
    inicio                  TEXT NOT NULL,
    fin                     TEXT NOT NULL,
    motivo                  TEXT NOT NULL,
    estado                  TEXT NOT NULL DEFAULT 'PROGRAMADA'
                            CHECK (estado IN ('PROGRAMADA','CONFIRMADA','ATENDIDA','CANCELADA','NO_ASISTIO')),
    plan_item_id            INTEGER REFERENCES plan_item(id) ON DELETE SET NULL,
    notas                   TEXT,
    recordatorio_enviado_en TEXT,
    creado_por              INTEGER NOT NULL REFERENCES usuario(id),
    creado_en               TEXT NOT NULL,
    CHECK (fin > inicio)
);
CREATE INDEX IF NOT EXISTS ix_cita_odontologo_inicio ON cita(odontologo_id, inicio);
CREATE INDEX IF NOT EXISTS ix_cita_paciente_inicio ON cita(paciente_id, inicio);
CREATE INDEX IF NOT EXISTS ix_cita_recordatorio ON cita(inicio)
    WHERE recordatorio_enviado_en IS NULL AND estado IN ('PROGRAMADA','CONFIRMADA');

CREATE TABLE IF NOT EXISTS notificacion_log (
    id          INTEGER PRIMARY KEY,
    cita_id     INTEGER REFERENCES cita(id) ON DELETE SET NULL,
    canal       TEXT NOT NULL CHECK (canal IN ('EMAIL')),
    destino     TEXT NOT NULL,
    estado      TEXT NOT NULL CHECK (estado IN ('ENVIADO','FALLIDO','OMITIDO')),
    detalle     TEXT,
    fecha       TEXT NOT NULL
);

-- ------------------------------------------------------------ AUDITORÍA
CREATE TABLE IF NOT EXISTS auditoria (
    id          INTEGER PRIMARY KEY,
    usuario_id  INTEGER REFERENCES usuario(id),
    accion      TEXT NOT NULL,              -- LOGIN, HALLAZGO_CREAR, HALLAZGO_ANULAR, ODONTOGRAMA_CERRAR, PAGO_REGISTRAR...
    entidad     TEXT,
    entidad_id  INTEGER,
    detalle     TEXT,                       -- JSON (Gson)
    fecha       TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_auditoria_fecha ON auditoria(fecha);
CREATE INDEX IF NOT EXISTS ix_auditoria_entidad ON auditoria(entidad, entidad_id);
