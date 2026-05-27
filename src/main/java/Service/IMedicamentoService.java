package Service;

import Entity.MedicamentoEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IMedicamentoService {

    List<MedicamentoEntity> listarTodos();

    Optional<MedicamentoEntity> buscarPorId(Long codMedicamento);

    MedicamentoEntity guardar(MedicamentoEntity medicamento);

    void eliminar(Long codMedicamento);

    Optional<MedicamentoEntity> buscarPorNombreMedicamento(String nombreMedicamento);

    List<MedicamentoEntity> buscarPorNombre(String nombreMedicamento);

    List<MedicamentoEntity> listarPorStockMenorA(int stockInventario);

    List<MedicamentoEntity> listarPorFechaVencimientoAntesDe(LocalDate fecha);

    List<MedicamentoEntity> listarPorProveedor(String rucProveedor);

    MedicamentoEntity registrarMedicamento(String rucProveedor, MedicamentoEntity medicamento);

    MedicamentoEntity actualizarStock(Long codMedicamento, int nuevaCantidad);

    MedicamentoEntity descontarStock(Long codMedicamento, int cantidad);
}
