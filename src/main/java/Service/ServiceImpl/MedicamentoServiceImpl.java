package Service.ServiceImpl;

import Entity.MedicamentoEntity;
import Entity.ProveedorEntity;
import Repository.IMedicamentoRepository;
import Repository.IProveedorRepository;
import Service.IMedicamentoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MedicamentoServiceImpl implements IMedicamentoService {

    private final IMedicamentoRepository medicamentoRepository;
    private final IProveedorRepository proveedorRepository;

    public MedicamentoServiceImpl(IMedicamentoRepository medicamentoRepository,
                                  IProveedorRepository proveedorRepository) {
        this.medicamentoRepository = medicamentoRepository;
        this.proveedorRepository = proveedorRepository;
    }

    @Override
    public List<MedicamentoEntity> listarTodos() {
        return medicamentoRepository.findAll();
    }

    @Override
    public Optional<MedicamentoEntity> buscarPorId(Long codMedicamento) {
        return medicamentoRepository.findById(codMedicamento);
    }

    @Override
    public MedicamentoEntity guardar(MedicamentoEntity medicamento) {
        return medicamentoRepository.save(medicamento);
    }

    @Override
    public void eliminar(Long codMedicamento) {
        medicamentoRepository.deleteById(codMedicamento);
    }

    @Override
    public Optional<MedicamentoEntity> buscarPorNombreMedicamento(String nombreMedicamento) {
        return medicamentoRepository.findByNombreMedicamento(nombreMedicamento);
    }

    @Override
    public List<MedicamentoEntity> buscarPorNombre(String nombreMedicamento) {
        return medicamentoRepository.findByNombreMedicamentoContainingIgnoreCase(nombreMedicamento);
    }

    @Override
    public List<MedicamentoEntity> listarPorStockMenorA(int stockInventario) {
        return medicamentoRepository.findByStockInventarioLessThan(stockInventario);
    }

    @Override
    public List<MedicamentoEntity> listarPorFechaVencimientoAntesDe(LocalDate fecha) {
        return medicamentoRepository.findByFechaVencimientoBefore(fecha);
    }

    @Override
    public List<MedicamentoEntity> listarPorProveedor(String rucProveedor) {
        return medicamentoRepository.findByProveedor_RucProveedor(rucProveedor);
    }

    @Override
    public MedicamentoEntity registrarMedicamento(String rucProveedor, MedicamentoEntity medicamento) {
        ProveedorEntity proveedor = proveedorRepository.findById(rucProveedor)
                .orElseThrow(() -> new EntityNotFoundException("No existe el proveedor con RUC: " + rucProveedor));

        medicamento.setProveedor(proveedor);
        return medicamentoRepository.save(medicamento);
    }

    @Override
    public MedicamentoEntity actualizarStock(Long codMedicamento, int nuevaCantidad) {
        if (nuevaCantidad < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo.");
        }

        MedicamentoEntity medicamento = obtenerMedicamento(codMedicamento);
        medicamento.setStockInventario(nuevaCantidad);

        return medicamentoRepository.save(medicamento);
    }

    @Override
    public MedicamentoEntity descontarStock(Long codMedicamento, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a descontar debe ser mayor a cero.");
        }

        MedicamentoEntity medicamento = obtenerMedicamento(codMedicamento);

        if (medicamento.getStockInventario() < cantidad) {
            throw new IllegalArgumentException("No hay stock suficiente para realizar el despacho.");
        }

        medicamento.setStockInventario(medicamento.getStockInventario() - cantidad);

        return medicamentoRepository.save(medicamento);
    }

    private MedicamentoEntity obtenerMedicamento(Long codMedicamento) {
        return medicamentoRepository.findById(codMedicamento)
                .orElseThrow(() -> new EntityNotFoundException("No existe el medicamento con código: " + codMedicamento));
    }
}
