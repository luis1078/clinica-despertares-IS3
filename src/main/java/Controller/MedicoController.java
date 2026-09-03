package Controller;

import Entity.MedicoEntity;
import Service.IMedicoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/medicos")
public class MedicoController {

    private final IMedicoService medicoService;

    public MedicoController(IMedicoService medicoService) {
        this.medicoService = medicoService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "nombre", required = false) String nombre, Model model) {
        model.addAttribute("medicos", nombre == null || nombre.isBlank()
                ? medicoService.listarTodos()
                : medicoService.buscarPorNombre(nombre));
        model.addAttribute("nombre", nombre);
        return "medicos/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("medico", new MedicoEntity());
        return "medicos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("medico") MedicoEntity medico, RedirectAttributes redirectAttributes) {
        medicoService.guardar(medico);
        redirectAttributes.addFlashAttribute("mensaje", "Médico guardado correctamente.");
        return "redirect:/medicos";
    }

    @GetMapping("/editar/{idMedico}")
    public String editar(@PathVariable Long idMedico, Model model) {
        MedicoEntity medico = medicoService.buscarPorId(idMedico)
                .orElseThrow(() -> new IllegalArgumentException("No existe el médico con ID: " + idMedico));
        model.addAttribute("medico", medico);
        return "medicos/formulario";
    }

    @PostMapping("/eliminar/{idMedico}")
    public String eliminar(@PathVariable Long idMedico, RedirectAttributes redirectAttributes) {
        medicoService.eliminar(idMedico);
        redirectAttributes.addFlashAttribute("mensaje", "Médico eliminado correctamente.");
        return "redirect:/medicos";
    }
}
