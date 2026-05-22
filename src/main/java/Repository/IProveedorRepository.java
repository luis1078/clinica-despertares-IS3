package Repository;

import Entity.ProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IProveedorRepository extends JpaRepository<ProveedorEntity, String> {
    Optional<ProveedorEntity> findByRucProveedor(String ruc);

    List<ProveedorEntity> findByNombreProveedorContainingIgnoreCase(String nombre);

    boolean existsByRucProveedor(String ruc);
}
