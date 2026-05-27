package Controller;

import Entity.Emuns.RolUsuarioEnum;
import Entity.MedicoEntity;
import Entity.UsuarioEntity;
import Service.IMedicoService;
import Service.IUsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

@Controller
public class AuthController {

    private final IUsuarioService usuarioService;
    private final IMedicoService medicoService;

    public AuthController(IUsuarioService usuarioService, IMedicoService medicoService) {
        this.usuarioService = usuarioService;
        this.medicoService = medicoService;
    }

    @GetMapping("/login")
    public String mostrarLogin(HttpSession session) {
        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/";
        }

        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {

        Optional<UsuarioEntity> usuarioOptional = usuarioService.login(username.trim(), password);

        if (usuarioOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuario, correo o contraseña incorrectos.");
            return "redirect:/login";
        }

        UsuarioEntity usuario = usuarioOptional.get();

        session.setAttribute("usuarioLogueado", usuario);
        session.setAttribute("idUsuario", usuario.getIdUsuario());
        session.setAttribute("nombreUsuario", usuario.getUsername());
        session.setAttribute("rolUsuario", usuario.getRol().name());

        return "redirect:/";
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("usuario", new UsuarioEntity());
        model.addAttribute("roles", RolUsuarioEnum.values());
        model.addAttribute("medicos", medicoService.listarTodos());
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String registrar(@ModelAttribute("usuario") UsuarioEntity usuario,
                            @RequestParam("confirmarPassword") String confirmarPassword,
                            @RequestParam(value = "idMedico", required = false) Long idMedico,
                            Model model,
                            RedirectAttributes redirectAttributes) {

        if (!usuario.getPassword().equals(confirmarPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            cargarDatosRegistro(model, usuario);
            return "auth/registro";
        }

        try {
            validarYAsignarMedico(usuario, idMedico);

            usuarioService.registrar(usuario);

            redirectAttributes.addFlashAttribute("mensaje", "Usuario registrado correctamente. Ahora puede iniciar sesión.");
            return "redirect:/login";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            cargarDatosRegistro(model, usuario);
            return "auth/registro";

        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "No se pudo registrar el usuario. Verifique que el rol sea válido y que el correo o username no estén duplicados.");
            cargarDatosRegistro(model, usuario);
            return "auth/registro";
        }
    }

    private void cargarDatosRegistro(Model model, UsuarioEntity usuario) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("roles", RolUsuarioEnum.values());
        model.addAttribute("medicos", medicoService.listarTodos());
    }

    private void validarYAsignarMedico(UsuarioEntity usuario, Long idMedico) {
        if (usuario.getRol() == null) {
            throw new IllegalArgumentException("Debe seleccionar un rol.");
        }

        if (usuario.getRol() == RolUsuarioEnum.MEDICO) {
            if (idMedico == null) {
                throw new IllegalArgumentException("Debe seleccionar un médico asociado para el rol MEDICO.");
            }

            MedicoEntity medico = medicoService.buscarPorId(idMedico)
                    .orElseThrow(() -> new IllegalArgumentException("El médico seleccionado no existe."));

            usuario.setMedico(medico);

        } else {
            usuario.setMedico(null);
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        return "acceso-denegado";
    }
}
