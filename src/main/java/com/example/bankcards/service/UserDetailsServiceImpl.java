package com.example.bankcards.service;

import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // username = "FirstName LastName"
        String[] names = username.split(" ", 2);
        if (names.length != 2) {
            throw new UsernameNotFoundException("Invalid username format. Expected 'FirstName LastName'");
        }

        String firstName = names[0];
        String lastName = names[1];

        User user = userRepository.findByFirstNameAndLastName(firstName, lastName)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with name: " + firstName + " " + lastName));

        return org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password(user.getPassword())
                .roles(user.getRole().name().replace("ROLE_", "")) // "ROLE_ADMIN" -> "ADMIN"
                .build();
    }
}