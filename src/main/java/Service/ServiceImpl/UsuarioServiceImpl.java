package Service.ServiceImpl;

import Entity.UsuarioEntity;
import Repository.IUsuarioRepository;
import Service.IUsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImpl implements IUsuarioService {

    private final IUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(IUsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UsuarioEntity> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    public Optional<UsuarioEntity> buscarPorId(Long idUsuario) {
        return usuarioRepository.findById(idUsuario);
    }

    @Override
    public UsuarioEntity guardar(UsuarioEntity usuario) {
        if (usuario.getIdUsuario() != null) {
            UsuarioEntity existente = usuarioRepository.findById(usuario.getIdUsuario())
                    .orElseThrow(() -> new IllegalArgumentException("No existe el usuario con ID: " + usuario.getIdUsuario()));

            if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
                usuario.setPassword(existente.getPassword());
            } else {
                usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            }
        } else {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }

        return usuarioRepository.save(usuario);
    }

    @Override
    public void eliminar(Long idUsuario) {
        usuarioRepository.deleteById(idUsuario);
    }

    @Override
    public Optional<UsuarioEntity> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    @Override
    public Optional<UsuarioEntity> buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo);
    }

    @Override
    public boolean existeUsername(String username) {
        return usuarioRepository.existsByUsername(username);
    }

    @Override
    public boolean existeCorreo(String correo) {
        return usuarioRepository.existsByCorreo(correo);
    }

    @Override
    public Optional<UsuarioEntity> buscarPorMedico(Long idMedico) {
        return usuarioRepository.findByMedico_IdMedico(idMedico);
    }

    @Override
    public UsuarioEntity registrar(UsuarioEntity usuario) {
        if (usuarioRepository.existsByUsername(usuario.getUsername())) {
            throw new IllegalArgumentException("El username ya se encuentra registrado.");
        }

        if (usuarioRepository.existsByCorreo(usuario.getCorreo())) {
            throw new IllegalArgumentException("El correo ya se encuentra registrado.");
        }

        usuario.setEstado(true);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return usuarioRepository.save(usuario);
    }

    @Override
    public Optional<UsuarioEntity> login(String usuarioOCorreo, String password) {
        Optional<UsuarioEntity> usuarioEncontrado = usuarioRepository.findByUsername(usuarioOCorreo);

        if (usuarioEncontrado.isEmpty()) {
            usuarioEncontrado = usuarioRepository.findByCorreo(usuarioOCorreo);
        }

        return usuarioEncontrado
                .filter(UsuarioEntity::isEstado)
                .filter(usuario -> passwordEncoder.matches(password, usuario.getPassword()));
    }

    @Override
    public void activarUsuario(Long idUsuario) {
        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("No existe el usuario con ID: " + idUsuario));

        usuario.setEstado(true);
        usuarioRepository.save(usuario);
    }

    @Override
    public void desactivarUsuario(Long idUsuario) {
        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("No existe el usuario con ID: " + idUsuario));

        usuario.setEstado(false);
        usuarioRepository.save(usuario);
    }
}
