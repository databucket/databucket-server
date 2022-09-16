package pl.databucket.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.server.entity.Svg;

@Repository
public interface SvgRepository extends JpaRepository<Svg, Long> {

}
