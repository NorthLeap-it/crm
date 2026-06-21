package it.northleap.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Log di ogni esecuzione di un Workflow. Niente @OneToMany di ritorno su Workflow (niente ciclo
// da rompere con @JsonIgnore): la lista runs per un workflow è composta a livello di DTO via
// WorkflowRunRepository, stesso pattern di RecordDetailResponse per i RecordLink (Fase 3).
@Entity
@Table(name = "workflow_run", indexes = @Index(name = "idx_workflow_run_workflow", columnList = "workflow_id, started_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowRunStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> input;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> steps;

    private String error;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant finishedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkflowRun other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
