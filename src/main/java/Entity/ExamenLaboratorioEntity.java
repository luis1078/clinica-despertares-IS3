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
@Table (name = "examenlaboratorio", schema = "public")
@PrimaryKeyJoinColumn(name = "codexamenmedico")

public class ExamenLaboratorioEntity extends ExamenMedicoEntity {
    @Column(name = "nombremuestra",length = 50, nullable = false)
    private String nombreMuestra;

    @Column(name = "descripcionresultado", columnDefinition = "TEXT", nullable = false)
    private String descripcionResultado;

    @Column(name = "valorreferencia")
    private Double valorReferencia;

    @Column(name = "unidadmedida")
    private String unidadMedida;

}
