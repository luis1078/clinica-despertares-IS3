package Controller;

import Entity.DiagnosticoEntity;
import Entity.Emuns.GravedadDiagnosticoEnum;
import Service.IDiagnosticoService;
import Service.IHistoriaMedicaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/diagnosticos")
public class DiagnosticoController {

    private final IDiagnosticoService diagnosticoService;
    private final IHistoriaMedicaService historiaMedicaService;

    public DiagnosticoController(IDiagnosticoService diagnosticoService, IHistoriaMedicaService historiaMedicaService) {
        this.diagnosticoService = diagnosticoService;
        this.historiaMedicaService = historiaMedicaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("diagnosticos", diagnosticoService.listarTodos());
        return "diagnosticos/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("diagnostico", new DiagnosticoEntity());
        cargarCombos(model);
        return "diagnosticos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("diagnostico") DiagnosticoEntity diagnostico,
                          @RequestParam("codHistoriaMedica") Long codHistoriaMedica,
                          RedirectAttributes redirectAttributes) {
        if (diagnostico.getIdDiagnostico() == null) {
            diagnosticoService.registrarDiagnostico(codHistoriaMedica, diagnostico);
        } else {
            DiagnosticoEntity diagnosticoExistente = diagnosticoService.buscarPorId(diagnostico.getIdDiagnostico())
                    .orElseThrow(() -> new IllegalArgumentException("No existe el diagnóstico: " + diagnostico.getIdDiagnostico()));
            diagnosticoExistente.setDescripcionDiagnostico(diagnostico.getDescripcionDiagnostico());
            diagnosticoExistente.setFechaDiagnostico(diagnostico.getFechaDiagnostico());
            diagnosticoExistente.setGravedadDiagnostico(diagnostico.getGravedadDiagnostico());
            diagnosticoExistente.setHistoriaMedica(historiaMedicaService.buscarPorId(codHistoriaMedica)
                    .orElseThrow(() -> new IllegalArgumentException("No existe la historia médica: " + codHistoriaMedica)));
            diagnosticoService.guardar(diagnosticoExistente);
        }
        redirectAttributes.addFlashAttribute("mensaje", "Diagnóstico guardado correctamente.");
        return "redirect:/diagnosticos";
    }

    @GetMapping("/editar/{idDiagnostico}")
    public String editar(@PathVariable Long idDiagnostico, Model model) {
        DiagnosticoEntity diagnostico = diagnosticoService.buscarPorId(idDiagnostico)
                .orElseThrow(() -> new IllegalArgumentException("No existe el diagnóstico: " + idDiagnostico));
        model.addAttribute("diagnostico", diagnostico);
        cargarCombos(model);
        return "diagnosticos/formulario";
    }

    @GetMapping("/eliminar/{idDiagnostico}")
    public String eliminar(@PathVariable Long idDiagnostico, RedirectAttributes redirectAttributes) {
        diagnosticoService.eliminar(idDiagnostico);
        redirectAttributes.addFlashAttribute("mensaje", "Diagnóstico eliminado correctamente.");
        return "redirect:/diagnosticos";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("historias", historiaMedicaService.listarTodos());
        model.addAttribute("gravedades", GravedadDiagnosticoEnum.values());
    }
}
