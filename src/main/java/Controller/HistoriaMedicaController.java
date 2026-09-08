package Controller;

import Entity.HistoriaMedicaEntity;
import Service.IHistoriaMedicaService;
import Service.IPacienteService;
import Util.Pagina;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/historias")
public class HistoriaMedicaController {

    private final IHistoriaMedicaService historiaMedicaService;
    private final IPacienteService pacienteService;

    public HistoriaMedicaController(IHistoriaMedicaService historiaMedicaService, IPacienteService pacienteService) {
        this.historiaMedicaService = historiaMedicaService;
        this.pacienteService = pacienteService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "q", required = false) String q,
                         @RequestParam(value = "pagina", defaultValue = "0") int numeroPagina,
                         Model model) {
        Pagina<HistoriaMedicaEntity> pagina = Pagina.de(historiaMedicaService.buscarHistorias(q), numeroPagina);
        model.addAttribute("historias", pagina.contenido());
        model.addAttribute("pagina", pagina);
        model.addAttribute("busqueda", q);
        return "historias/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(value = "dniPaciente", required = false) String dniPaciente,
                        Model model,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        if (!tieneRol(session, "MEDICO", "ENFERMERA")) {
            redirectAttributes.addFlashAttribute("error", "Solo el médico o la enfermera pueden crear historias médicas.");
            return "redirect:/acceso-denegado";
        }
        model.addAttribute("historia", new HistoriaMedicaEntity());
        model.addAttribute("pacientes", pacienteService.listarTodos());
        model.addAttribute("pacienteSeleccionado", dniPaciente);
        return "historias/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("historia") HistoriaMedicaEntity historia,
                          @RequestParam(value = "dniPaciente", required = false) String dniPaciente,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        if (historia.getCodHistoriaMedica() == null) {
            if (!tieneRol(session, "MEDICO", "ENFERMERA")) {
                redirectAttributes.addFlashAttribute("error", "Solo el médico o la enfermera pueden crear historias médicas.");
                return "redirect:/acceso-denegado";
            }
            historiaMedicaService.crearHistoriaMedica(dniPaciente, historia);
            redirectAttributes.addFlashAttribute("mensaje", "Historia médica creada correctamente.");
            return "redirect:/historias";
        }
        if (!tieneRol(session, "MEDICO")) {
            redirectAttributes.addFlashAttribute("error", "Solo el médico puede modificar historias médicas.");
            return "redirect:/acceso-denegado";
        }
        historiaMedicaService.actualizarHistoriaMedica(historia.getCodHistoriaMedica(), historia);
        redirectAttributes.addFlashAttribute("mensaje", "Historia médica actualizada correctamente.");
        return "redirect:/historias";
    }

    @GetMapping("/editar/{codHistoriaMedica}")
    public String editar(@PathVariable Long codHistoriaMedica, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!tieneRol(session, "MEDICO")) {
            redirectAttributes.addFlashAttribute("error", "Solo el médico puede modificar historias médicas.");
            return "redirect:/acceso-denegado";
        }
        HistoriaMedicaEntity historia = historiaMedicaService.buscarPorId(codHistoriaMedica)
                .orElseThrow(() -> new IllegalArgumentException("No existe la historia médica: " + codHistoriaMedica));
        model.addAttribute("historia", historia);
        model.addAttribute("pacientes", pacienteService.listarTodos());
        model.addAttribute("pacienteSeleccionado", historia.getPaciente() != null ? historia.getPaciente().getDnipaciente() : null);
        return "historias/formulario";
    }

    @PostMapping("/eliminar/{codHistoriaMedica}")
    public String eliminar(@PathVariable Long codHistoriaMedica, HttpSession session, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "La eliminación de historias médicas no está habilitada.");
        return "redirect:/historias";
    }

    private boolean tieneRol(HttpSession session, String... rolesPermitidos) {
        Object rol = session.getAttribute("rolUsuario");
        if (rol == null) { return false; }
        for (String rolPermitido : rolesPermitidos) {
            if (rolPermitido.equals(rol.toString())) { return true; }
        }
        return false;
    }
}
