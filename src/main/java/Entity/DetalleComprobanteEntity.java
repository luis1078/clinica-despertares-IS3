package Entity;

import Entity.Emuns.TipoItemEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "detallecomprobante", schema = "public")

public class DetalleComprobanteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "iddetalle")
    private Long iddetalle;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipoitem", nullable = false)
    private TipoItemEnum tipoItem;

    @Column(name = "idreferencia", length = 20, nullable = false)
    private Long idReferencia;

    @Column(name = "descripciondetalle", columnDefinition = "TEXT", nullable = false)
    private String descripcionDetalle;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "preciounitario", nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", nullable = false)
    private BigDecimal subtotal;

    @ManyToOne
    @JoinColumn(name = "codcomprobante")
    private ComprobantePagoEntity comprobantePago;
}
