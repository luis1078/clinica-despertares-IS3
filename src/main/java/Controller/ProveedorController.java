package Controller;

import Entity.ProveedorEntity;
import Service.IProveedorService;
import Util.Pagina;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    private final IProveedorService proveedorService;

    public ProveedorController(IProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "nombre", required = false) String nombre,
                         @RequestParam(value = "pagina", defaultValue = "0") int numeroPagina,
                         Model model) {
        List<ProveedorEntity> todos = nombre == null || nombre.isBlank()
                ? proveedorService.listarTodos()
                : proveedorService.buscarPorNombre(nombre);
        Pagina<ProveedorEntity> pagina = Pagina.de(todos, numeroPagina);
        model.addAttribute("proveedores", pagina.contenido());
        model.addAttribute("pagina", pagina);
        model.addAttribute("nombre", nombre);
        return "proveedores/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("proveedor", new ProveedorEntity());
        return "proveedores/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("proveedor") ProveedorEntity proveedor, RedirectAttributes redirectAttributes) {
        proveedorService.guardar(proveedor);
        redirectAttributes.addFlashAttribute("mensaje", "Proveedor guardado correctamente.");
        return "redirect:/proveedores";
    }

    @GetMapping("/editar/{rucProveedor}")
    public String editar(@PathVariable String rucProveedor, Model model) {
        ProveedorEntity proveedor = proveedorService.buscarPorId(rucProveedor)
                .orElseThrow(() -> new IllegalArgumentException("No existe el proveedor: " + rucProveedor));
        model.addAttribute("proveedor", proveedor);
        return "proveedores/formulario";
    }

    @PostMapping("/eliminar/{rucProveedor}")
    public String eliminar(@PathVariable String rucProveedor, RedirectAttributes redirectAttributes) {
        proveedorService.eliminar(rucProveedor);
        redirectAttributes.addFlashAttribute("mensaje", "Proveedor eliminado correctamente.");
        return "redirect:/proveedores";
    }
}
