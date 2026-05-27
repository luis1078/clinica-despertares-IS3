package Service;

import Entity.ProveedorEntity;

import java.util.List;
import java.util.Optional;

public interface IProveedorService {

    List<ProveedorEntity> listarTodos();

    Optional<ProveedorEntity> buscarPorId(String rucProveedor);

    ProveedorEntity guardar(ProveedorEntity proveedor);

    void eliminar(String rucProveedor);

    Optional<ProveedorEntity> buscarPorNombreProveedor(String nombreProveedor);

    List<ProveedorEntity> buscarPorNombre(String nombreProveedor);

    boolean existePorNombreProveedor(String nombreProveedor);
}
