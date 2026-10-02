package com.mbeltran.budgettracker.service;

import com.mbeltran.budgettracker.entity.User;
import com.mbeltran.budgettracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserProvisioningService service() {
        return new UserProvisioningService(userRepository, passwordEncoder);
    }

    @Test
    void createsNewUserWithRandomPlaceholderPasswordWhenEmailIsNew() {
        UserProvisioningService service = service();
        when(userRepository.findByEmail("nuevo.google@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("ENCODED-RANDOM");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            toSave.setId(42L);
            return toSave;
        });

        User result = service.findOrCreateFromOAuth("google", "nuevo.google@test.com", "Persona Nueva");

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getEmail()).isEqualTo("nuevo.google@test.com");
        assertThat(result.getName()).isEqualTo("Persona Nueva");
        assertThat(result.getPassword()).isEqualTo("ENCODED-RANDOM");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void fallsBackToEmailAsNameWhenProviderNameIsBlank() {
        UserProvisioningService service = service();
        when(userRepository.findByEmail("sinnombre@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.findOrCreateFromOAuth("facebook", "sinnombre@test.com", "  ");

        assertThat(result.getName()).isEqualTo("sinnombre@test.com");
    }

    @Test
    void linksExistingUserInsteadOfCreatingADuplicate() {
        UserProvisioningService service = service();
        User existing = User.builder().id(7L).name("Ya Existe").email("existe@test.com").password("hash").build();
        when(userRepository.findByEmail("existe@test.com")).thenReturn(Optional.of(existing));

        User result = service.findOrCreateFromOAuth("google", "existe@test.com", "Nombre Del Proveedor");

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result).isSameAs(existing);
        verify(userRepository, never()).save(any());
    }
}
