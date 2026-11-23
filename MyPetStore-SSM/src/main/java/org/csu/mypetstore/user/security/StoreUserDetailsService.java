package org.csu.mypetstore.user.security;

import org.csu.mypetstore.user.domain.StoreUser;
import org.csu.mypetstore.user.mapper.StoreUserMapper;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class StoreUserDetailsService implements UserDetailsService {
    private final StoreUserMapper users;public StoreUserDetailsService(StoreUserMapper users){this.users=users;}
    public UserDetails loadUserByUsername(String username){StoreUser u=users.findByUsername(username==null?"":username.strip());if(u==null||!"CUSTOMER".equals(u.getRole()))throw new UsernameNotFoundException("账号或密码错误");return StorePrincipal.from(u);}
}
