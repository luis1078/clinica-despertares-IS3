package Service.ServiceImpl;

import Entity.Emuns.EstadoTratamientoEnum;
import Entity.HistoriaMedicaEntity;
import Entity.TratamientoEntity;
import Repository.IHistoriaMedicaRepository;
import Repository.ITratamientoRepository;
import Service.ITratamientoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TratamientoServiceImpl implements ITratamientoService {

    private final ITratamientoRepository tratamientoRepository;
    private final IHistoriaMedicaRepository historiaMedicaRepository;

    public TratamientoServiceImpl(ITratamientoRepository tratamientoRepository,
                                  IHistoriaMedicaRepository historiaMedicaRepository) {
        this.tratamientoRepository = tratamientoRepository;
        this.historiaMedicaRepository = historiaMedicaRepository;
    }

    @Override
    public List<TratamientoEntity> listarTodos() {
        return tratamientoRepository.findAll();
    }

    @Override
    public Optional<TratamientoEntity> buscarPorId(Long idTratamiento) {
        return tratamientoRepository.findById(idTratamiento);
    }

    @Override
    public TratamientoEntity guardar(TratamientoEntity tratamiento) {
        return tratamientoRepository.save(tratamiento);
    }

    @Override
    public void eliminar(Long idTratamiento) {
        tratamientoRepository.deleteById(idTratamiento);
    }

    @Override
    public List<TratamientoEntity> listarPorTipo(String tipoTratamiento) {
        return tratamientoRepository.findByTipoTratamiento(tipoTratamiento);
    }

    @Override
    public List<TratamientoEntity> listarPorEstado(EstadoTratamientoEnum estadoTratamiento) {
        return tratamientoRepository.findByEstadoTratamiento(estadoTratamiento);
    }

    @Override
    public List<TratamientoEntity> listarPorHistoriaMedica(Long codHistoriaMedica) {
        return tratamientoRepository.findByHistoriaMedica_CodHistoriaMedica(codHistoriaMedica);
    }

    @Override
    public TratamientoEntity registrarTratamiento(Long codHistoriaMedica, TratamientoEntity tratamiento) {
        HistoriaMedicaEntity historia = historiaMedicaRepository.findById(codHistoriaMedica)
                .orElseThrow(() -> new EntityNotFoundException("No existe la historia médica con código: " + codHistoriaMedica));

        tratamiento.setHistoriaMedica(historia);

        if (tratamiento.getEstadoTratamiento() == null) {
            tratamiento.setEstadoTratamiento(EstadoTratamientoEnum.ACTIVO);
        }

        return tratamientoRepository.save(tratamiento);
    }

    @Override
    public TratamientoEntity finalizarTratamiento(Long idTratamiento) {
        TratamientoEntity tratamiento = obtenerTratamiento(idTratamiento);
        tratamiento.setEstadoTratamiento(EstadoTratamientoEnum.FINALIZADO);
        return tratamientoRepository.save(tratamiento);
    }

    @Override
    public TratamientoEntity suspenderTratamiento(Long idTratamiento) {
        TratamientoEntity tratamiento = obtenerTratamiento(idTratamiento);
        tratamiento.setEstadoTratamiento(EstadoTratamientoEnum.SUSPENDIDO);
        return tratamientoRepository.save(tratamiento);
    }

    private TratamientoEntity obtenerTratamiento(Long idTratamiento) {
        return tratamientoRepository.findById(idTratamiento)
                .orElseThrow(() -> new EntityNotFoundException("No existe el tratamiento con ID: " + idTratamiento));
    }
}
