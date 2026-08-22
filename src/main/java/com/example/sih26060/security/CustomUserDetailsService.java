package com.example.sih26060.security;

import com.example.sih26060.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Read-only tx keeps the Hibernate session open long enough for UserPrincipal's
    // constructor to read the lazy `station` association — open-in-view is disabled, so
    // without this, station-scoped users (anyone but HQ_ADMIN) fail with a
    // LazyInitializationException that surfaces as a misleading "invalid credentials".
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user: " + username));
    }
}
