package org.csu.mypetstore.user.controller;

import org.csu.mypetstore.user.security.StorePrincipal;import org.csu.mypetstore.user.service.CartService;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {
    private final CartService carts;public GlobalModelAdvice(CartService carts){this.carts=carts;}
    @ModelAttribute("currentUser") public StorePrincipal user(Authentication auth){return auth!=null&&auth.getPrincipal() instanceof StorePrincipal p?p:null;}
    @ModelAttribute("cartCount") public int cartCount(Authentication auth){try{return auth!=null&&auth.getPrincipal() instanceof StorePrincipal p?carts.count(p.id()):0;}catch(Exception ignored){return 0;}}
}
