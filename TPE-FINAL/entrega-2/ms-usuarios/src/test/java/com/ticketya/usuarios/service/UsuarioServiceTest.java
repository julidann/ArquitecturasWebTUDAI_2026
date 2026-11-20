package com.ticketya.usuarios.service;

import com.ticketya.usuarios.dto.*;
import com.ticketya.usuarios.exception.AccesoDenegadoException;
import com.ticketya.usuarios.exception.CredencialesInvalidasException;
import com.ticketya.usuarios.exception.OperacionInvalidaException;
import com.ticketya.usuarios.model.Rol;
import com.ticketya.usuarios.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class UsuarioServiceTest {

    @Autowired
    UsuarioService service;
    @Autowired
    JwtService jwtService;
    @Autowired
    UsuarioRepository repository;

    @BeforeEach
    void limpiar() {
        repository.deleteAll();
    }

    @Test
    void elRegistroPublicoSiempreCreaUnCliente() {
        UsuarioResponse u = service.registrar(new RegistroRequest("Juana@Mail.com", "Juana", "secreta123"));
        assertThat(u.rol()).isEqualTo(Rol.CLIENTE);
        assertThat(u.email()).isEqualTo("juana@mail.com");
        // la clave no se guarda en texto plano
        assertThat(repository.findById(u.id()).orElseThrow().getPasswordHash()).isNotEqualTo("secreta123").startsWith("$2");
    }

    @Test
    void noSePuedeRegistrarDosVecesElMismoEmail() {
        service.registrar(new RegistroRequest("juana@mail.com", "Juana", "secreta123"));
        assertThatThrownBy(() -> service.registrar(new RegistroRequest("JUANA@mail.com", "Otra", "secreta123")))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void elLoginDevuelveUnJwtConElIdYElRol() {
        UsuarioResponse u = service.registrar(new RegistroRequest("juana@mail.com", "Juana", "secreta123"));
        TokenResponse t = service.login(new LoginRequest("juana@mail.com", "secreta123"));

        Claims claims = jwtService.leer(t.token());
        assertThat(claims.getSubject()).isEqualTo(String.valueOf(u.id()));
        assertThat(claims.get("rol", String.class)).isEqualTo("CLIENTE");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        assertThat(t.tipo()).isEqualTo("Bearer");
    }

    @Test
    void elLoginConClaveIncorrectaOEmailInexistenteDa401ConElMismoMensaje() {
        service.registrar(new RegistroRequest("juana@mail.com", "Juana", "secreta123"));
        assertThatThrownBy(() -> service.login(new LoginRequest("juana@mail.com", "otra")))
                .isInstanceOf(CredencialesInvalidasException.class).hasMessage("Email o contraseña incorrectos");
        assertThatThrownBy(() -> service.login(new LoginRequest("nadie@mail.com", "otra")))
                .isInstanceOf(CredencialesInvalidasException.class).hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void soloUnAdminPuedeCrearUsuariosConRol() {
        AltaUsuarioRequest req = new AltaUsuarioRequest("puerta@ticketya.com", "Puerta", "acceso123", Rol.ACCESO);
        assertThatThrownBy(() -> service.alta(req, Rol.ORGANIZADOR)).isInstanceOf(AccesoDenegadoException.class);
        assertThat(service.alta(req, Rol.ADMIN).rol()).isEqualTo(Rol.ACCESO);
    }

    @Test
    void unUsuarioDeshabilitadoNoPuedeIniciarSesion() {
        UsuarioResponse u = service.registrar(new RegistroRequest("juana@mail.com", "Juana", "secreta123"));
        service.cambiarActivo(u.id(), false, Rol.ADMIN);
        assertThatThrownBy(() -> service.login(new LoginRequest("juana@mail.com", "secreta123")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }
}
