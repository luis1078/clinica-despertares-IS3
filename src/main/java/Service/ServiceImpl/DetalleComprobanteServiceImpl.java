package Service.ServiceImpl;

import Entity.ComprobantePagoEntity;
import Entity.DetalleComprobanteEntity;
import Entity.Emuns.TipoItemEnum;
import Repository.IComprobantePagoRepository;
import Repository.IDetalleComprobanteRepository;
import Service.IDetalleComprobanteService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class DetalleComprobanteServiceImpl implements IDetalleComprobanteService {

    private final IDetalleComprobanteRepository detalleComprobanteRepository;
    private final IComprobantePagoRepository comprobantePagoRepository;

    public DetalleComprobanteServiceImpl(IDetalleComprobanteRepository detalleComprobanteRepository,
                                         IComprobantePagoRepository comprobantePagoRepository) {
        this.detalleComprobanteRepository = detalleComprobanteRepository;
        this.comprobantePagoRepository = comprobantePagoRepository;
    }

    @Override
    public List<DetalleComprobanteEntity> listarTodos() {
        return detalleComprobanteRepository.findAll();
    }

    @Override
    public Optional<DetalleComprobanteEntity> buscarPorId(Long idDetalle) {
        return detalleComprobanteRepository.findById(idDetalle);
    }

    @Override
    public DetalleComprobanteEntity guardar(DetalleComprobanteEntity detalleComprobante) {
        calcularSubtotal(detalleComprobante);
        return detalleComprobanteRepository.save(detalleComprobante);
    }

    @Override
    public void eliminar(Long idDetalle) {
        detalleComprobanteRepository.deleteById(idDetalle);
    }

    @Override
    public List<DetalleComprobanteEntity> listarPorComprobante(Long codComprobante) {
        return detalleComprobanteRepository.findByComprobantePago_Codcomprobante(codComprobante);
    }

    @Override
    public List<DetalleComprobanteEntity> listarPorTipoItem(TipoItemEnum tipoItem) {
        return detalleComprobanteRepository.findByTipoItem(tipoItem);
    }

    @Override
    public List<DetalleComprobanteEntity> listarPorIdReferencia(Long idReferencia) {
        return detalleComprobanteRepository.findByIdReferencia(idReferencia);
    }

    @Override
    public DetalleComprobanteEntity registrarDetallePorCita(Long codComprobante,
                                                            Long codCitaMedica,
                                                            DetalleComprobanteEntity detalleComprobante) {
        return registrarDetalle(codComprobante, TipoItemEnum.CITA, codCitaMedica, detalleComprobante);
    }

    @Override
    public DetalleComprobanteEntity registrarDetallePorExamen(Long codComprobante,
                                                              Long codExamenMedico,
                                                              DetalleComprobanteEntity detalleComprobante) {
        return registrarDetalle(codComprobante, TipoItemEnum.EXAMEN, codExamenMedico, detalleComprobante);
    }

    @Override
    public DetalleComprobanteEntity registrarDetallePorMedicamento(Long codComprobante,
                                                                   Long codMedicamento,
                                                                   DetalleComprobanteEntity detalleComprobante) {
        return registrarDetalle(codComprobante, TipoItemEnum.MEDICAMENTO, codMedicamento, detalleComprobante);
    }

    private DetalleComprobanteEntity registrarDetalle(Long codComprobante,
                                                      TipoItemEnum tipoItem,
                                                      Long idReferencia,
                                                      DetalleComprobanteEntity detalleComprobante) {
        ComprobantePagoEntity comprobante = comprobantePagoRepository.findById(codComprobante)
                .orElseThrow(() -> new EntityNotFoundException("No existe el comprobante con código: " + codComprobante));

        detalleComprobante.setComprobantePago(comprobante);
        detalleComprobante.setTipoItem(tipoItem);
        detalleComprobante.setIdReferencia(idReferencia);

        calcularSubtotal(detalleComprobante);

        return detalleComprobanteRepository.save(detalleComprobante);
    }

    private void calcularSubtotal(DetalleComprobanteEntity detalleComprobante) {
        if (detalleComprobante.getPrecioUnitario() != null) {
            BigDecimal cantidad = BigDecimal.valueOf(detalleComprobante.getCantidad());
            detalleComprobante.setSubtotal(detalleComprobante.getPrecioUnitario().multiply(cantidad));
        }
    }
}
