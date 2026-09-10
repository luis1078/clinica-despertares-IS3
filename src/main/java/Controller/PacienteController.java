package Controller;

import Entity.Emuns.GeneroPacienteEnum;
import Entity.PacienteEntity;
import Service.IHistorialClinicoService;
import Service.IPacienteService;
import Util.Pagina;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pacientes")
public class PacienteController {

    private final IPacienteService pacienteService;
    private final IHistorialClinicoService historialClinicoService;

    public PacienteController(IPacienteService pacienteService, IHistorialClinicoService historialClinicoService) {
        this.pacienteService = pacienteService;
        this.historialClinicoService = historialClinicoService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "buscar", required = false) String buscar,
                         @RequestParam(value = "pagina", defaultValue = "0") int numeroPagina,
                         Model model) {
        Pagina<PacienteEntity> pagina = Pagina.de(pacienteService.buscarPacientes(buscar), numeroPagina);
        model.addAttribute("pacientes", pagina.contenido());
        model.addAttribute("pagina", pagina);
        model.addAttribute("buscar", buscar);
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

    @PostMapping("/eliminar/{dniPaciente}")
    public String eliminar(@PathVariable String dniPaciente, RedirectAttributes redirectAttributes) {
        pacienteService.eliminar(dniPaciente);
        redirectAttributes.addFlashAttribute("mensaje", "Paciente eliminado correctamente.");
        return "redirect:/pacientes";
    }

    @GetMapping("/{dniPaciente}/historial")
    public String historial(@PathVariable String dniPaciente, Model model) {
        model.addAttribute("historial", historialClinicoService.construirHistorial(dniPaciente));
        return "pacientes/historial";
    }
}
