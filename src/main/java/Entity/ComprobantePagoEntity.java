package Entity;

import Entity.Emuns.EstadoPagoEnum;
import Entity.Emuns.MetodoPagoEnum;
import Entity.Emuns.TipoComprobanteEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "comprobantepago", schema = "public")
public class ComprobantePagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codcomprobante")
    private Long codcomprobante;

    @Column(name = "fechaemision", nullable = false)
    private LocalDate fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipocomprobante", nullable = false)
    private TipoComprobanteEnum tipoComprobante;

    @Column(name = "subtotal", nullable = false)
    private BigDecimal subtotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodopago", length = 20, nullable = false)
    private MetodoPagoEnum metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPagoEnum estado;

    @Column(name = "montototal", nullable = false)
    private BigDecimal montoTotal;

    @ManyToOne
    @JoinColumn(name = "dnipaciente")
    private PacienteEntity paciente;

    @OneToMany(mappedBy = "comprobantePago")
    private List<DetalleComprobanteEntity> detalleComprobantes;
}