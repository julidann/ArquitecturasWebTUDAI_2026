package com.ticketya.usuarios.service;

import com.ticketya.usuarios.dto.*;
import com.ticketya.usuarios.exception.*;
import com.ticketya.usuarios.model.Rol;
import com.ticketya.usuarios.model.Usuario;
import com.ticketya.usuarios.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder encoder, JwtService jwtService) {
        this.repository = repository;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    /** Registro publico: siempre crea un CLIENTE (los otros roles los da un administrador). */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest req) {
        return UsuarioResponse.de(crear(req.email(), req.nombre(), req.password(), Rol.CLIENTE));
    }

    @Transactional
    public UsuarioResponse alta(AltaUsuarioRequest req, Rol rolSolicitante) {
        if (rolSolicitante != Rol.ADMIN) {
            throw new AccesoDenegadoException("Solo un administrador puede dar de alta usuarios con rol");
        }
        return UsuarioResponse.de(crear(req.email(), req.nombre(), req.password(), req.rol()));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        Usuario u = repository.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new CredencialesInvalidasException("Email o contraseña incorrectos"));
        // mismo mensaje si no existe o si la clave esta mal: no damos pistas de que emails estan registrados
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            log.warn("Login fallido para {}", u.getEmail());
            throw new CredencialesInvalidasException("Email o contraseña incorrectos");
        }
        if (!u.isActivo()) {
            throw new CredencialesInvalidasException("El usuario está deshabilitado");
        }
        JwtService.TokenEmitido t = jwtService.emitir(u);
        log.info("Login de {} ({})", u.getId(), u.getRol());
        return new TokenResponse(t.token(), "Bearer", t.expiraEn(), UsuarioResponse.de(u));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.de(buscar(id));
    }

    @Transactional(readOnly = true)
    public ContactoResponse contacto(Long id) {
        Usuario u = buscar(id);
        return new ContactoResponse(u.getId(), u.getEmail(), u.getNombre(), u.isActivo());
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Rol filtro, Rol rolSolicitante) {
        if (rolSolicitante != Rol.ADMIN) {
            throw new AccesoDenegadoException("Solo un administrador puede listar usuarios");
        }
        List<Usuario> lista = filtro == null ? repository.findAll() : repository.findByRolOrderById(filtro);
        return lista.stream().map(UsuarioResponse::de).toList();
    }

    @Transactional
    public UsuarioResponse cambiarActivo(Long id, boolean activo, Rol rolSolicitante) {
        if (rolSolicitante != Rol.ADMIN) {
            throw new AccesoDenegadoException("Solo un administrador puede habilitar o deshabilitar usuarios");
        }
        Usuario u = buscar(id);
        u.setActivo(activo);
        return UsuarioResponse.de(u);
    }

    private Usuario crear(String email, String nombre, String password, Rol rol) {
        String normalizado = email.trim().toLowerCase();
        if (repository.existsByEmail(normalizado)) {
            throw new OperacionInvalidaException("Ya existe un usuario con el email " + normalizado);
        }
        try {
            Usuario u = repository.saveAndFlush(new Usuario(normalizado, nombre, encoder.encode(password), rol));
            log.info("Usuario {} creado con rol {}", u.getId(), rol);
            return u;
        } catch (DataIntegrityViolationException e) {
            throw new OperacionInvalidaException("Ya existe un usuario con el email " + normalizado);
        }
    }

    private Usuario buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + id));
    }
}
