package Service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Un punto en la línea de tiempo del historial clínico de un paciente.
 * Se arma en {@link Service.ServiceImpl.HistorialClinicoServiceImpl} combinando
 * citas, diagnósticos, exámenes, tratamientos y pagos en un único formato
 * que la plantilla pinta sin necesidad de conocer la entidad de origen.
 */
@Getter
@Setter
@AllArgsConstructor
public class EventoHistorialDTO {

    /** CITA, DIAGNOSTICO, EXAMEN, TRATAMIENTO o PAGO. */
    private String tipo;
    private LocalDate fecha;
    private LocalTime hora;
    /** Nombre del fragmento SVG de fragments/icons.html a usar en el marcador de la línea de tiempo. */
    private String icono;
    private String titulo;
    private String subtitulo;
    /** Texto corto para el badge de estado (p. ej. "Finalizada", "Grave"). */
    private String estadoTexto;
    /** azul, verde, rojo, ambar o gris: clase de color del badge. */
    private String estadoColor;
    private List<String> detalles;
}
