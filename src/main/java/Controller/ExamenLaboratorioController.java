package Controller;

import Entity.Emuns.EstadoExamenMedicoEnum;
import Entity.Emuns.TipoExamenEnum;
import Entity.ExamenLaboratorioEntity;
import Service.IDiagnosticoService;
import Service.IExamenLaboratorioService;
import Util.Pagina;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/laboratorio")
public class ExamenLaboratorioController {

    private final IExamenLaboratorioService examenLaboratorioService;
    private final IDiagnosticoService diagnosticoService;

    public ExamenLaboratorioController(IExamenLaboratorioService examenLaboratorioService, IDiagnosticoService diagnosticoService) {
        this.examenLaboratorioService = examenLaboratorioService;
        this.diagnosticoService = diagnosticoService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "buscar", required = false) String buscar,
                         @RequestParam(value = "pagina", defaultValue = "0") int numeroPagina,
                         Model model) {
        Pagina<ExamenLaboratorioEntity> pagina = Pagina.de(examenLaboratorioService.buscarExamenes(buscar), numeroPagina);
        model.addAttribute("examenes", pagina.contenido());
        model.addAttribute("pagina", pagina);
        model.addAttribute("buscar", buscar);
        return "laboratorio/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("examen", new ExamenLaboratorioEntity());
        cargarCombos(model);
        return "laboratorio/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("examen") ExamenLaboratorioEntity examen,
                          @RequestParam("idDiagnostico") Long idDiagnostico,
                          RedirectAttributes redirectAttributes) {
        examen.setTipoExamen(TipoExamenEnum.LABORATORIO);
        if (examen.getCodExamenMedico() == null) {
            examenLaboratorioService.registrarExamenLaboratorio(idDiagnostico, examen);
        } else {
            examenLaboratorioService.registrarResultadoLaboratorio(examen.getCodExamenMedico(), examen);
        }
        redirectAttributes.addFlashAttribute("mensaje", "Examen de laboratorio guardado correctamente.");
        return "redirect:/laboratorio";
    }

    @GetMapping("/editar/{codExamenMedico}")
    public String editar(@PathVariable Long codExamenMedico, Model model) {
        ExamenLaboratorioEntity examen = examenLaboratorioService.buscarPorId(codExamenMedico)
                .orElseThrow(() -> new IllegalArgumentException("No existe el examen de laboratorio: " + codExamenMedico));
        model.addAttribute("examen", examen);
        cargarCombos(model);
        return "laboratorio/formulario";
    }

    @PostMapping("/eliminar/{codExamenMedico}")
    public String eliminar(@PathVariable Long codExamenMedico, RedirectAttributes redirectAttributes) {
        examenLaboratorioService.eliminar(codExamenMedico);
        redirectAttributes.addFlashAttribute("mensaje", "Examen de laboratorio eliminado correctamente.");
        return "redirect:/laboratorio";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("diagnosticos", diagnosticoService.listarTodos());
        model.addAttribute("estados", EstadoExamenMedicoEnum.values());
    }
}
