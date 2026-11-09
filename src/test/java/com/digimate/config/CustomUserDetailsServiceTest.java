package com.digimate.config;

import com.digimate.entity.User;
import com.digimate.entity.enums.HelperStatus;
import com.digimate.entity.enums.Role;
import com.digimate.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for loading stored credentials and translating account roles to authorities.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    private static final String EMAIL = "anna.novak@example.com";
    private static final String PASSWORD_HASH = "synthetic-stored-password-hash";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void shouldReturnStoredEmailAsUsername() {
        User user = user(Role.USER, HelperStatus.NONE);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername(EMAIL);

        assertThat(details.getUsername()).isEqualTo(EMAIL);
    }

    @Test
    void shouldReturnStoredPasswordHash() {
        User user = user(Role.USER, HelperStatus.NONE);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername(EMAIL);

        assertThat(details.getPassword()).isEqualTo(PASSWORD_HASH);
    }

    @Test
    void shouldGrantUserAuthority() {
        User user = user(Role.USER, HelperStatus.NONE);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername(EMAIL);

        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void shouldGrantAdminAuthority() {
        User user = user(Role.ADMIN, HelperStatus.NONE);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername(EMAIL);

        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void shouldRejectUnknownEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(EMAIL));
    }

    @ParameterizedTest
    @EnumSource(value = HelperStatus.class, names = {"PENDING", "VERIFIED"})
    void shouldNotGrantHelperAuthorityFromHelperStatus(HelperStatus helperStatus) {
        User user = user(Role.USER, helperStatus);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername(EMAIL);

        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    private static User user(Role role, HelperStatus helperStatus) {
        User user = new User();
        user.setId(17L);
        user.setFullName("Anna Novak");
        user.setEmail(EMAIL);
        user.setPasswordHash(PASSWORD_HASH);
        user.setRole(role);
        user.setHelperStatus(helperStatus);
        return user;
    }
}
