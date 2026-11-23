package org.csu.mypetstore.user.security;

import org.csu.mypetstore.user.domain.StoreUser;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.*;

public record StorePrincipal(long id,String username,String password,String displayName,String status) implements UserDetails {
    public static StorePrincipal from(StoreUser u){return new StorePrincipal(u.getId(),u.getUsername(),u.getPasswordHash(),u.getDisplayName(),u.getStatus());}
    public Collection<? extends GrantedAuthority> getAuthorities(){return List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));}public String getPassword(){return password;}public String getUsername(){return username;}public boolean isEnabled(){return "ENABLED".equals(status);}
}
