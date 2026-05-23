package Repository;

import Entity.MedicamentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IMedicamentoRepository extends JpaRepository<MedicamentoEntity, Long> {
    Optional<MedicamentoEntity> findByNombreMedicamento(String nombreMedicamento);

    List<MedicamentoEntity> findByNombreMedicamentoContainingIgnoreCase(String nombreMedicamento);

    List<MedicamentoEntity> findByStockInventarioLessThan(int stockInventario);

    List<MedicamentoEntity> findByFechaVencimientoBefore(LocalDate fecha);

    List<MedicamentoEntity> findByProveedor_RucProveedor(String rucProveedor);
}
