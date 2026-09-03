package Service;

import Entity.DetalleComprobanteEntity;
import Entity.Emuns.TipoItemEnum;

import java.util.List;
import java.util.Optional;

public interface IDetalleComprobanteService {

    List<DetalleComprobanteEntity> listarTodos();

    Optional<DetalleComprobanteEntity> buscarPorId(Long idDetalle);

    DetalleComprobanteEntity guardar(DetalleComprobanteEntity detalleComprobante);

    void eliminar(Long idDetalle);

    List<DetalleComprobanteEntity> listarPorComprobante(Long codComprobante);

    List<DetalleComprobanteEntity> listarPorTipoItem(TipoItemEnum tipoItem);

    List<DetalleComprobanteEntity> listarPorIdReferencia(Long idReferencia);

    DetalleComprobanteEntity registrarDetallePorCita(Long codComprobante,
                                                     Long codCitaMedica,
                                                     DetalleComprobanteEntity detalleComprobante);

    DetalleComprobanteEntity registrarDetallePorExamen(Long codComprobante,
                                                       Long codExamenMedico,
                                                       DetalleComprobanteEntity detalleComprobante);

    DetalleComprobanteEntity registrarDetallePorMedicamento(Long codComprobante,
                                                            Long codMedicamento,
                                                            DetalleComprobanteEntity detalleComprobante);
}
