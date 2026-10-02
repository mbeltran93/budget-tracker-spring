package com.mbeltran.budgettracker.service;

import com.mbeltran.budgettracker.dto.RegisterForm;
import com.mbeltran.budgettracker.entity.User;
import com.mbeltran.budgettracker.exception.DuplicateResourceException;
import com.mbeltran.budgettracker.repository.UserRepository;
import com.mbeltran.budgettracker.security.AppUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    private UserService service() {
        return new UserService(userRepository, passwordEncoder);
    }

    @Test
    void loadsUserByEmailAsPrincipal() {
        userService = service();
        User user = User.builder().id(1L).name("Demo").email("demo@test.com").password("hash").build();
        when(userRepository.findByEmail("demo@test.com")).thenReturn(Optional.of(user));

        UserDetails details = userService.loadUserByUsername("demo@test.com");

        assertThat(details).isInstanceOf(AppUserPrincipal.class);
        assertThat(details.getUsername()).isEqualTo("demo@test.com");
    }

    @Test
    void throwsWhenEmailNotFound() {
        userService = service();
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("missing@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void registersNewUserWithEncodedPassword() {
        userService = service();
        RegisterForm form = new RegisterForm();
        form.setName("Nuevo");
        form.setEmail("nuevo@test.com");
        form.setPassword("plain1234");

        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(passwordEncoder.encode("plain1234")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.register(form);

        assertThat(saved.getPassword()).isEqualTo("ENCODED");
        assertThat(saved.getEmail()).isEqualTo("nuevo@test.com");
    }

    @Test
    void rejectsRegistrationWithDuplicateEmail() {
        userService = service();
        RegisterForm form = new RegisterForm();
        form.setName("Nuevo");
        form.setEmail("existe@test.com");
        form.setPassword("plain1234");

        when(userRepository.existsByEmail("existe@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(form))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }
}
