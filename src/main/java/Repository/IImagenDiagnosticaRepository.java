package Repository;

import Entity.ImagenDiagnosticaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface IImagenDiagnosticaRepository extends JpaRepository<ImagenDiagnosticaEntity, Long> {

    List<ImagenDiagnosticaEntity> findByRegionCuerpo(String regionCuerpo);

    List<ImagenDiagnosticaEntity> findByUsaContraste(boolean usaContraste);

    List<ImagenDiagnosticaEntity> findByNombreImagenContainingIgnoreCase(String nombreImagen);

    @Query("""
            select i
            from ImagenDiagnosticaEntity i
            left join i.diagnostico d
            left join d.historiaMedica h
            left join h.paciente p
            where lower(coalesce(i.nombreImagen, '')) like lower(concat(concat('%', :texto), '%'))
               or lower(coalesce(i.regionCuerpo, '')) like lower(concat(concat('%', :texto), '%'))
               or lower(coalesce(i.informeMedico, '')) like lower(concat(concat('%', :texto), '%'))
               or lower(coalesce(i.tipoContraste, '')) like lower(concat(concat('%', :texto), '%'))
               or lower(coalesce(i.observaciones, '')) like lower(concat(concat('%', :texto), '%'))
               or lower(coalesce(p.nombrePaciente, '')) like lower(concat(concat('%', :texto), '%'))
               or lower(coalesce(p.apellidoPaciente, '')) like lower(concat(concat('%', :texto), '%'))
               or lower(concat(concat(coalesce(p.nombrePaciente, ''), ' '), coalesce(p.apellidoPaciente, ''))) like lower(concat(concat('%', :texto), '%'))
               or lower(coalesce(p.dnipaciente, '')) like lower(concat(concat('%', :texto), '%'))
            """)
    List<ImagenDiagnosticaEntity> buscarPorTexto(@Param("texto") String texto);
}
