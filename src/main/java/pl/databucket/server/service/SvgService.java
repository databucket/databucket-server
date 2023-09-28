package pl.databucket.server.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.SvgDto;
import pl.databucket.server.entity.Svg;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.repository.SvgRepository;


@Service
@RequiredArgsConstructor
public class SvgService {

    private final SvgRepository svgRepository;
    private final ModelMapper modelMapper;

    public SvgDto createSvg(SvgDto svgDto) {
        Svg svg = new Svg();
        modelMapper.map(svgDto, svg);
        Svg saved = svgRepository.save(svg);
        modelMapper.map(saved, svgDto);
        return svgDto;
    }

    public SvgDto modifySvg(SvgDto svgDto) throws ModifyByNullEntityIdException {
        if (svgDto.getId() == null) {
            throw new ModifyByNullEntityIdException(Svg.class);
        }

        return svgRepository.findById(svgDto.getId())
            .map(svg -> {
                modelMapper.map(svgDto, svg);
                Svg saved = svgRepository.save(svg);
                modelMapper.map(saved, svgDto);
                return svgDto;
            })
            .orElse(svgDto);
    }

    public List<SvgDto> getSvgList() {
        return svgRepository.findAll().stream()
            .map(svg -> modelMapper.map(svg, SvgDto.class))
            .toList();
    }

    public void deleteSvg(long svgId) throws ItemNotFoundException {
        Optional<Svg> svgOptional = svgRepository.findById(svgId);

        if (!svgOptional.isPresent()) {
            throw new ItemNotFoundException(Svg.class, svgId);
        }

        svgRepository.delete(svgOptional.get());
    }

}
