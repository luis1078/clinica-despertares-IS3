package Service.ServiceImpl;

import Entity.ProveedorEntity;
import Repository.IProveedorRepository;
import Service.IProveedorService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProveedorServiceImpl implements IProveedorService {

    private final IProveedorRepository proveedorRepository;

    public ProveedorServiceImpl(IProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @Override
    public List<ProveedorEntity> listarTodos() {
        return proveedorRepository.findAll();
    }

    @Override
    public Optional<ProveedorEntity> buscarPorId(String rucProveedor) {
        return proveedorRepository.findById(rucProveedor);
    }

    @Override
    public ProveedorEntity guardar(ProveedorEntity proveedor) {
        return proveedorRepository.save(proveedor);
    }

    @Override
    public void eliminar(String rucProveedor) {
        proveedorRepository.deleteById(rucProveedor);
    }

    @Override
    public Optional<ProveedorEntity> buscarPorNombreProveedor(String nombreProveedor) {
        return proveedorRepository.findByNombreProveedor(nombreProveedor);
    }

    @Override
    public List<ProveedorEntity> buscarPorNombre(String nombreProveedor) {
        return proveedorRepository.findByNombreProveedorContainingIgnoreCase(nombreProveedor);
    }

    @Override
    public boolean existePorNombreProveedor(String nombreProveedor) {
        return proveedorRepository.existsByNombreProveedor(nombreProveedor);
    }
}
