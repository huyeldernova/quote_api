package com.example.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Entity @Table(name="users")
@NoArgsConstructor @AllArgsConstructor @Builder @Getter @Setter
public class User implements UserDetails {

    @Id @GeneratedValue(strategy=GenerationType.UUID)
    private String id;

    @Column(nullable=false)
    private String name;

    @Column(unique=true,nullable=false)
    private String email;

    private String password;
    private String company;

    @OneToMany(mappedBy="user", cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @Builder.Default
    private List<UserHasRole> userHasRoles = new ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userHasRoles.stream()
                .map(r -> new SimpleGrantedAuthority(r.getRole().getName()))
                .toList();
    }

    public void addRole(Role role) {
        userHasRoles.add(UserHasRole.builder().user(this).role(role).build());
    }

    // Spring Security uses this as the principal name (username = email for login)
    @Override public String getUsername() { return this.email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
