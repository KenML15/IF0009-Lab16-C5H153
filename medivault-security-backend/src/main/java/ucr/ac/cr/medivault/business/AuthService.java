package ucr.ac.cr.medivault.business;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucr.ac.cr.medivault.controller.dto.LoginRequestDto;
import ucr.ac.cr.medivault.controller.dto.LoginResponseDto;
import ucr.ac.cr.medivault.data.UsuarioRepository;
import ucr.ac.cr.medivault.domain.Usuario;
import ucr.ac.cr.medivault.security.JwtTokenProvider;

@Service
public class AuthService {

    private final UsuarioRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider jwtProvider;
    private final long expirationMs;

    public AuthService(UsuarioRepository userRepo,
                       PasswordEncoder encoder,
                       JwtTokenProvider jwtProvider,
                       @Value("${medivault.security.jwt.expiration-ms}") long expirationMs) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtProvider = jwtProvider;
        this.expirationMs = expirationMs;
    }

    @Transactional(readOnly = true)
    public LoginResponseDto autenticar(LoginRequestDto request) {
        Usuario u = userRepo.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas."));

        if (!encoder.matches(request.password(), u.getPasswordHash())
                || !"ACTIVO".equals(u.getEstado())) {
            throw new BadCredentialsException("Credenciales invalidas.");
        }

        String token = jwtProvider.generateToken(u.getUsername(), u.getRol().name());
        return new LoginResponseDto(token, "Bearer", expirationMs,
                u.getUsername(), u.getRol().name());
    }
}