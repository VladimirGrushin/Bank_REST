package com.example.bankcards.service;


import com.example.bankcards.dto.request.AuthRequest;
import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.bankcards.entity.Role;
import java.util.Date;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final long jwtExpirationMs = 86400000; // 24 часа
    private final SecretKey jwtSecretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);


    public AuthResponse authenticate(AuthRequest authRequest){
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.getFirstName() + " " + authRequest.getLastName(), authRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByFirstNameAndLastName(
                authRequest.getFirstName(),
                authRequest.getLastName()
        ).orElseThrow(() -> new ResourceNotFoundException("User", "firstName",  authRequest.getFirstName(), "lastName", authRequest.getLastName()));

        String accessToken = generateAccessToken(user);
        return createAuthResponse(user, accessToken);
    }

    public AuthResponse register(AuthRequest authRequest) {
        if (userRepository.existsByFirstNameAndLastName(authRequest.getFirstName(), authRequest.getLastName()))
            throw new BadRequestException("User with name '" + authRequest.getFirstName() + " " + authRequest.getLastName() + "' already exists");


        User user = new User();
        user.setFirstName(authRequest.getFirstName());
        user.setLastName(authRequest.getLastName());
        user.setPassword(passwordEncoder.encode(authRequest.getPassword()));
        user.setRole(Role.ROLE_USER);

        User savedUser = userRepository.save(user);

        String accessToken = generateAccessToken(savedUser);

        return createAuthResponse(savedUser, accessToken);
    }

    private String generateAccessToken(User user){
        return Jwts.builder()
                .setSubject(createUsername(user.getFirstName(), user.getLastName()))
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(jwtSecretKey)
                .compact();
    }

    private AuthResponse createAuthResponse(User user, String accessToken) {
        AuthResponse response = new AuthResponse();
        response.setToken(accessToken);
        response.setUserId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        return response;
    }

    private String createUsername(String firstName, String lastName) {
        return firstName + " " + lastName;
    }

    public void logout() {
        SecurityContextHolder.clearContext();
    }

}
