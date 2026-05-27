package Controller;

import Entity.Emuns.RolUsuarioEnum;
import Entity.UsuarioEntity;
import Service.IMedicoService;
import Service.IUsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final IUsuarioService usuarioService;
    private final IMedicoService medicoService;

    public UsuarioController(IUsuarioService usuarioService, IMedicoService medicoService) {
        this.usuarioService = usuarioService;
        this.medicoService = medicoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "usuarios/listar";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("usuario", new UsuarioEntity());
        cargarCombos(model);
        return "usuarios/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("usuario") UsuarioEntity usuario,
                          @RequestParam(value = "idMedico", required = false) Long idMedico,
                          RedirectAttributes redirectAttributes) {
        if (idMedico != null) {
            usuario.setMedico(medicoService.buscarPorId(idMedico)
                    .orElseThrow(() -> new IllegalArgumentException("No existe el médico: " + idMedico)));
        } else {
            usuario.setMedico(null);
        }
        usuarioService.guardar(usuario);
        redirectAttributes.addFlashAttribute("mensaje", "Usuario guardado correctamente.");
        return "redirect:/usuarios";
    }

    @GetMapping("/editar/{idUsuario}")
    public String editar(@PathVariable Long idUsuario, Model model) {
        UsuarioEntity usuario = usuarioService.buscarPorId(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("No existe el usuario con ID: " + idUsuario));
        model.addAttribute("usuario", usuario);
        cargarCombos(model);
        return "usuarios/formulario";
    }

    @GetMapping("/activar/{idUsuario}")
    public String activar(@PathVariable Long idUsuario, RedirectAttributes redirectAttributes) {
        usuarioService.activarUsuario(idUsuario);
        redirectAttributes.addFlashAttribute("mensaje", "Usuario activado correctamente.");
        return "redirect:/usuarios";
    }

    @GetMapping("/desactivar/{idUsuario}")
    public String desactivar(@PathVariable Long idUsuario, RedirectAttributes redirectAttributes) {
        usuarioService.desactivarUsuario(idUsuario);
        redirectAttributes.addFlashAttribute("mensaje", "Usuario desactivado correctamente.");
        return "redirect:/usuarios";
    }

    @GetMapping("/eliminar/{idUsuario}")
    public String eliminar(@PathVariable Long idUsuario, RedirectAttributes redirectAttributes) {
        usuarioService.eliminar(idUsuario);
        redirectAttributes.addFlashAttribute("mensaje", "Usuario eliminado correctamente.");
        return "redirect:/usuarios";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("roles", RolUsuarioEnum.values());
        model.addAttribute("medicos", medicoService.listarTodos());
    }
}
