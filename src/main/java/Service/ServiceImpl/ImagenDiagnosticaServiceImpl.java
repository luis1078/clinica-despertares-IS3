package Service.ServiceImpl;

import Entity.DiagnosticoEntity;
import Entity.Emuns.EstadoExamenMedicoEnum;
import Entity.Emuns.TipoExamenEnum;
import Entity.ImagenDiagnosticaEntity;
import Repository.IDiagnosticoRepository;
import Repository.IImagenDiagnosticaRepository;
import Service.IImagenDiagnosticaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ImagenDiagnosticaServiceImpl implements IImagenDiagnosticaService {

    private final IImagenDiagnosticaRepository imagenDiagnosticaRepository;
    private final IDiagnosticoRepository diagnosticoRepository;

    public ImagenDiagnosticaServiceImpl(IImagenDiagnosticaRepository imagenDiagnosticaRepository,
                                        IDiagnosticoRepository diagnosticoRepository) {
        this.imagenDiagnosticaRepository = imagenDiagnosticaRepository;
        this.diagnosticoRepository = diagnosticoRepository;
    }

    @Override
    public List<ImagenDiagnosticaEntity> listarTodos() {
        return imagenDiagnosticaRepository.findAll();
    }

    @Override
    public Optional<ImagenDiagnosticaEntity> buscarPorId(Long codExamenMedico) {
        return imagenDiagnosticaRepository.findById(codExamenMedico);
    }

    @Override
    public ImagenDiagnosticaEntity guardar(ImagenDiagnosticaEntity imagenDiagnostica) {
        return imagenDiagnosticaRepository.save(imagenDiagnostica);
    }

    @Override
    public void eliminar(Long codExamenMedico) {
        imagenDiagnosticaRepository.deleteById(codExamenMedico);
    }

    @Override
    public List<ImagenDiagnosticaEntity> listarPorRegionCuerpo(String regionCuerpo) {
        return imagenDiagnosticaRepository.findByRegionCuerpo(regionCuerpo);
    }

    @Override
    public List<ImagenDiagnosticaEntity> listarPorUsoContraste(boolean usaContraste) {
        return imagenDiagnosticaRepository.findByUsaContraste(usaContraste);
    }

    @Override
    public List<ImagenDiagnosticaEntity> buscarPorNombreImagen(String nombreImagen) {
        return imagenDiagnosticaRepository.findByNombreImagenContainingIgnoreCase(nombreImagen);
    }

    @Override
    public List<ImagenDiagnosticaEntity> buscarImagenes(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return imagenDiagnosticaRepository.findAll();
        }

        return imagenDiagnosticaRepository.buscarPorTexto(texto.trim());
    }

    @Override
    public ImagenDiagnosticaEntity registrarImagenDiagnostica(Long idDiagnostico,
                                                             ImagenDiagnosticaEntity imagenDiagnostica) {
        DiagnosticoEntity diagnostico = diagnosticoRepository.findById(idDiagnostico)
                .orElseThrow(() -> new EntityNotFoundException("No existe el diagnóstico con ID: " + idDiagnostico));

        if (imagenDiagnostica.getFechaExamen() == null) {
            imagenDiagnostica.setFechaExamen(LocalDate.now());
        }

        imagenDiagnostica.setTipoExamen(TipoExamenEnum.IMAGEN);
        imagenDiagnostica.setEstadoExamenMedico(EstadoExamenMedicoEnum.PENDIENTE);
        imagenDiagnostica.setDiagnostico(diagnostico);

        return imagenDiagnosticaRepository.save(imagenDiagnostica);
    }

    @Override
    public ImagenDiagnosticaEntity registrarResultadoImagenDiagnostica(Long codExamenMedico,
                                                                      ImagenDiagnosticaEntity datosResultado) {
        ImagenDiagnosticaEntity imagen = imagenDiagnosticaRepository.findById(codExamenMedico)
                .orElseThrow(() -> new EntityNotFoundException("No existe la imagen diagnóstica con código: " + codExamenMedico));

        imagen.setNombreImagen(datosResultado.getNombreImagen());
        imagen.setRegionCuerpo(datosResultado.getRegionCuerpo());
        imagen.setInformeMedico(datosResultado.getInformeMedico());
        imagen.setUsaContraste(datosResultado.isUsaContraste());
        imagen.setTipoContraste(datosResultado.getTipoContraste());
        imagen.setObservaciones(datosResultado.getObservaciones());
        imagen.setFechaResultado(LocalDate.now());
        imagen.setEstadoExamenMedico(EstadoExamenMedicoEnum.FINALIZADO);

        return imagenDiagnosticaRepository.save(imagen);
    }
}
