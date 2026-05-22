package Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "medico", schema = "public")

public class MedicoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "idmedico")
    private Long idMedico;

    @Column(name = "nombremedico", nullable = false, length = 30)
    private String nombreMedico;

    @Column(name = "apellidomedico", nullable = false, length = 30)
    private String apellidoMedico;

    @Column(name = "telefono", nullable = false, unique = true, length = 9)
    private String telefono;

    @Column(name = "cmpmedico", nullable = false, unique = true, length = 8)
    private String cmpMedico;

    @Column(name = "especialidad", nullable = false, length = 20)
    private String especialidad;

    @Column(name = "horarioatencion", nullable = false, length = 20)
    private String horarioAtencion;

    @OneToOne(mappedBy = "medico")
    private UsuarioEntity usuario;

    @OneToMany(mappedBy = "medico")
    private List<CitaMedicaEntity> citasMedicas;
}
