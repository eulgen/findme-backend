package com.geolink.findme.authservice.security;

import com.geolink.findme.authservice.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Adaptateur Spring Security enveloppant l'entité JPA {@link User}.
 * <p>
 * Conforme à la règle d'isolation hexagonale : l'entité {@code User} n'implémente pas
 * {@code UserDetails}. Cette classe sert de pont entre le domaine métier et Spring Security.
 * </p>
 * <p>
 * Les autorisations produites incluent :
 * <ul>
 *   <li>Les rôles préfixés par {@code ROLE_} (ex. {@code ROLE_USER}, {@code ROLE_ADMIN})</li>
 *   <li>Les permissions sous forme de code brut (ex. {@code USER_LIST_VIEW})</li>
 * </ul>
 * </p>
 */
public class UserPrincipal implements UserDetails {

    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.user = user;
        this.authorities = buildAuthorities(user);
    }

    private static Collection<? extends GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> auths = new HashSet<>();
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> {
                if (role.getName() != null) {
                    auths.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                }
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(permission -> {
                        if (permission.getCode() != null) {
                            auths.add(new SimpleGrantedAuthority(permission.getCode()));
                        }
                    });
                }
            });
        }
        return auths;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
