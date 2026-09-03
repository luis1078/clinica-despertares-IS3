package Config;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Evita que un recurso inexistente (p. ej. /pacientes/editar/{dni} con un DNI
 * que ya no existe) termine en la página blanca de error por defecto de Spring.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({EntityNotFoundException.class, IllegalArgumentException.class})
    public String manejarNoEncontrado(RuntimeException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error-negocio";
    }
}
