package Entity;

import Entity.Emuns.EstadoCitaEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "citamedica", schema = "public")

public class CitaMedicaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "codcitamedica")
    private Long codCitaMedica;

    @Column(name = "fechacita", nullable = false)
    private LocalDate fechaCita;

    @Column(name = "horacita", nullable = false)
    private LocalTime horaCita;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadocita", nullable = false)
    private EstadoCitaEnum estadoCita;

    @Column(name = "motivoconsulta", columnDefinition = "TEXT", nullable = false)
    private String motivoConsulta;

    @ManyToOne
    @JoinColumn(name = "dnipaciente")
    private PacienteEntity paciente;

    @ManyToOne
    @JoinColumn(name = "idmedico")
    private MedicoEntity medico;
}
