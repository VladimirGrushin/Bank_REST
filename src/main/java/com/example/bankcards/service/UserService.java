package com.example.bankcards.service;


import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import javax.transaction.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //Получить текущего пользователя для дальнейшей проверки прав доступа
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByFirstNameAndLastName(username.split(" ")[0], username.split(" ")[1])
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void isUserAdmin() {
        User currentUser = getCurrentUser();
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Only administrators can perform this action");
        }
    }

    // ==== МЕТОДЫ АДМИНА ====

    public User createUser(String password, String firstName, String lastName, Role role){
        isUserAdmin();
        if (userRepository.existsByFirstNameAndLastName(firstName, lastName)) throw new RuntimeException("Such user already exists");
        User user = new User( passwordEncoder.encode(password), firstName, lastName, role);
        return userRepository.save(user);
    }

    public void deleteUser(Long id){
        isUserAdmin();
        User userToDelete = findUserBuId(id);
        User currentUser = getCurrentUser();
        if (userToDelete.getId().equals(currentUser.getId())) throw new RuntimeException("You can not delete your own account");
        userRepository.delete(userToDelete);
    }

    public void changeUserRole(Long id, Role role){
        isUserAdmin();
        User user = findUserBuId(id);
        user.setRole(role);
        userRepository.save(user);
    }

    public User findUserBuId(Long id){
        isUserAdmin();
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No user with such id"));
    }

    public User findUserByName(String firstName, String lastName){
        isUserAdmin();
        return userRepository.findByFirstNameAndLastName(firstName, lastName)
                .orElseThrow(() -> new RuntimeException("No user with such name"));
    }

    public List<User> findUsersByRole(Role role){
        isUserAdmin();
        return userRepository.findByRole(role);
    }

    public List<User> getAllUsers(){
        isUserAdmin();
        return userRepository.findAll();
    }

    // ==== ОБЩИЕ МЕТОДЫ ====
    public User getMyAccount(){
        return getCurrentUser();
    }

    public void changeMyPassword(String newPassword){
        User currentUser = getCurrentUser();
        currentUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(currentUser);
    }


}

