-- ============================================================
-- Migration V1 - fire-shield-localidades-ms (H2 - testes)
-- ============================================================

CREATE TABLE loc_ocorrencia (
    uuid                        CHAR(36)        NOT NULL,
    latitude                    DOUBLE          NOT NULL,
    longitude                   DOUBLE          NOT NULL,
    severidade                  VARCHAR(20)     NOT NULL,
    horario_deteccao            TIMESTAMP       NOT NULL,
    horario_salvamento          TIMESTAMP       NOT NULL,
    status_enriquecimento       VARCHAR(30)     NOT NULL,
    tentativas_enriquecimento   INT DEFAULT 0   NOT NULL,
    place_id                    BIGINT          NULL,
    nome                        VARCHAR(255)    NULL,
    display_name                VARCHAR(500)    NULL,
    bairro                      VARCHAR(255)    NULL,
    cidade                      VARCHAR(255)    NULL,
    estado                      VARCHAR(255)    NULL,
    regiao                      VARCHAR(255)    NULL,
    cep                         VARCHAR(20)     NULL,
    pais                        VARCHAR(100)    NULL,
    codigo_pais                 VARCHAR(2)      NULL,
    CONSTRAINT pk_loc_ocorrencia PRIMARY KEY (uuid),
    CONSTRAINT chk_loc_ocorrencia_severidade
        CHECK (severidade IN ('CRITICO', 'ALTA', 'MEDIA', 'BAIXA')),
    CONSTRAINT chk_loc_ocorrencia_status
        CHECK (status_enriquecimento IN ('PENDENTE', 'ENRIQUECIDA', 'COORDENADAS_INVALIDAS', 'SERVICO_INDISPONIVEL'))
);

CREATE TABLE loc_outbox_event (
    id             CHAR(36)        NOT NULL,
    reference_id   VARCHAR(255)    NOT NULL,
    event_type     VARCHAR(100)    NOT NULL,
    destination    VARCHAR(255)    NOT NULL,
    payload        CLOB            NOT NULL,
    status         VARCHAR(20)     NOT NULL,
    criado_em      TIMESTAMP       NOT NULL,
    enviado_em     TIMESTAMP       NULL,
    trace_id       CHAR(32)        NULL,
    span_id        CHAR(16)        NULL,
    trace_sampled  VARCHAR(8)      NULL,
    CONSTRAINT pk_loc_outbox_event PRIMARY KEY (id),
    CONSTRAINT chk_loc_outbox_event_status
        CHECK (status IN ('PENDENTE', 'ENVIADO'))
);

CREATE INDEX idx_loc_outbox_event_status ON loc_outbox_event (status);
CREATE INDEX idx_loc_ocorrencia_status_enriq ON loc_ocorrencia (status_enriquecimento);

