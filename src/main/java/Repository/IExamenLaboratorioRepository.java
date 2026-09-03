package Repository;

import Entity.ExamenLaboratorioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IExamenLaboratorioRepository extends JpaRepository<ExamenLaboratorioEntity, Long> {

    List<ExamenLaboratorioEntity> findByNombreMuestra(String nombreMuestra);

    List<ExamenLaboratorioEntity> findByUnidadMedida(String unidadMedida);

    @Query("""
            select e
            from ExamenLaboratorioEntity e
            left join e.diagnostico d
            left join d.historiaMedica h
            left join h.paciente p
            where lower(coalesce(e.nombreMuestra, '')) like lower(concat(concat('%', :texto), '%'))
               or lower(coalesce(e.descripcionResultado, '')) like lower(concat(concat('%', :texto), '%'))
               or lower(coalesce(e.unidadMedida, '')) like lower(concat(concat('%', :texto), '%'))
               or lower(coalesce(e.observaciones, '')) like lower(concat(concat('%', :texto), '%'))
               or lower(coalesce(p.nombrePaciente, '')) like lower(concat(concat('%', :texto), '%'))
               or lower(coalesce(p.apellidoPaciente, '')) like lower(concat(concat('%', :texto), '%'))
               or lower(concat(concat(coalesce(p.nombrePaciente, ''), ' '), coalesce(p.apellidoPaciente, ''))) like lower(concat(concat('%', :texto), '%'))
               or lower(coalesce(p.dnipaciente, '')) like lower(concat(concat('%', :texto), '%'))
            """)
    List<ExamenLaboratorioEntity> buscarPorTexto(@Param("texto") String texto);
}
