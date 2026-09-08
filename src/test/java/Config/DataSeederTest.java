package Config;

import Entity.ProveedorEntity;
import Repository.IMedicamentoRepository;
import Repository.IMedicoRepository;
import Repository.IPacienteRepository;
import Repository.IProveedorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DataSeeder es lo único que hace posible registrar un usuario con rol
 * MEDICO en una base recién creada (el registro pide un médico existente).
 * Cubre las dos ramas que importan: siembra en una base vacía, y no toca
 * nada si ya hay datos propios.
 */
@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private IMedicoRepository medicoRepository;
    @Mock
    private IProveedorRepository proveedorRepository;
    @Mock
    private IPacienteRepository pacienteRepository;
    @Mock
    private IMedicamentoRepository medicamentoRepository;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void baseVacia_siembraMedicosProveedoresPacientesYMedicamentos() {
        when(proveedorRepository.saveAll(any())).thenAnswer(invocacion -> List.of(
                proveedorConRuc("20123456789"), proveedorConRuc("20234567890"), proveedorConRuc("20345678901")));

        dataSeeder.run();

        verify(medicoRepository).saveAll(any());
        verify(proveedorRepository).saveAll(any());
        verify(pacienteRepository).saveAll(any());
        verify(medicamentoRepository).saveAll(any());
    }

    @Test
    void baseConDatosPropios_noSiembraNadaEnNingunaTabla() {
        when(medicoRepository.count()).thenReturn(3L);
        when(proveedorRepository.count()).thenReturn(1L);
        when(pacienteRepository.count()).thenReturn(10L);
        when(medicamentoRepository.count()).thenReturn(5L);

        dataSeeder.run();

        verify(medicoRepository, never()).saveAll(any());
        verify(proveedorRepository, never()).saveAll(any());
        verify(pacienteRepository, never()).saveAll(any());
        verify(medicamentoRepository, never()).saveAll(any());
        verify(proveedorRepository, times(1)).findAll();
    }

    private ProveedorEntity proveedorConRuc(String ruc) {
        ProveedorEntity proveedor = new ProveedorEntity();
        proveedor.setRucProveedor(ruc);
        return proveedor;
    }
}
