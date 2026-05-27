package Service;

import Entity.UsuarioEntity;

import java.util.List;
import java.util.Optional;

public interface IUsuarioService {

    List<UsuarioEntity> listarTodos();

    Optional<UsuarioEntity> buscarPorId(Long idUsuario);

    UsuarioEntity guardar(UsuarioEntity usuario);

    void eliminar(Long idUsuario);

    Optional<UsuarioEntity> buscarPorUsername(String username);

    Optional<UsuarioEntity> buscarPorCorreo(String correo);

    boolean existeUsername(String username);

    boolean existeCorreo(String correo);

    Optional<UsuarioEntity> buscarPorMedico(Long idMedico);

    UsuarioEntity registrar(UsuarioEntity usuario);

    Optional<UsuarioEntity> login(String usuarioOCorreo, String password);

    void activarUsuario(Long idUsuario);

    void desactivarUsuario(Long idUsuario);
}
