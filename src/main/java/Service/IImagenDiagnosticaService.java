package Service;

import Entity.ImagenDiagnosticaEntity;

import java.util.List;
import java.util.Optional;

public interface IImagenDiagnosticaService {

    List<ImagenDiagnosticaEntity> listarTodos();

    Optional<ImagenDiagnosticaEntity> buscarPorId(Long codExamenMedico);

    ImagenDiagnosticaEntity guardar(ImagenDiagnosticaEntity imagenDiagnostica);

    void eliminar(Long codExamenMedico);

    List<ImagenDiagnosticaEntity> listarPorRegionCuerpo(String regionCuerpo);

    List<ImagenDiagnosticaEntity> listarPorUsoContraste(boolean usaContraste);

    List<ImagenDiagnosticaEntity> buscarPorNombreImagen(String nombreImagen);

    List<ImagenDiagnosticaEntity> buscarImagenes(String texto);

    ImagenDiagnosticaEntity registrarImagenDiagnostica(Long idDiagnostico, ImagenDiagnosticaEntity imagenDiagnostica);

    ImagenDiagnosticaEntity registrarResultadoImagenDiagnostica(Long codExamenMedico, ImagenDiagnosticaEntity datosResultado);
}
