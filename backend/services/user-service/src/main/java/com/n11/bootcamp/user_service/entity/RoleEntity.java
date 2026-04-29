package com.n11.bootcamp.user_service.entity;

import com.n11.bootcamp.common_lib.auth.enums.Role;
import com.n11.bootcamp.common_lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "roles")
public class RoleEntity extends BaseEntity implements GrantedAuthority {

    @Enumerated(EnumType.STRING)
    private Role authority;

    @Override
    public String getAuthority() {
        return authority.getValue();
    }
}
