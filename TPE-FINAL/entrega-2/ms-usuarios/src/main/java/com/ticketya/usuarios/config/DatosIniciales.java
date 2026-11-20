package com.ticketya.usuarios.config;

import com.ticketya.usuarios.model.Rol;
import com.ticketya.usuarios.model.Usuario;
import com.ticketya.usuarios.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Usuarios de prueba (solo si la tabla esta vacia). Los ids quedan fijos porque se crean en orden:
 * 1 admin, 2 organizador, 3 organizador2, 4 cliente, 5 cliente2, 6 acceso.
 */
@Component
@ConditionalOnProperty(name = "usuarios.cargar-iniciales", havingValue = "true", matchIfMissing = true)
public class DatosIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;

    public DatosIniciales(UsuarioRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        crear("admin@ticketya.com", "Ana Admin", "admin123", Rol.ADMIN);
        crear("organizador@ticketya.com", "Productora Sur", "organizador123", Rol.ORGANIZADOR);
        crear("organizador2@ticketya.com", "Teatro Tandil", "organizador123", Rol.ORGANIZADOR);
        crear("cliente@ticketya.com", "Carla Cliente", "cliente123", Rol.CLIENTE);
        crear("cliente2@ticketya.com", "Mateo Cliente", "cliente123", Rol.CLIENTE);
        crear("acceso@ticketya.com", "Puerta 1", "acceso123", Rol.ACCESO);
        log.info("Usuarios de prueba creados (ver README)");
    }

    private void crear(String email, String nombre, String password, Rol rol) {
        repository.save(new Usuario(email, nombre, encoder.encode(password), rol));
    }
}
