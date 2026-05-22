package Entity;

import Entity.Emuns.GravedadDiagnosticoEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "diagnostico", schema = "public")

public class DiagnosticoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "iddiagnostico")
    private Long idDiagnostico;

    @Column(name = "descripciondiagnostico", columnDefinition = "TEXT", nullable = false)
    private String descripcionDiagnostico;

    @Column(name = "fechadiagnostico", nullable = false)
    private LocalDate fechaDiagnostico;

    @Enumerated(EnumType.STRING)
    @Column(name = "gravedaddiagnostico", nullable = false)
    private GravedadDiagnosticoEnum gravedadDiagnostico;

    @ManyToOne
    @JoinColumn(name = "codhistoriamedica")
    private HistoriaMedicaEntity historiaMedica;

    @OneToMany(mappedBy = "diagnostico")
    private List<ExamenMedicoEntity> examenMedicos;

}
