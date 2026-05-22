package Entity;

import Entity.Emuns.EstadoExamenMedicoEnum;
import Entity.Emuns.TipoExamenEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "examenmedico", schema = "public")
@Inheritance(strategy = InheritanceType.JOINED)

public class ExamenMedicoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "codexamenmedico")
    private Long codExamenMedico;

    @Column(name = "fechaexamen", nullable = false)
    private LocalDate fechaExamen;

    @Column(name = "fecharesultado", nullable = true)
    private LocalDate fechaResultado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipoexamen", nullable = false)
    private TipoExamenEnum tipoExamen;

    @Column(name = "observaciones", columnDefinition = "TEXT", nullable = true)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadoexamenmedico", nullable = false)
    private EstadoExamenMedicoEnum estadoExamenMedico;

    @ManyToOne
    @JoinColumn(name = "iddiagnostico") //columna FK de BD
    private DiagnosticoEntity diagnostico;
}
