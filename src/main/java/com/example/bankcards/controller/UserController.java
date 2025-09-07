package com.example.bankcards.controller;


import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(){
        User user = userService.getMyAccount();
        UserResponse response = new UserResponse(user);
        return ResponseEntity.ok(response);
    }


    @PatchMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(@RequestParam String newPassword){
        userService.changeMyPassword(newPassword);
        return ResponseEntity.ok().build();
    }

    // === АДМИНСКИЕ ЭНДПОИНТЫ ===
    // @PreAuthorize("hasRole('ADMIN')") В целом это не нужно, так как в самом сервисе происходит проверка на права доступа
    @GetMapping("/all")
    public ResponseEntity<List<UserResponse>> getAllUsers(){
        List<User> users = userService.getAllUsers();
        List<UserResponse> responses = users.stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestParam String firstName, @RequestParam String lastName, @RequestParam String password, @RequestParam Role role){
        User user = userService.createUser(password, firstName, lastName, role);
        UserResponse response = new UserResponse(user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id){
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeUserRole(@PathVariable Long id, @RequestParam Role newRole){
        userService.changeUserRole(id, newRole);
        User user = userService.findUserBuId(id);
        UserResponse response = new UserResponse(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<UserResponse> findUserByName(@RequestParam String firstName, @RequestParam String lastName){
        User user = userService.findUserByName(firstName, lastName);
        UserResponse response = new UserResponse(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findUserById(@PathVariable Long id){
        User user = userService.findUserBuId(id);
        UserResponse response = new UserResponse(user);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/by-role/{role}")
    public ResponseEntity<List<UserResponse>> findUsersByRole(@PathVariable Role role){
        List<User> users = userService.findUsersByRole(role);
        List<UserResponse> responses = users.stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

}
