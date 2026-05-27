package Controller;

import Entity.HistoriaMedicaEntity;
import Service.IHistoriaMedicaService;
import Service.IPacienteService;
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
    public String listar(Model model) {
        model.addAttribute("historias", historiaMedicaService.listarTodos());
        return "historias/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("historia", new HistoriaMedicaEntity());
        model.addAttribute("pacientes", pacienteService.listarTodos());
        return "historias/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("historia") HistoriaMedicaEntity historia,
                          @RequestParam(value = "dniPaciente", required = false) String dniPaciente,
                          RedirectAttributes redirectAttributes) {
        if (historia.getCodHistoriaMedica() == null && dniPaciente != null && !dniPaciente.isBlank()) {
            historiaMedicaService.crearHistoriaMedica(dniPaciente, historia);
        } else if (historia.getCodHistoriaMedica() != null) {
            historiaMedicaService.actualizarHistoriaMedica(historia.getCodHistoriaMedica(), historia);
        } else {
            historiaMedicaService.guardar(historia);
        }
        redirectAttributes.addFlashAttribute("mensaje", "Historia médica guardada correctamente.");
        return "redirect:/historias";
    }

    @GetMapping("/editar/{codHistoriaMedica}")
    public String editar(@PathVariable Long codHistoriaMedica, Model model) {
        HistoriaMedicaEntity historia = historiaMedicaService.buscarPorId(codHistoriaMedica)
                .orElseThrow(() -> new IllegalArgumentException("No existe la historia médica: " + codHistoriaMedica));
        model.addAttribute("historia", historia);
        model.addAttribute("pacientes", pacienteService.listarTodos());
        return "historias/formulario";
    }

    @GetMapping("/eliminar/{codHistoriaMedica}")
    public String eliminar(@PathVariable Long codHistoriaMedica, RedirectAttributes redirectAttributes) {
        historiaMedicaService.eliminar(codHistoriaMedica);
        redirectAttributes.addFlashAttribute("mensaje", "Historia médica eliminada correctamente.");
        return "redirect:/historias";
    }
}
