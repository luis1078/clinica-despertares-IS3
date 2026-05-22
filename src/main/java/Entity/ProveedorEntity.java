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
@Table(name = "proveedor", schema = "public")

public class ProveedorEntity {
    @Id
    @Column (name = "rucproveedor", length = 11)
    private String rucProveedor;

    @Column (name = "nombreproveedor", length = 30, nullable = false)
    private String nombreProveedor;

    @Column(name = "correoproveedor", length = 30, unique = true, nullable = false)
    private String correoProveedor;

    @Column(name = "numerocontacto", length = 9, unique = true, nullable = false)
    private String numeroContacto;

    @Column(name = "direccion", length = 40, nullable = false)
    private String direccion;

    @OneToMany(mappedBy = "proveedor")
    private List<MedicamentoEntity> medicamentos;
}


