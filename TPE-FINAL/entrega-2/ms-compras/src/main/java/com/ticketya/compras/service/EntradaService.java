package com.ticketya.compras.service;

import com.ticketya.compras.dto.EntradaResponse;
import com.ticketya.compras.exception.AccesoDenegadoException;
import com.ticketya.compras.exception.RecursoNoEncontradoException;
import com.ticketya.compras.model.Entrada;
import com.ticketya.compras.repository.EntradaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EntradaService {

    private final EntradaRepository entradaRepository;

    public EntradaService(EntradaRepository entradaRepository) {
        this.entradaRepository = entradaRepository;
    }

    @Transactional(readOnly = true)
    public List<EntradaResponse> deUsuario(Long usuarioId) {
        return entradaRepository.findByUsuarioIdOrderByEmitidaEnDesc(usuarioId).stream()
                .map(EntradaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public EntradaResponse obtener(Long id, Long usuarioId) {
        Entrada entrada = entradaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la entrada " + id));
        if (usuarioId != null && !usuarioId.equals(entrada.getUsuarioId())) {
            throw new AccesoDenegadoException("La entrada no pertenece al usuario");
        }
        return EntradaResponse.de(entrada);
    }
}
