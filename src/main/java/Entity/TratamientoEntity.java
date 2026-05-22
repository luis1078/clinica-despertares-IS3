package Entity;

import Entity.Emuns.EstadoTratamientoEnum;
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
@Table (name = "tratamiento", schema = "public")

public class TratamientoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "idtratamiento")
    private Long idTratamiento;

    @Column(name = "descripciontratamiento", columnDefinition = "TEXT", nullable = false)
    private String descripcionTratamiento;

    @Column(name = "fechainicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fechafin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "tipotratamiento", nullable = false)
    private String tipoTratamiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadotratamiento", nullable = false)
    private EstadoTratamientoEnum estadoTratamiento;

    @OneToMany(mappedBy = "tratamiento") //atributo Java
    private List<DetalleTratamientoEntity> detalleTratamientos;

    @ManyToOne
    @JoinColumn(name = "codhistoriamedica")
    private HistoriaMedicaEntity historiaMedica;

}
