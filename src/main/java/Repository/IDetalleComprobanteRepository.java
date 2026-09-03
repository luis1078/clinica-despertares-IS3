package Repository;

import Entity.DetalleComprobanteEntity;
import Entity.Emuns.TipoItemEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IDetalleComprobanteRepository extends JpaRepository<DetalleComprobanteEntity, Long> {

    List<DetalleComprobanteEntity> findByComprobantePago_Codcomprobante(Long codComprobante);

    List<DetalleComprobanteEntity> findByTipoItem(TipoItemEnum tipoItem);

    List<DetalleComprobanteEntity> findByIdReferencia(Long idReferencia);

    boolean existsByTipoItemAndIdReferencia(TipoItemEnum tipoItem, Long idReferencia);
}
