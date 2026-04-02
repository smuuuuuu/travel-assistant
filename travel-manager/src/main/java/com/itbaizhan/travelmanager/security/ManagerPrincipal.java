package com.itbaizhan.travelmanager.security;

import com.itbaizhan.travelcommon.pojo.ManagerUser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class ManagerPrincipal implements UserDetails {

    private final ManagerUser managerUser;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = managerUser.getRole() != null ? managerUser.getRole() : "ROLE_ADMIN";
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return managerUser.getPassword();
    }

    @Override
    public String getUsername() {
        return managerUser.getUsername();
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
        return managerUser.getEnabled() == null || managerUser.getEnabled() != 0;
    }
}
