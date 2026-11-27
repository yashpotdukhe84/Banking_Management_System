package com.sprint.bank.controller;

import com.sprint.bank.dto.AuthResponse;
import com.sprint.bank.dto.LoginRequest;
import com.sprint.bank.dto.RegisterRequest;
import com.sprint.bank.repository.UserRepository;
import com.sprint.bank.security.JwtFilter;
import com.sprint.bank.service.AuthService;
import com.sprint.bank.shared.entity.User;
import com.sprint.bank.shared.exception.DuplicateResourceException;
import com.sprint.bank.shared.exception.InvalidCredentialsException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/** Serves simple Thymeleaf pages for login, register, dashboard, and logout. */
@Controller
public class AuthPageController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthPageController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        return isLoggedIn(authentication) ? "redirect:/dashboard" : "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginPage(Authentication authentication, Model model) {
        if (isLoggedIn(authentication)) {
            return "redirect:/dashboard";
        }
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
                              BindingResult bindingResult,
                              HttpServletResponse response,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Please enter a valid email and password.");
            return "login";
        }
        try {
            AuthResponse auth = authService.login(loginRequest);
            addAuthCookie(response, auth);
            return "redirect:/dashboard?login=true";
        } catch (InvalidCredentialsException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "login";
        }
    }

    @GetMapping("/register")
    public String showRegisterPage(Authentication authentication, Model model) {
        if (isLoggedIn(authentication)) {
            return "redirect:/dashboard";
        }
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                                 BindingResult bindingResult,
                                 HttpServletResponse response,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Please fix the highlighted fields.");
            return "register";
        }
        try {
            AuthResponse auth = authService.register(registerRequest);
            addAuthCookie(response, auth);
            return "redirect:/dashboard?registered=true";
        } catch (DuplicateResourceException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "register";
        }
    }

    @GetMapping("/dashboard")
    public String showDashboard(Authentication authentication, Model model) {
        if (!isLoggedIn(authentication)) {
            return "redirect:/login";
        }
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("email", user.getEmail());
        model.addAttribute("role", user.getRole().name());
        model.addAttribute("fullName",
                user.getCustomerProfile() != null ? user.getCustomerProfile().getFullName() : "Admin");
        return "dashboard";
    }

    @PostMapping("/logout")
    public String handleLogout(HttpServletRequest request, HttpServletResponse response) {
        String token = extractTokenFromCookie(request);
        if (token != null) {
            authService.logout(token);
        }
        clearAuthCookie(response);
        return "redirect:/login?logout=true";
    }

    private boolean isLoggedIn(Authentication authentication) {
        return authentication != null
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.isAuthenticated();
    }

    private void addAuthCookie(HttpServletResponse response, AuthResponse auth) {
        Cookie cookie = new Cookie(JwtFilter.TOKEN_COOKIE_NAME, auth.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) auth.getExpiresInSeconds());
        response.addCookie(cookie);
    }

    private void clearAuthCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(JwtFilter.TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (JwtFilter.TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
