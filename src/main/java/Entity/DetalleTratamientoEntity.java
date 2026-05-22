package Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "detalletratamiento", schema = "public")

public class DetalleTratamientoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "iddetalletratamiento")
    private Long idDetalleTratamiento;

    @Column(name = "dosisindicada", length = 30, nullable = false)
    private String dosisIndicada;

    @Column(name = "frecuenciadetoma", length = 30, nullable = false)
    private String frecuenciaDeToma;

    @Column(name = "observaciones", columnDefinition = "TEXT", nullable = true)
    private String observaciones;

    @ManyToOne
    @JoinColumn(name = "idtratamiento")
    private TratamientoEntity tratamiento;

    @ManyToOne
    @JoinColumn(name = "codmedicamento")
    private MedicamentoEntity medicamento;

}
