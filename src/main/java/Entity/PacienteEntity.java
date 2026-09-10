package Entity;

import Entity.Emuns.GeneroPacienteEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "paciente", schema = "public")

public class PacienteEntity {
    @Id
    @Column(name = "dnipaciente", length = 8)
    private String dnipaciente;

    @Column(name = "nombrepaciente",length = 30, nullable = false)
    private String nombrePaciente;

    @Column(name = "apellidopaciente",length = 30, nullable = false)
    private String apellidoPaciente;

    @Column(name = "telefono",length = 9, nullable = false, unique = true)
    private String telefono;

    @Column(name = "direccion", length = 50, nullable = true)
    private String direccion;

    @Column(name = "correoelectronico",length = 50, nullable = false, unique = true)
    private String correoElectronico;

    @Column(name = "fechanacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "genero", nullable = false)
    private GeneroPacienteEnum genero;

    @Column(name = "tiposangre", nullable = false)
    private String tipoSangre;

    @Column(name = "alergias", columnDefinition = "TEXT", nullable = true)
    private String alergias;

    @OneToOne(mappedBy = "paciente")
    private HistoriaMedicaEntity historiaMedica;

    @OneToMany(mappedBy = "paciente")
    private List<CitaMedicaEntity> citasMedicas;

    @OneToMany(mappedBy = "paciente")
    private List<ComprobantePagoEntity> comprobantePagos;

    /** Edad calculada a partir de la fecha de nacimiento; no es una columna, solo se usa en pantalla. */
    public int getEdad() {
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }
}
