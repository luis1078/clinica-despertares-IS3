package Repository;

import Entity.ImagenDiagnosticaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface IImagenDiagnosticaRepository extends JpaRepository<ImagenDiagnosticaEntity, Long> {

    List<ImagenDiagnosticaEntity> findByRegionCuerpo(String regionCuerpo);

    List<ImagenDiagnosticaEntity> findByUsaContraste(boolean usaContraste);

    List<ImagenDiagnosticaEntity> findByNombreImagenContainingIgnoreCase(String nombreImagen);
}
