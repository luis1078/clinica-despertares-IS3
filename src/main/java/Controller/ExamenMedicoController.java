package Controller;

import Entity.Emuns.EstadoExamenMedicoEnum;
import Entity.Emuns.TipoExamenEnum;
import Entity.ExamenMedicoEntity;
import Service.IDiagnosticoService;
import Service.IExamenMedicoService;
import Util.Pagina;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/examenes")
public class ExamenMedicoController {

    private final IExamenMedicoService examenMedicoService;
    private final IDiagnosticoService diagnosticoService;

    public ExamenMedicoController(IExamenMedicoService examenMedicoService, IDiagnosticoService diagnosticoService) {
        this.examenMedicoService = examenMedicoService;
        this.diagnosticoService = diagnosticoService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "pagina", defaultValue = "0") int numeroPagina, Model model) {
        Pagina<ExamenMedicoEntity> pagina = Pagina.de(examenMedicoService.listarTodos(), numeroPagina);
        model.addAttribute("examenes", pagina.contenido());
        model.addAttribute("pagina", pagina);
        return "examenes/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("examen", new ExamenMedicoEntity());
        cargarCombos(model);
        return "examenes/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("examen") ExamenMedicoEntity examen,
                          @RequestParam("idDiagnostico") Long idDiagnostico,
                          RedirectAttributes redirectAttributes) {
        if (examen.getCodExamenMedico() == null) {
            examenMedicoService.registrarSolicitudExamenMedico(idDiagnostico, examen);
        } else {
            examen.setDiagnostico(diagnosticoService.buscarPorId(idDiagnostico)
                    .orElseThrow(() -> new IllegalArgumentException("No existe el diagnóstico: " + idDiagnostico)));
            examenMedicoService.guardar(examen);
        }
        redirectAttributes.addFlashAttribute("mensaje", "Solicitud de examen guardada correctamente.");
        return "redirect:/examenes";
    }

    @GetMapping("/editar/{codExamenMedico}")
    public String editar(@PathVariable Long codExamenMedico, Model model) {
        ExamenMedicoEntity examen = examenMedicoService.buscarPorId(codExamenMedico)
                .orElseThrow(() -> new IllegalArgumentException("No existe el examen: " + codExamenMedico));
        model.addAttribute("examen", examen);
        cargarCombos(model);
        return "examenes/formulario";
    }

    @PostMapping("/proceso/{codExamenMedico}")
    public String proceso(@PathVariable Long codExamenMedico,
                          RedirectAttributes redirectAttributes) {
        try {
            examenMedicoService.marcarEnProceso(codExamenMedico);
            redirectAttributes.addFlashAttribute("mensaje", "Examen marcado como EN PROCESO correctamente.");

        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", "No se pudo actualizar el estado del examen. Verifique que la base de datos permita el estado EN_PROCESO.");

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/examenes";
    }

    @PostMapping("/finalizar/{codExamenMedico}")
    public String finalizar(@PathVariable Long codExamenMedico, RedirectAttributes redirectAttributes) {
        examenMedicoService.marcarFinalizado(codExamenMedico);
        redirectAttributes.addFlashAttribute("mensaje", "Examen marcado como finalizado.");
        return "redirect:/examenes";
    }

    @PostMapping("/eliminar/{codExamenMedico}")
    public String eliminar(@PathVariable Long codExamenMedico, RedirectAttributes redirectAttributes) {
        examenMedicoService.eliminar(codExamenMedico);
        redirectAttributes.addFlashAttribute("mensaje", "Examen eliminado correctamente.");
        return "redirect:/examenes";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("diagnosticos", diagnosticoService.listarTodos());
        model.addAttribute("tipos", TipoExamenEnum.values());
        model.addAttribute("estados", EstadoExamenMedicoEnum.values());
    }
}
