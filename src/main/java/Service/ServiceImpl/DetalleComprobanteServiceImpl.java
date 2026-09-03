package Service.ServiceImpl;

import Entity.ComprobantePagoEntity;
import Entity.DetalleComprobanteEntity;
import Entity.Emuns.TipoItemEnum;
import Entity.MedicamentoEntity;
import Repository.IComprobantePagoRepository;
import Repository.IDetalleComprobanteRepository;
import Repository.IMedicamentoRepository;
import Service.IDetalleComprobanteService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class DetalleComprobanteServiceImpl implements IDetalleComprobanteService {

    private final IDetalleComprobanteRepository detalleComprobanteRepository;
    private final IComprobantePagoRepository comprobantePagoRepository;
    private final IMedicamentoRepository medicamentoRepository;

    public DetalleComprobanteServiceImpl(IDetalleComprobanteRepository detalleComprobanteRepository,
                                         IComprobantePagoRepository comprobantePagoRepository,
                                         IMedicamentoRepository medicamentoRepository) {
        this.detalleComprobanteRepository = detalleComprobanteRepository;
        this.comprobantePagoRepository = comprobantePagoRepository;
        this.medicamentoRepository = medicamentoRepository;
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
    @Transactional
    public DetalleComprobanteEntity guardar(DetalleComprobanteEntity detalleComprobante) {

        if (detalleComprobante.getIddetalle() == null) {
            validarComprobante(detalleComprobante);
            validarReferenciaUnica(detalleComprobante);
            calcularSubtotal(detalleComprobante);
            DetalleComprobanteEntity detalleGuardado = detalleComprobanteRepository.save(detalleComprobante);
            recalcularTotalComprobante(detalleGuardado.getComprobantePago());
            return detalleGuardado;
        }

        DetalleComprobanteEntity detalleAnterior = detalleComprobanteRepository.findById(detalleComprobante.getIddetalle())
                .orElseThrow(() -> new EntityNotFoundException("No existe el detalle con código: " + detalleComprobante.getIddetalle()));

        TipoItemEnum tipoAnterior = detalleAnterior.getTipoItem();
        Long referenciaAnterior = detalleAnterior.getIdReferencia();
        int cantidadAnterior = detalleAnterior.getCantidad();
        ComprobantePagoEntity comprobanteAnterior = detalleAnterior.getComprobantePago();

        validarComprobante(detalleComprobante);
        validarReferenciaUnica(detalleComprobante);
        calcularSubtotal(detalleComprobante);

        ajustarStockPorEdicion(detalleComprobante, tipoAnterior, referenciaAnterior, cantidadAnterior);

        DetalleComprobanteEntity detalleGuardado = detalleComprobanteRepository.save(detalleComprobante);

        if (comprobanteAnterior != null
                && detalleGuardado.getComprobantePago() != null
                && !comprobanteAnterior.getCodcomprobante().equals(detalleGuardado.getComprobantePago().getCodcomprobante())) {
            recalcularTotalComprobante(comprobanteAnterior);
        }
        recalcularTotalComprobante(detalleGuardado.getComprobantePago());

        return detalleGuardado;
    }

    @Override
    @Transactional
    public void eliminar(Long idDetalle) {

        DetalleComprobanteEntity detalle = detalleComprobanteRepository.findById(idDetalle)
                .orElseThrow(() -> new EntityNotFoundException("No existe el detalle con código: " + idDetalle));

        if (detalle.getTipoItem() == TipoItemEnum.MEDICAMENTO) {
            devolverStock(detalle.getIdReferencia(), detalle.getCantidad());
        }

        ComprobantePagoEntity comprobante = detalle.getComprobantePago();
        detalleComprobanteRepository.deleteById(idDetalle);
        recalcularTotalComprobante(comprobante);
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
    @Transactional
    public DetalleComprobanteEntity registrarDetallePorCita(Long codComprobante,
                                                            Long codCitaMedica,
                                                            DetalleComprobanteEntity detalleComprobante) {
        return registrarDetalle(codComprobante, TipoItemEnum.CITA, codCitaMedica, detalleComprobante, false);
    }

    @Override
    @Transactional
    public DetalleComprobanteEntity registrarDetallePorExamen(Long codComprobante,
                                                              Long codExamenMedico,
                                                              DetalleComprobanteEntity detalleComprobante) {
        return registrarDetalle(codComprobante, TipoItemEnum.EXAMEN, codExamenMedico, detalleComprobante, false);
    }

    @Override
    @Transactional
    public DetalleComprobanteEntity registrarDetallePorMedicamento(Long codComprobante,
                                                                   Long codMedicamento,
                                                                   DetalleComprobanteEntity detalleComprobante) {
        return registrarDetalle(codComprobante, TipoItemEnum.MEDICAMENTO, codMedicamento, detalleComprobante, true);
    }

    private DetalleComprobanteEntity registrarDetalle(Long codComprobante,
                                                      TipoItemEnum tipoItem,
                                                      Long idReferencia,
                                                      DetalleComprobanteEntity detalleComprobante,
                                                      boolean descuentaStock) {

        if (detalleComprobanteRepository.existsByTipoItemAndIdReferencia(tipoItem, idReferencia)
                && tipoItem != TipoItemEnum.MEDICAMENTO) {
            throw new IllegalArgumentException("Ya existe un detalle registrado para el ítem seleccionado.");
        }

        ComprobantePagoEntity comprobante = comprobantePagoRepository.findById(codComprobante)
                .orElseThrow(() -> new EntityNotFoundException("No existe el comprobante con código: " + codComprobante));

        detalleComprobante.setComprobantePago(comprobante);
        detalleComprobante.setTipoItem(tipoItem);
        detalleComprobante.setIdReferencia(idReferencia);

        if (descuentaStock) {
            descontarStock(idReferencia, detalleComprobante.getCantidad());
        }

        calcularSubtotal(detalleComprobante);

        DetalleComprobanteEntity detalleGuardado = detalleComprobanteRepository.save(detalleComprobante);
        recalcularTotalComprobante(comprobante);

        return detalleGuardado;
    }

    private void validarComprobante(DetalleComprobanteEntity detalleComprobante) {
        if (detalleComprobante.getComprobantePago() == null
                || detalleComprobante.getComprobantePago().getCodcomprobante() == null) {
            throw new IllegalArgumentException("Debe seleccionar un comprobante de pago.");
        }
    }

    private void validarReferenciaUnica(DetalleComprobanteEntity detalleComprobante) {
        if (detalleComprobante.getTipoItem() == TipoItemEnum.MEDICAMENTO) {
            return;
        }

        boolean existeOtroDetalle = detalleComprobanteRepository.findByTipoItem(detalleComprobante.getTipoItem())
                .stream()
                .anyMatch(detalle -> detalle.getIdReferencia().equals(detalleComprobante.getIdReferencia())
                        && !detalle.getIddetalle().equals(detalleComprobante.getIddetalle()));

        if (existeOtroDetalle) {
            throw new IllegalArgumentException("Ya existe un detalle registrado para el ítem seleccionado.");
        }
    }

    private void calcularSubtotal(DetalleComprobanteEntity detalleComprobante) {

        if (detalleComprobante.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }

        if (detalleComprobante.getPrecioUnitario() == null) {
            detalleComprobante.setPrecioUnitario(BigDecimal.ZERO);
        }

        BigDecimal cantidad = BigDecimal.valueOf(detalleComprobante.getCantidad());
        detalleComprobante.setSubtotal(detalleComprobante.getPrecioUnitario().multiply(cantidad));
    }

    private void recalcularTotalComprobante(ComprobantePagoEntity comprobante) {
        if (comprobante == null || comprobante.getCodcomprobante() == null) {
            return;
        }

        BigDecimal total = detalleComprobanteRepository.findByComprobantePago_Codcomprobante(comprobante.getCodcomprobante())
                .stream()
                .filter(detalle -> detalle.getIddetalle() != null)
                .map(this::obtenerSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        comprobante.setSubtotal(total);
        comprobante.setMontoTotal(total);
        comprobantePagoRepository.save(comprobante);
    }

    private BigDecimal obtenerSubtotal(DetalleComprobanteEntity detalle) {
        return detalle.getSubtotal() == null ? BigDecimal.ZERO : detalle.getSubtotal();
    }

    private void ajustarStockPorEdicion(DetalleComprobanteEntity nuevoDetalle,
                                        TipoItemEnum tipoAnterior,
                                        Long referenciaAnterior,
                                        int cantidadAnterior) {

        if (tipoAnterior == TipoItemEnum.MEDICAMENTO
                && nuevoDetalle.getTipoItem() == TipoItemEnum.MEDICAMENTO
                && referenciaAnterior.equals(nuevoDetalle.getIdReferencia())) {

            int diferencia = nuevoDetalle.getCantidad() - cantidadAnterior;

            if (diferencia > 0) {
                descontarStock(nuevoDetalle.getIdReferencia(), diferencia);
            } else if (diferencia < 0) {
                devolverStock(nuevoDetalle.getIdReferencia(), Math.abs(diferencia));
            }

            return;
        }

        if (tipoAnterior == TipoItemEnum.MEDICAMENTO) {
            devolverStock(referenciaAnterior, cantidadAnterior);
        }

        if (nuevoDetalle.getTipoItem() == TipoItemEnum.MEDICAMENTO) {
            descontarStock(nuevoDetalle.getIdReferencia(), nuevoDetalle.getCantidad());
        }
    }

    private void descontarStock(Long codMedicamento, int cantidad) {

        MedicamentoEntity medicamento = medicamentoRepository.findById(codMedicamento)
                .orElseThrow(() -> new EntityNotFoundException("No existe el medicamento con código: " + codMedicamento));

        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }

        if (medicamento.getStockInventario() < cantidad) {
            throw new IllegalArgumentException("Stock insuficiente para el medicamento seleccionado.");
        }

        medicamento.setStockInventario(medicamento.getStockInventario() - cantidad);
        medicamentoRepository.save(medicamento);
    }

    private void devolverStock(Long codMedicamento, int cantidad) {

        MedicamentoEntity medicamento = medicamentoRepository.findById(codMedicamento)
                .orElseThrow(() -> new EntityNotFoundException("No existe el medicamento con código: " + codMedicamento));

        medicamento.setStockInventario(medicamento.getStockInventario() + cantidad);
        medicamentoRepository.save(medicamento);
    }
}
