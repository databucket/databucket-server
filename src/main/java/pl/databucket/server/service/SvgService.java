package pl.databucket.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.SvgDto;
import pl.databucket.server.entity.Svg;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.repository.SvgRepository;

import java.util.List;
import java.util.Optional;


@Service
public class SvgService {

    @Autowired
    private SvgRepository svgRepository;

    public Svg createSvg(SvgDto svgDto) throws ItemAlreadyExistsException {
        Svg svg = new Svg();
        svg.setName(svgDto.getName());
        svg.setStructure(svgDto.getStructure());
        return svgRepository.save(svg);
    }

    public Svg modifySvg(SvgDto svgDto) throws ModifyByNullEntityIdException {
        if (svgDto.getId() == null)
            throw new ModifyByNullEntityIdException(Svg.class);

        Svg svg = svgRepository.getById(svgDto.getId());
        svg.setName(svgDto.getName());
        svg.setStructure(svgDto.getStructure());

        return svgRepository.save(svg);
    }

    public List<Svg> getSvgList() {
        return svgRepository.findAll();
    }

    public void deleteSvg(long svgId) throws ItemNotFoundException {
        Optional<Svg> svgOptional = svgRepository.findById(svgId);

        if (!svgOptional.isPresent())
            throw new ItemNotFoundException(Svg.class, svgId);

        svgRepository.delete(svgOptional.get());
    }
}
