package org.csu.mypetstore.user.domain;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("store_user")
public class StoreUser {
    @TableId(type = IdType.AUTO) private Long id;
    private String username; private String passwordHash; private String displayName; private String role; private String status; private String email; private String phone; private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getUsername(){return username;} public void setUsername(String v){username=v;} public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;} public String getDisplayName(){return displayName;} public void setDisplayName(String v){displayName=v;} public String getRole(){return role;} public void setRole(String v){role=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getPhone(){return phone;} public void setPhone(String v){phone=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
