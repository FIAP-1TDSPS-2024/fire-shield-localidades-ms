package br.com.catech.fire_shield_localidades_ms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, updatable = false, nullable = false)
    private UUID id;

    @Column(name = "reference_id", nullable = false)
    private String referenceId;      // ID do pedido que originou o evento

    @Column(name = "class", nullable = false, columnDefinition = "CLOB")
    private String type;             // ex: "PEDIDO" — identifica o tipo do agregado

    @Column(nullable = false, columnDefinition = "CLOB")
    private String destination;      // ex: "pedido.queue" — fila de destino

    @Column(nullable = false, columnDefinition = "CLOB")
    private String payload;          // conteúdo JSON da mensagem

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;

    @Column(name = "trace_id", length = 32)
    private String traceId;

    @Column(name = "span_id", length = 16)
    private String spanId;

    @Column(name = "trace_sampled", length = 8)
    private String traceSampled;

    public OutboxEvent() {
    }

    public OutboxEvent(String referenceId, String type, String destination, String payload) {
        this(referenceId, type, destination, payload, null, null, null);
    }

    public OutboxEvent(String referenceId,
                       String type,
                       String destination,
                       String payload,
                       String traceId,
                       String spanId,
                       String traceSampled) {
        this.referenceId = referenceId;
        this.type = type;
        this.destination = destination;
        this.payload = payload;
        this.criadoEm = LocalDateTime.now();
        this.status = Status.PENDENTE;
        this.traceId = traceId;
        this.spanId = spanId;
        this.traceSampled = traceSampled;
    }

    public void marcarComoEnviado() {
        this.status = Status.ENVIADO;
        this.enviadoEm = LocalDateTime.now();
    }

    public enum Status {
        PENDENTE,
        ENVIADO
    }
}

