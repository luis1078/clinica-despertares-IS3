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
@Table(name = "medicamento", schema = "public")

public class MedicamentoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "codmedicamento")
    private Long codMedicamento;

    @Column (name = "nombremedicamento", length = 30, nullable = false)
    private String nombreMedicamento;

    @Column (name = "descripcionmedicamento", columnDefinition = "TEXT", nullable = false)
    private String descripcionMedicamento;

    @Column (name = "tipomedicamento", length = 20, nullable = false)
    private String tipoMedicamento;

    @Column (name = "fechavencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column (name = "stockinventario", nullable = false)
    private int stockInventario = 0;

    //define la FK de proveedor, es el dueño de la relación entre Proveedor y Medicamento.
    @ManyToOne
    @JoinColumn(name = "rucproveedor") //aca se usa la PK de la tabla Proveedor, que seria FK
    private ProveedorEntity proveedor; //se define

    //define relación
    @OneToMany(mappedBy = "medicamento")
    private List<DetalleTratamientoEntity> detalleTratamientos;

}
