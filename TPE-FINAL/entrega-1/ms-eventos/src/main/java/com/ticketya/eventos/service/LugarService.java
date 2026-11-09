package com.ticketya.eventos.service;

import com.ticketya.eventos.dto.AsientoResponse;
import com.ticketya.eventos.dto.LugarRequest;
import com.ticketya.eventos.dto.LugarResponse;
import com.ticketya.eventos.dto.SectorRequest;
import com.ticketya.eventos.exception.RecursoNoEncontradoException;
import com.ticketya.eventos.model.Lugar;
import com.ticketya.eventos.model.Sector;
import com.ticketya.eventos.model.TipoSector;
import com.ticketya.eventos.repository.LugarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LugarService {

    private final LugarRepository lugarRepository;

    public LugarService(LugarRepository lugarRepository) {
        this.lugarRepository = lugarRepository;
    }

    @Transactional
    public LugarResponse crear(LugarRequest request) {
        Lugar lugar = new Lugar(request.nombre(), request.direccion(), request.ciudad(),
                request.latitud(), request.longitud());
        for (SectorRequest s : request.sectores()) {
            lugar.agregarSector(crearSector(s));
        }
        return LugarResponse.de(lugarRepository.save(lugar));
    }

    private Sector crearSector(SectorRequest s) {
        if (s.tipo() == TipoSector.NUMERADO) {
            if (s.filas() == null || s.asientosPorFila() == null) {
                throw new IllegalArgumentException("El sector numerado '" + s.nombre()
                        + "' debe indicar filas y asientosPorFila");
            }
            return Sector.numerado(s.nombre(), s.filas(), s.asientosPorFila());
        }
        if (s.capacidad() == null) {
            throw new IllegalArgumentException("El sector no numerado '" + s.nombre() + "' debe indicar capacidad");
        }
        return Sector.noNumerado(s.nombre(), s.capacidad());
    }

    @Transactional(readOnly = true)
    public List<LugarResponse> listar(String ciudad) {
        List<Lugar> lugares = ciudad == null || ciudad.isBlank()
                ? lugarRepository.findAll()
                : lugarRepository.findByCiudadIgnoreCase(ciudad);
        return lugares.stream().map(LugarResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public LugarResponse obtener(Long id) {
        return LugarResponse.de(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<AsientoResponse> asientosDeSector(Long lugarId, Long sectorId) {
        Sector sector = buscar(lugarId).getSectores().stream()
                .filter(s -> s.getId().equals(sectorId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El lugar " + lugarId + " no tiene un sector con id " + sectorId));
        return sector.getAsientos().stream().map(AsientoResponse::de).toList();
    }

    Lugar buscar(Long id) {
        return lugarRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el lugar con id " + id));
    }
}
