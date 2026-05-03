package com.n11.bootcamp;

import com.n11.bootcamp.common_lib.auth.enums.Role;
import com.n11.bootcamp.user_service.entity.RoleEntity;
import com.n11.bootcamp.user_service.entity.User;
import com.n11.bootcamp.user_service.repository.RoleRepository;
import com.n11.bootcamp.user_service.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@SpringBootApplication
public class UserServiceApplication implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceApplication(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

    @Override
    public void run(String... args) {
        addDefaultUsers();
    }

    public void addDefaultUsers() {

        if (roleRepository.count() > 0) return;

        RoleEntity r1 = new RoleEntity();
        r1.setAuthority(Role.ADMIN);

        RoleEntity r2 = new RoleEntity();
        r2.setAuthority(Role.USER);

        roleRepository.saveAll(List.of(r1, r2));

        User u1 = new User();
        u1.setEmail("admin@test.com");
        u1.setPassword(passwordEncoder.encode("admin123"));
        u1.setFirstName("Admin");
        u1.setLastName("User");
        u1.setPhone("5555555555");
        u1.setRoles(List.of(r1));
        userRepository.save(u1);

        User u2 = new User();
        u2.setEmail("test@test.com");
        u2.setPassword(passwordEncoder.encode("test123"));
        u1.setPhone("5555555556");
        u1.setFirstName("Test");
        u1.setLastName("User");
        u2.setRoles(List.of(r2));
        userRepository.save(u2);
    }
}
