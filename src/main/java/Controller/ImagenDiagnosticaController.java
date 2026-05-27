package Controller;

import Entity.Emuns.EstadoExamenMedicoEnum;
import Entity.Emuns.TipoExamenEnum;
import Entity.ImagenDiagnosticaEntity;
import Service.IDiagnosticoService;
import Service.IImagenDiagnosticaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/imagenes")
public class ImagenDiagnosticaController {

    private final IImagenDiagnosticaService imagenDiagnosticaService;
    private final IDiagnosticoService diagnosticoService;

    public ImagenDiagnosticaController(IImagenDiagnosticaService imagenDiagnosticaService, IDiagnosticoService diagnosticoService) {
        this.imagenDiagnosticaService = imagenDiagnosticaService;
        this.diagnosticoService = diagnosticoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("imagenes", imagenDiagnosticaService.listarTodos());
        return "imagenes/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("imagen", new ImagenDiagnosticaEntity());
        cargarCombos(model);
        return "imagenes/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("imagen") ImagenDiagnosticaEntity imagen,
                          @RequestParam("idDiagnostico") Long idDiagnostico,
                          RedirectAttributes redirectAttributes) {
        imagen.setTipoExamen(TipoExamenEnum.IMAGEN);
        if (imagen.getCodExamenMedico() == null) {
            imagenDiagnosticaService.registrarImagenDiagnostica(idDiagnostico, imagen);
        } else {
            imagenDiagnosticaService.registrarResultadoImagenDiagnostica(imagen.getCodExamenMedico(), imagen);
        }
        redirectAttributes.addFlashAttribute("mensaje", "Imagen diagnóstica guardada correctamente.");
        return "redirect:/imagenes";
    }

    @GetMapping("/editar/{codExamenMedico}")
    public String editar(@PathVariable Long codExamenMedico, Model model) {
        ImagenDiagnosticaEntity imagen = imagenDiagnosticaService.buscarPorId(codExamenMedico)
                .orElseThrow(() -> new IllegalArgumentException("No existe la imagen diagnóstica: " + codExamenMedico));
        model.addAttribute("imagen", imagen);
        cargarCombos(model);
        return "imagenes/formulario";
    }

    @GetMapping("/eliminar/{codExamenMedico}")
    public String eliminar(@PathVariable Long codExamenMedico, RedirectAttributes redirectAttributes) {
        imagenDiagnosticaService.eliminar(codExamenMedico);
        redirectAttributes.addFlashAttribute("mensaje", "Imagen diagnóstica eliminada correctamente.");
        return "redirect:/imagenes";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("diagnosticos", diagnosticoService.listarTodos());
        model.addAttribute("estados", EstadoExamenMedicoEnum.values());
    }
}
