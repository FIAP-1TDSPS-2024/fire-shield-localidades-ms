-- ============================================================
-- Migration V1 - fire-shield-localidades-ms (Oracle)
-- Tipos alinhados com o que o Hibernate 6 / OracleDialect gera
-- ============================================================

CREATE TABLE loc_ocorrencia (
    uuid                        RAW(16)         NOT NULL,
    latitude                    FLOAT(49)       NOT NULL,
    longitude                   FLOAT(49)       NOT NULL,
    severidade                  VARCHAR2(20)    NOT NULL,
    horario_deteccao            TIMESTAMP(6)    NOT NULL,
    horario_salvamento          TIMESTAMP(6)    NOT NULL,
    status_enriquecimento       VARCHAR2(30)    NOT NULL,
    tentativas_enriquecimento   NUMBER(10,0)    DEFAULT 0 NOT NULL,
    place_id                    NUMBER(19,0)    NULL,
    nome                        VARCHAR2(255)   NULL,
    display_name                VARCHAR2(500)   NULL,
    bairro                      VARCHAR2(255)   NULL,
    cidade                      VARCHAR2(255)   NULL,
    estado                      VARCHAR2(255)   NULL,
    regiao                      VARCHAR2(255)   NULL,
    cep                         VARCHAR2(255)   NULL,
    pais                        VARCHAR2(255)   NULL,
    codigo_pais                 VARCHAR2(2)     NULL,
    CONSTRAINT pk_loc_ocorrencia PRIMARY KEY (uuid),
    CONSTRAINT chk_loc_ocorrencia_severidade
        CHECK (severidade IN ('CRITICO', 'ALTA', 'MEDIA', 'BAIXA')),
    CONSTRAINT chk_loc_ocorrencia_status
        CHECK (status_enriquecimento IN ('PENDENTE', 'ENRIQUECIDA', 'COORDENADAS_INVALIDAS', 'SERVICO_INDISPONIVEL'))
);

CREATE TABLE loc_outbox_event (
    id             CHAR(36)        NOT NULL,
    reference_id   VARCHAR2(255)   NOT NULL,
    event_type     CLOB            NOT NULL,
    destination    CLOB            NOT NULL,
    payload        CLOB            NOT NULL,
    status         VARCHAR2(255)   NOT NULL,
    criado_em      TIMESTAMP(6)    NOT NULL,
    enviado_em     TIMESTAMP(6)    NULL,
    trace_id       VARCHAR2(32)    NULL,
    span_id        VARCHAR2(16)    NULL,
    trace_sampled  VARCHAR2(8)     NULL,
    CONSTRAINT pk_loc_outbox_event PRIMARY KEY (id),
    CONSTRAINT chk_loc_outbox_event_status
        CHECK (status IN ('PENDENTE', 'ENVIADO'))
);

CREATE INDEX idx_loc_outbox_event_status ON loc_outbox_event (status);
CREATE INDEX idx_loc_ocorrencia_status_enriq ON loc_ocorrencia (status_enriquecimento);
