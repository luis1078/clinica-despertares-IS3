package Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "imagendiagnostica", schema = "public")
@PrimaryKeyJoinColumn(name = "codexamenmedico")

public class ImagenDiagnosticaEntity extends ExamenMedicoEntity {
    @Column(name = "nombreimagen", length = 50, nullable = false)
    private String nombreImagen;

    @Column(name = "regioncuerpo", length = 30, nullable = false)
    private String regionCuerpo;

    @Column(name = "informemedico", columnDefinition = "TEXT", nullable = false)
    private String informeMedico;

    @Column(name = "usacontraste", nullable = false)
    private boolean usaContraste;

    @Column(name = "tipocontraste", length = 30)
    private String tipoContraste;
}
