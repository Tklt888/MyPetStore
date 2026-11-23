package org.csu.mypetstore.user.service;

import org.csu.mypetstore.user.domain.StoreUser;
import org.csu.mypetstore.user.mapper.StoreUserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final StoreUserMapper users; private final PasswordEncoder encoder;
    public AccountService(StoreUserMapper users,PasswordEncoder encoder){this.users=users;this.encoder=encoder;}
    @Transactional public void register(String username,String password,String displayName,String email,String phone){
        username=clean(username,50,"用户名"); displayName=clean(displayName,50,"显示名称");
        if(password==null||password.length()<8||password.length()>72)throw new StoreException("密码长度应为 8—72 个字符");
        if(email!=null&&!email.isBlank()&&email.strip().length()>100)throw new StoreException("邮箱长度不能超过 100 个字符");
        StoreUser user=new StoreUser();user.setUsername(username);user.setPasswordHash(encoder.encode(password));user.setDisplayName(displayName);user.setEmail(blank(email));user.setPhone(blank(phone));user.setRole("CUSTOMER");user.setStatus("ENABLED");
        try{users.insert(user);}catch(DuplicateKeyException e){throw new StoreException("该用户名已被使用");}
    }
    private static String clean(String value,int max,String label){if(value==null||value.strip().isEmpty())throw new StoreException(label+"不能为空");String v=value.strip();if(v.length()>max)throw new StoreException(label+"长度不能超过 "+max+" 个字符");return v;}
    private static String blank(String v){return v==null||v.isBlank()?null:v.strip();}
}
