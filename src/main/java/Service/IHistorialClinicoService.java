package Service;

import Service.dto.HistorialClinicoDTO;

public interface IHistorialClinicoService {

    /**
     * Arma la línea de tiempo clínica completa de un paciente (citas,
     * diagnósticos, exámenes, tratamientos y pagos) ordenada de la más
     * reciente a la más antigua.
     *
     * @throws IllegalArgumentException si no existe un paciente con ese DNI.
     */
    HistorialClinicoDTO construirHistorial(String dniPaciente);
}
