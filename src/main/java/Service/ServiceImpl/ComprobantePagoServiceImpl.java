package Service.ServiceImpl;

import Entity.ComprobantePagoEntity;
import Entity.Emuns.EstadoPagoEnum;
import Entity.PacienteEntity;
import Repository.IComprobantePagoRepository;
import Repository.IPacienteRepository;
import Service.IComprobantePagoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ComprobantePagoServiceImpl implements IComprobantePagoService {

    private final IComprobantePagoRepository comprobantePagoRepository;
    private final IPacienteRepository pacienteRepository;

    public ComprobantePagoServiceImpl(IComprobantePagoRepository comprobantePagoRepository,
                                      IPacienteRepository pacienteRepository) {
        this.comprobantePagoRepository = comprobantePagoRepository;
        this.pacienteRepository = pacienteRepository;
    }

    @Override
    public List<ComprobantePagoEntity> listarTodos() {
        return comprobantePagoRepository.findAll();
    }

    @Override
    public Optional<ComprobantePagoEntity> buscarPorId(Long codComprobante) {
        return comprobantePagoRepository.findById(codComprobante);
    }

    @Override
    public ComprobantePagoEntity guardar(ComprobantePagoEntity comprobantePago) {
        return comprobantePagoRepository.save(comprobantePago);
    }

    @Override
    public void eliminar(Long codComprobante) {
        comprobantePagoRepository.deleteById(codComprobante);
    }

    @Override
    public List<ComprobantePagoEntity> listarPorPaciente(String dniPaciente) {
        return comprobantePagoRepository.findByPaciente_Dnipaciente(dniPaciente);
    }

    @Override
    public List<ComprobantePagoEntity> listarPorFechaEmision(LocalDate fechaEmision) {
        return comprobantePagoRepository.findByFechaEmision(fechaEmision);
    }

    @Override
    public List<ComprobantePagoEntity> listarPorEstado(EstadoPagoEnum estado) {
        return comprobantePagoRepository.findByEstado(estado);
    }

    @Override
    public ComprobantePagoEntity registrarComprobante(String dniPaciente, ComprobantePagoEntity comprobantePago) {
        PacienteEntity paciente = pacienteRepository.findById(dniPaciente)
                .orElseThrow(() -> new EntityNotFoundException("No existe el paciente con DNI: " + dniPaciente));

        if (comprobantePago.getFechaEmision() == null) {
            comprobantePago.setFechaEmision(LocalDate.now());
        }

        if (comprobantePago.getEstado() == null) {
            comprobantePago.setEstado(EstadoPagoEnum.FALTA_PAGAR);
        }

        comprobantePago.setPaciente(paciente);
        return comprobantePagoRepository.save(comprobantePago);
    }

    @Override
    public ComprobantePagoEntity cancelarComprobante(Long codComprobante) {
        ComprobantePagoEntity comprobante = obtenerComprobante(codComprobante);
        comprobante.setEstado(EstadoPagoEnum.CANCELADO);
        return comprobantePagoRepository.save(comprobante);
    }

    @Override
    public ComprobantePagoEntity marcarComoFaltaPagar(Long codComprobante) {
        ComprobantePagoEntity comprobante = obtenerComprobante(codComprobante);
        comprobante.setEstado(EstadoPagoEnum.FALTA_PAGAR);
        return comprobantePagoRepository.save(comprobante);
    }

    private ComprobantePagoEntity obtenerComprobante(Long codComprobante) {
        return comprobantePagoRepository.findById(codComprobante)
                .orElseThrow(() -> new EntityNotFoundException("No existe el comprobante con código: " + codComprobante));
    }
}
