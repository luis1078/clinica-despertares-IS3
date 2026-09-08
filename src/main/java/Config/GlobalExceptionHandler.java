package Config;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Evita que un recurso inexistente (p. ej. /pacientes/editar/{dni} con un DNI
 * que ya no existe) termine en la página blanca de error por defecto de Spring.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({EntityNotFoundException.class, IllegalArgumentException.class})
    public String manejarNoEncontrado(RuntimeException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error-negocio";
    }

    /**
     * Red de seguridad: cualquier excepción no prevista (falla de BD, NPE, etc.)
     * cae aquí en vez de terminar en la página blanca de error de Spring, sin
     * exponer detalles internos al usuario.
     */
    @ExceptionHandler(Exception.class)
    public String manejarErrorInesperado(Exception ex, Model model) {
        log.error("Error inesperado no controlado", ex);
        model.addAttribute("error", "Ocurrió un error inesperado. Intenta nuevamente o contacta al administrador.");
        return "error-negocio";
    }
}
