package Service.ServiceImpl;

import Entity.DetalleTratamientoEntity;
import Entity.MedicamentoEntity;
import Entity.TratamientoEntity;
import Repository.IDetalleTratamientoRepository;
import Repository.IMedicamentoRepository;
import Repository.ITratamientoRepository;
import Service.IDetalleTratamientoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DetalleTratamientoServiceImpl implements IDetalleTratamientoService {

    private final IDetalleTratamientoRepository detalleTratamientoRepository;
    private final ITratamientoRepository tratamientoRepository;
    private final IMedicamentoRepository medicamentoRepository;

    public DetalleTratamientoServiceImpl(IDetalleTratamientoRepository detalleTratamientoRepository,
                                         ITratamientoRepository tratamientoRepository,
                                         IMedicamentoRepository medicamentoRepository) {
        this.detalleTratamientoRepository = detalleTratamientoRepository;
        this.tratamientoRepository = tratamientoRepository;
        this.medicamentoRepository = medicamentoRepository;
    }

    @Override
    public List<DetalleTratamientoEntity> listarTodos() {
        return detalleTratamientoRepository.findAll();
    }

    @Override
    public Optional<DetalleTratamientoEntity> buscarPorId(Long idDetalleTratamiento) {
        return detalleTratamientoRepository.findById(idDetalleTratamiento);
    }

    @Override
    public DetalleTratamientoEntity guardar(DetalleTratamientoEntity detalleTratamiento) {
        return detalleTratamientoRepository.save(detalleTratamiento);
    }

    @Override
    public void eliminar(Long idDetalleTratamiento) {
        detalleTratamientoRepository.deleteById(idDetalleTratamiento);
    }

    @Override
    public List<DetalleTratamientoEntity> listarPorTratamiento(Long idTratamiento) {
        return detalleTratamientoRepository.findByTratamiento_IdTratamiento(idTratamiento);
    }

    @Override
    public List<DetalleTratamientoEntity> listarPorMedicamento(Long codMedicamento) {
        return detalleTratamientoRepository.findByMedicamento_CodMedicamento(codMedicamento);
    }

    @Override
    public DetalleTratamientoEntity registrarDetalleTratamiento(Long idTratamiento,
                                                                Long codMedicamento,
                                                                DetalleTratamientoEntity detalleTratamiento) {
        TratamientoEntity tratamiento = tratamientoRepository.findById(idTratamiento)
                .orElseThrow(() -> new EntityNotFoundException("No existe el tratamiento con ID: " + idTratamiento));

        MedicamentoEntity medicamento = medicamentoRepository.findById(codMedicamento)
                .orElseThrow(() -> new EntityNotFoundException("No existe el medicamento con código: " + codMedicamento));

        detalleTratamiento.setTratamiento(tratamiento);
        detalleTratamiento.setMedicamento(medicamento);

        return detalleTratamientoRepository.save(detalleTratamiento);
    }
}
