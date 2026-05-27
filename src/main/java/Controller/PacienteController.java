package Controller;

import Entity.Emuns.GeneroPacienteEnum;
import Entity.PacienteEntity;
import Service.IPacienteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pacientes")
public class PacienteController {

    private final IPacienteService pacienteService;

    public PacienteController(IPacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "nombre", required = false) String nombre, Model model) {
        model.addAttribute("pacientes", nombre == null || nombre.isBlank()
                ? pacienteService.listarTodos()
                : pacienteService.buscarPorNombre(nombre));
        model.addAttribute("nombre", nombre);
        return "pacientes/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("paciente", new PacienteEntity());
        model.addAttribute("generos", GeneroPacienteEnum.values());
        return "pacientes/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("paciente") PacienteEntity paciente, RedirectAttributes redirectAttributes) {
        pacienteService.guardar(paciente);
        redirectAttributes.addFlashAttribute("mensaje", "Paciente guardado correctamente.");
        return "redirect:/pacientes";
    }

    @GetMapping("/editar/{dniPaciente}")
    public String editar(@PathVariable String dniPaciente, Model model) {
        PacienteEntity paciente = pacienteService.buscarPorId(dniPaciente)
                .orElseThrow(() -> new IllegalArgumentException("No existe el paciente con DNI: " + dniPaciente));
        model.addAttribute("paciente", paciente);
        model.addAttribute("generos", GeneroPacienteEnum.values());
        return "pacientes/formulario";
    }

    @GetMapping("/eliminar/{dniPaciente}")
    public String eliminar(@PathVariable String dniPaciente, RedirectAttributes redirectAttributes) {
        pacienteService.eliminar(dniPaciente);
        redirectAttributes.addFlashAttribute("mensaje", "Paciente eliminado correctamente.");
        return "redirect:/pacientes";
    }
}
