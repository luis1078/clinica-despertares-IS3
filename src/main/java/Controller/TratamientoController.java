package Controller;

import Entity.Emuns.EstadoTratamientoEnum;
import Entity.TratamientoEntity;
import Service.IHistoriaMedicaService;
import Service.ITratamientoService;
import Util.Pagina;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tratamientos")
public class TratamientoController {

    private final ITratamientoService tratamientoService;
    private final IHistoriaMedicaService historiaMedicaService;

    public TratamientoController(ITratamientoService tratamientoService, IHistoriaMedicaService historiaMedicaService) {
        this.tratamientoService = tratamientoService;
        this.historiaMedicaService = historiaMedicaService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "pagina", defaultValue = "0") int numeroPagina, Model model) {
        Pagina<TratamientoEntity> pagina = Pagina.de(tratamientoService.listarTodos(), numeroPagina);
        model.addAttribute("tratamientos", pagina.contenido());
        model.addAttribute("pagina", pagina);
        return "tratamientos/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("tratamiento", new TratamientoEntity());
        cargarCombos(model);
        return "tratamientos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("tratamiento") TratamientoEntity tratamiento,
                          @RequestParam("codHistoriaMedica") Long codHistoriaMedica,
                          RedirectAttributes redirectAttributes) {
        if (tratamiento.getIdTratamiento() == null) {
            tratamientoService.registrarTratamiento(codHistoriaMedica, tratamiento);
        } else {
            TratamientoEntity tratamientoExistente = tratamientoService.buscarPorId(tratamiento.getIdTratamiento())
                    .orElseThrow(() -> new IllegalArgumentException("No existe el tratamiento: " + tratamiento.getIdTratamiento()));
            tratamientoExistente.setDescripcionTratamiento(tratamiento.getDescripcionTratamiento());
            tratamientoExistente.setFechaInicio(tratamiento.getFechaInicio());
            tratamientoExistente.setFechaFin(tratamiento.getFechaFin());
            tratamientoExistente.setTipoTratamiento(tratamiento.getTipoTratamiento());
            tratamientoExistente.setEstadoTratamiento(tratamiento.getEstadoTratamiento());
            tratamientoExistente.setHistoriaMedica(historiaMedicaService.buscarPorId(codHistoriaMedica)
                    .orElseThrow(() -> new IllegalArgumentException("No existe la historia médica: " + codHistoriaMedica)));
            tratamientoService.guardar(tratamientoExistente);
        }
        redirectAttributes.addFlashAttribute("mensaje", "Tratamiento guardado correctamente.");
        return "redirect:/tratamientos";
    }

    @GetMapping("/editar/{idTratamiento}")
    public String editar(@PathVariable Long idTratamiento, Model model) {
        TratamientoEntity tratamiento = tratamientoService.buscarPorId(idTratamiento)
                .orElseThrow(() -> new IllegalArgumentException("No existe el tratamiento: " + idTratamiento));
        model.addAttribute("tratamiento", tratamiento);
        cargarCombos(model);
        return "tratamientos/formulario";
    }

    @PostMapping("/finalizar/{idTratamiento}")
    public String finalizar(@PathVariable Long idTratamiento, RedirectAttributes redirectAttributes) {
        tratamientoService.finalizarTratamiento(idTratamiento);
        redirectAttributes.addFlashAttribute("mensaje", "Tratamiento finalizado correctamente.");
        return "redirect:/tratamientos";
    }

    @PostMapping("/suspender/{idTratamiento}")
    public String suspender(@PathVariable Long idTratamiento, RedirectAttributes redirectAttributes) {
        tratamientoService.suspenderTratamiento(idTratamiento);
        redirectAttributes.addFlashAttribute("mensaje", "Tratamiento suspendido correctamente.");
        return "redirect:/tratamientos";
    }

    @PostMapping("/eliminar/{idTratamiento}")
    public String eliminar(@PathVariable Long idTratamiento, RedirectAttributes redirectAttributes) {
        tratamientoService.eliminar(idTratamiento);
        redirectAttributes.addFlashAttribute("mensaje", "Tratamiento eliminado correctamente.");
        return "redirect:/tratamientos";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("historias", historiaMedicaService.listarTodos());
        model.addAttribute("estados", EstadoTratamientoEnum.values());
    }
}
