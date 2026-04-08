package DAJ2EE.demo.config;

import DAJ2EE.demo.service.CustomUserDetailsService;
import DAJ2EE.demo.service.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Bật Method Security để dùng @PreAuthorize
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Khai báo rõ ràng DaoAuthenticationProvider gắn UserDetailsService + PasswordEncoder
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // Spring Security 7: constructor nhận UserDetailsService, sau đó set PasswordEncoder
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // Expose AuthenticationManager để Spring Security dùng provider ở trên
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider()) // Gắn provider vào filter chain
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/images/**", "/uploads/**", "/admin/login", "/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/tenant/**").hasRole("TENANT")
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(customAuthenticationSuccessHandler())
                .failureHandler(customAuthenticationFailureHandler())
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestResolver(authorizationRequestResolver(clientRegistrationRepository))
                )
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(customAuthenticationSuccessHandler())
                .failureHandler(customAuthenticationFailureHandler())
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutRequestMatcher(request -> "/logout".equals(request.getServletPath()) &&
                    ("GET".equals(request.getMethod()) || "POST".equals(request.getMethod())))
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }

    private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization");
        authorizationRequestResolver.setAuthorizationRequestCustomizer(
                customizer -> customizer.additionalParameters(params -> params.put("prompt", "select_account")));
        return authorizationRequestResolver;
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(grant -> grant.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin) {
                response.sendRedirect("/admin");
            } else {
                response.sendRedirect("/tenant");
            }
        };
    }

    @Bean
    public AuthenticationFailureHandler customAuthenticationFailureHandler() {
        return (request, response, exception) -> {
            String referer = request.getHeader("Referer");
            boolean isDisabled = isOrCausedByDisabled(exception);
            
            if (referer != null && referer.contains("/admin/login")) {
                response.sendRedirect("/admin/login?" + (isDisabled ? "disabled" : "error"));
            } else {
                response.sendRedirect("/login?" + (isDisabled ? "disabled" : "error"));
            }
        };
    }

    private boolean isOrCausedByDisabled(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof DisabledException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
