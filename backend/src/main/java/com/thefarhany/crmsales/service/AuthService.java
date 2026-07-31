package com.thefarhany.crmsales.service;

import com.thefarhany.crmsales.exception.BadRequestException;
import com.thefarhany.crmsales.exception.ResourceNotFoundException;
import com.thefarhany.crmsales.exception.UnauthorizedException;
import com.thefarhany.crmsales.model.dto.request.ForgotPasswordRequest;
import com.thefarhany.crmsales.model.dto.request.LoginRequest;
import com.thefarhany.crmsales.model.dto.request.ResetPasswordRequest;
import com.thefarhany.crmsales.model.dto.response.LoginResponse;
import com.thefarhany.crmsales.model.entity.User;
import com.thefarhany.crmsales.model.enums.UserStatus;
import com.thefarhany.crmsales.repository.UserRepository;
import com.thefarhany.crmsales.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.generateToken(authentication);
        log.info("Generated token: {}", token != null ? "NOT NULL" : "NULL");
        if (token != null) log.info("Token starts with: {}", token.substring(0, 10));

        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new ResourceNotFoundException("User", "username or email", request.getUsernameOrEmail()));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return LoginResponse.builder()
                .accessToken(token)
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public String refreshToken(String oldToken) {
        if (!tokenProvider.validateToken(oldToken)) {
            throw new UnauthorizedException("Invalid or expired token");
        }

        String username = tokenProvider.getUsernameFromToken(oldToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User is not active");
        }

        return tokenProvider.generateTokenFromUsername(username);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("User is not active");
        }

        String resetToken = UUID.randomUUID().toString();

        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        sendResetEmail(user.getEmail(), resetToken);
    }

    private void sendResetEmail(String email, String token) {
        String resetLink = this.frontendUrl + "/reset-password?token=" + token;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Reset Request - Digi CRM");
        message.setText("Hello,\n\n"
                + "You requested a password reset for your Digi CRM account.\n"
                + "Please click the link below to reset your password:\n\n"
                + resetLink + "\n\n"
                + "This link will expire in 1 hour.\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "Best regards,\nDigi CRM Team");

        mailSender.send(message);
        log.info("📧 Password reset email sent to: {}", email);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid reset token"));

        if (user.getResetTokenExpiry() == null ||
                user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepository.save(user);
    }

    public boolean validateResetToken(String token) {
        return userRepository.findByResetToken(token)
                .map(user -> user.getResetTokenExpiry() != null &&
                        user.getResetTokenExpiry().isAfter(LocalDateTime.now()))
                .orElse(false);
    }
}
