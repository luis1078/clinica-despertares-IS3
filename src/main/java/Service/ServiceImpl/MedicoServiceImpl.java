package Service.ServiceImpl;

import Entity.MedicoEntity;
import Repository.IMedicoRepository;
import Service.IMedicoService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MedicoServiceImpl implements IMedicoService {

    private final IMedicoRepository medicoRepository;

    public MedicoServiceImpl(IMedicoRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    @Override
    public List<MedicoEntity> listarTodos() {
        return medicoRepository.findAll();
    }

    @Override
    public Optional<MedicoEntity> buscarPorId(Long idMedico) {
        return medicoRepository.findById(idMedico);
    }

    @Override
    public MedicoEntity guardar(MedicoEntity medico) {
        return medicoRepository.save(medico);
    }

    @Override
    public void eliminar(Long idMedico) {
        medicoRepository.deleteById(idMedico);
    }

    @Override
    public Optional<MedicoEntity> buscarPorCmpMedico(String cmpMedico) {
        return medicoRepository.findByCmpMedico(cmpMedico);
    }

    @Override
    public List<MedicoEntity> buscarPorEspecialidad(String especialidad) {
        return medicoRepository.findByEspecialidad(especialidad);
    }

    @Override
    public List<MedicoEntity> buscarPorNombre(String nombreMedico) {
        return medicoRepository.findByNombreMedicoContainingIgnoreCase(nombreMedico);
    }

    @Override
    public boolean existePorCmpMedico(String cmpMedico) {
        return medicoRepository.existsByCmpMedico(cmpMedico);
    }

    @Override
    public boolean existePorTelefono(String telefono) {
        return medicoRepository.existsByTelefono(telefono);
    }
}
