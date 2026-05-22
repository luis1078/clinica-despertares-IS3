package Entity;

import Entity.Emuns.RolUsuarioEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "usuario", schema = "public")

public class UsuarioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "idusuario")
    private Long idUsuario;

    @Column(name = "correo",length = 50, unique = true, nullable = false)
    private String correo;

    @Column(name = "username", length = 50, unique = true, nullable = false)
    private String username;

    @Column(name = "password", length = 100, nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol",length = 15, nullable = false)
    private RolUsuarioEnum rol;

    @Column(name = "estado", nullable = false)
    private boolean estado = true;

    @OneToOne
    @JoinColumn(name = "idmedico", unique = true)
    private MedicoEntity medico;
}
