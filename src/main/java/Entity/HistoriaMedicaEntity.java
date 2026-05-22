package Entity;

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
@Table(name = "historiamedica", schema = "public")

public class HistoriaMedicaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "codhistoriamedica")
    private Long codHistoriaMedica;

    @Column(name = "fechacreacion", nullable = false)
    private LocalDate fechaCreacion;

    @Column(name = "antecedentespersonales", columnDefinition = "TEXT", nullable = true)
    private String antecedentesPersonales;

    @Column(name = "antecedentesfamiliares", columnDefinition = "TEXT", nullable = true)
    private String antecedentesFamiliares;

    @Column(name = "intervencionesquirurgicas", columnDefinition = "TEXT", nullable = true)
    private String intervencionesQuirurgicas;

    @OneToMany(mappedBy = "historiaMedica")
    private List<TratamientoEntity> tratamientos;

    @OneToMany(mappedBy = "historiaMedica")
    private List<DiagnosticoEntity> diagnosticos;

    @OneToOne
    @JoinColumn(name = "dnipaciente", unique = true)
    private PacienteEntity paciente;

}
