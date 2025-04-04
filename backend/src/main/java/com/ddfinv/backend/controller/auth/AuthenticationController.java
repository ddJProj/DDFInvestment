package com.ddfinv.backend.controller.auth;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ddfinv.backend.dto.auth.AuthenticationRequest;
import com.ddfinv.backend.dto.auth.AuthenticationResponse;
import com.ddfinv.backend.dto.auth.RegisterAuthRequest;
import com.ddfinv.backend.exception.ApplicationException;
import com.ddfinv.backend.exception.security.InvalidPasswordException;
import com.ddfinv.backend.exception.validation.EmailValidationException;
import com.ddfinv.backend.service.auth.AuthenticationService;
import com.ddfinv.backend.service.auth.TokenBlacklistService;
import com.ddfinv.core.domain.UserAccount;
import com.ddfinv.core.repository.UserAccountRepository;



@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthenticationController {

    private final TokenBlacklistService tokenBlacklist;
    private final AuthenticationService authenticationService;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationController(AuthenticationService authenticationService, PasswordEncoder passwordEncoder, UserAccountRepository userAccountRepository, TokenBlacklistService tokenBlacklist){
        this.authenticationService = authenticationService;
        this.passwordEncoder = passwordEncoder;
        this.userAccountRepository = userAccountRepository;
        this.tokenBlacklist = tokenBlacklist;

    }

    /**
     * 
     * @param request
     * @return
     * @throws EmailValidationException 
     */
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterAuthRequest request) throws ApplicationException {
        return ResponseEntity.ok(authenticationService.register(request));
        
    }

    /**
     * 
     * @param request
     * @return
     * @throws InvalidPasswordException 
     */
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) throws InvalidPasswordException {
        return ResponseEntity.ok(authenticationService.authenticate(request));
        
    }

    /**
     * blacklists user's token, logs user from system
     * 
     * @param authHeader
     * @return
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")){
            String token = authHeader.substring(7);
            tokenBlacklist.blacklistToken(token);
        }
        Map<String, String> response = new HashMap<>();
        response.put("message", "You have successfully logged out of the system.");
        return ResponseEntity.ok(response);
    }
    


    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("password");
        
        try {
            UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            // hash the new password
            String hashedPassword = passwordEncoder.encode(newPassword);
            user.setHashedPassword(hashedPassword);
            userAccountRepository.save(user);
            
            return ResponseEntity.ok("Password reset successful. New hash: " + hashedPassword);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testAuthEndpoint() {
        return ResponseEntity.ok("Endpoint is correctly authorizing.");
    }
    

    
}
