package Config;

import Entity.Emuns.GeneroPacienteEnum;
import Entity.MedicamentoEntity;
import Entity.MedicoEntity;
import Entity.PacienteEntity;
import Entity.ProveedorEntity;
import Repository.IMedicamentoRepository;
import Repository.IMedicoRepository;
import Repository.IPacienteRepository;
import Repository.IProveedorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Carga datos de ejemplo cuando la base está vacía: médicos, proveedores,
 * pacientes y medicamentos. Sin esto no había forma de registrar un usuario
 * con rol MEDICO (el formulario de registro pide asociarlo a un médico
 * existente, y no había ninguno) ni de ver nada útil en pacientes,
 * medicamentos o citas en un ambiente recién levantado.
 *
 * Cada tabla se revisa por separado antes de sembrar, así que no duplica
 * nada si ya tiene datos propios. Se puede desactivar por completo con
 * SEED_DATA=false si en algún momento se usa esta misma imagen contra una
 * base que no debe tocarse.
 */
@Component
@ConditionalOnProperty(prefix = "app", name = "seed-data", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final IMedicoRepository medicoRepository;
    private final IProveedorRepository proveedorRepository;
    private final IPacienteRepository pacienteRepository;
    private final IMedicamentoRepository medicamentoRepository;

    public DataSeeder(IMedicoRepository medicoRepository,
                       IProveedorRepository proveedorRepository,
                       IPacienteRepository pacienteRepository,
                       IMedicamentoRepository medicamentoRepository) {
        this.medicoRepository = medicoRepository;
        this.proveedorRepository = proveedorRepository;
        this.pacienteRepository = pacienteRepository;
        this.medicamentoRepository = medicamentoRepository;
    }

    @Override
    public void run(String... args) {
        sembrarMedicos();
        List<ProveedorEntity> proveedores = sembrarProveedores();
        sembrarPacientes();
        sembrarMedicamentos(proveedores);
    }

    private void sembrarMedicos() {
        if (medicoRepository.count() > 0) {
            return;
        }

        medicoRepository.saveAll(List.of(
                medico("Carlos", "Ramírez", "987654321", "CMP12345", "Medicina General", "Lun-Vie 8:00-14:00"),
                medico("Lucía", "Fernández", "987654322", "CMP12346", "Pediatría", "Lun-Vie 9:00-15:00"),
                medico("Jorge", "Salazar", "987654323", "CMP12347", "Cardiología", "Mar-Sáb 10:00-16:00"),
                medico("Andrea", "Torres", "987654324", "CMP12348", "Ginecología", "Lun-Vie 8:00-13:00"),
                medico("Miguel", "Rojas", "987654325", "CMP12349", "Traumatología", "Lun-Sáb 14:00-20:00")
        ));
    }

    private List<ProveedorEntity> sembrarProveedores() {
        if (proveedorRepository.count() > 0) {
            return proveedorRepository.findAll();
        }

        return proveedorRepository.saveAll(List.of(
                proveedor("20123456789", "Distribuidora FarmaPeru", "contacto@farmaperu.com", "912345678", "Av. Industrial 450, Lima"),
                proveedor("20234567890", "Laboratorios San Rafael", "ventas@sanrafael.pe", "912345679", "Jr. Comercio 120, Lima"),
                proveedor("20345678901", "Suministros MedSalud", "pedidos@medsalud.pe", "912345680", "Calle Los Andes 88, Lima")
        ));
    }

    private void sembrarPacientes() {
        if (pacienteRepository.count() > 0) {
            return;
        }

        pacienteRepository.saveAll(List.of(
                paciente("71234561", "María", "González", "923456781", "maria.gonzalez@mail.com",
                        LocalDate.of(1990, 5, 14), GeneroPacienteEnum.FEMENINO, "O+", "Av. Los Olivos 234", null),
                paciente("71234562", "Pedro", "Vargas", "923456782", "pedro.vargas@mail.com",
                        LocalDate.of(1985, 11, 2), GeneroPacienteEnum.MASCULINO, "A+", "Jr. Las Flores 112", null),
                paciente("71234563", "Ana", "Mendoza", "923456783", "ana.mendoza@mail.com",
                        LocalDate.of(2001, 2, 27), GeneroPacienteEnum.FEMENINO, "B+", "Calle Sol 45", null),
                paciente("71234564", "Luis", "Chávez", "923456784", "luis.chavez@mail.com",
                        LocalDate.of(1978, 7, 19), GeneroPacienteEnum.MASCULINO, "AB+", "Av. Grau 900", "Penicilina"),
                paciente("71234565", "Carmen", "Rojas", "923456785", "carmen.rojas@mail.com",
                        LocalDate.of(1995, 3, 8), GeneroPacienteEnum.FEMENINO, "O-", "Jr. Amazonas 310", null),
                paciente("71234566", "Diego", "Flores", "923456786", "diego.flores@mail.com",
                        LocalDate.of(1999, 12, 24), GeneroPacienteEnum.MASCULINO, "A-", "Av. Perú 77", null)
        ));
    }

    private void sembrarMedicamentos(List<ProveedorEntity> proveedores) {
        if (medicamentoRepository.count() > 0 || proveedores.size() < 3) {
            return;
        }

        ProveedorEntity farmaPeru = proveedores.get(0);
        ProveedorEntity sanRafael = proveedores.get(1);
        ProveedorEntity medSalud = proveedores.get(2);

        medicamentoRepository.saveAll(List.of(
                medicamento("Paracetamol 500mg", "Analgésico y antipirético de uso general.", "Analgésico",
                        LocalDate.of(2027, 6, 30), 120, 20, farmaPeru),
                medicamento("Amoxicilina 500mg", "Antibiótico de amplio espectro.", "Antibiótico",
                        LocalDate.of(2026, 12, 15), 15, 20, sanRafael),
                medicamento("Ibuprofeno 400mg", "Antiinflamatorio no esteroideo.", "Antiinflamatorio",
                        LocalDate.of(2027, 3, 20), 80, 15, farmaPeru),
                medicamento("Loratadina 10mg", "Antihistamínico para alergias.", "Antihistamínico",
                        LocalDate.of(2026, 9, 10), 8, 10, medSalud),
                medicamento("Omeprazol 20mg", "Inhibidor de la bomba de protones.", "Gastroprotector",
                        LocalDate.of(2027, 1, 5), 60, 15, sanRafael),
                medicamento("Metformina 850mg", "Antidiabético oral.", "Antidiabético",
                        LocalDate.of(2026, 11, 30), 45, 20, medSalud)
        ));
    }

    private MedicoEntity medico(String nombre, String apellido, String telefono, String cmp, String especialidad, String horario) {
        MedicoEntity medico = new MedicoEntity();
        medico.setNombreMedico(nombre);
        medico.setApellidoMedico(apellido);
        medico.setTelefono(telefono);
        medico.setCmpMedico(cmp);
        medico.setEspecialidad(especialidad);
        medico.setHorarioAtencion(horario);
        return medico;
    }

    private ProveedorEntity proveedor(String ruc, String nombre, String correo, String contacto, String direccion) {
        ProveedorEntity proveedor = new ProveedorEntity();
        proveedor.setRucProveedor(ruc);
        proveedor.setNombreProveedor(nombre);
        proveedor.setCorreoProveedor(correo);
        proveedor.setNumeroContacto(contacto);
        proveedor.setDireccion(direccion);
        return proveedor;
    }

    private PacienteEntity paciente(String dni, String nombre, String apellido, String telefono, String correo,
                                     LocalDate fechaNacimiento, GeneroPacienteEnum genero, String tipoSangre,
                                     String direccion, String alergias) {
        PacienteEntity paciente = new PacienteEntity();
        paciente.setDnipaciente(dni);
        paciente.setNombrePaciente(nombre);
        paciente.setApellidoPaciente(apellido);
        paciente.setTelefono(telefono);
        paciente.setCorreoElectronico(correo);
        paciente.setFechaNacimiento(fechaNacimiento);
        paciente.setGenero(genero);
        paciente.setTipoSangre(tipoSangre);
        paciente.setDireccion(direccion);
        paciente.setAlergias(alergias);
        return paciente;
    }

    private MedicamentoEntity medicamento(String nombre, String descripcion, String tipo, LocalDate vencimiento,
                                           int stock, int minimo, ProveedorEntity proveedor) {
        MedicamentoEntity medicamento = new MedicamentoEntity();
        medicamento.setNombreMedicamento(nombre);
        medicamento.setDescripcionMedicamento(descripcion);
        medicamento.setTipoMedicamento(tipo);
        medicamento.setFechaVencimiento(vencimiento);
        medicamento.setStockInventario(stock);
        medicamento.setCantidadMinima(minimo);
        medicamento.setProveedor(proveedor);
        return medicamento;
    }
}
