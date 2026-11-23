package org.csu.mypetstore.user.controller;

import org.csu.mypetstore.user.security.StorePrincipal;import org.csu.mypetstore.user.service.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {
    private final CartService carts;public CartController(CartService c){carts=c;}
    @GetMapping("/cart")String cart(@AuthenticationPrincipal StorePrincipal user,Model m){var lines=carts.lines(user.id());m.addAttribute("lines",lines);m.addAttribute("total",carts.total(lines));return "Cart";}
    @PostMapping("/cart/items")String add(@AuthenticationPrincipal StorePrincipal user,@RequestParam long productId,@RequestParam int quantity,RedirectAttributes r){try{carts.add(user.id(),productId,quantity);r.addFlashAttribute("message","已加入购物车");return "redirect:/cart";}catch(StoreException e){r.addFlashAttribute("error",e.getMessage());return "redirect:/product?id="+productId;}}
    @PostMapping("/cart/items/{id}")String update(@AuthenticationPrincipal StorePrincipal user,@PathVariable long id,@RequestParam int quantity,@RequestParam long version,RedirectAttributes r){act(r,()->carts.update(user.id(),id,quantity,version),"购物车数量已更新");return "redirect:/cart#item-"+id;}
    @PostMapping("/cart/items/{id}/remove")String remove(@AuthenticationPrincipal StorePrincipal user,@PathVariable long id,@RequestParam long version,RedirectAttributes r){act(r,()->carts.remove(user.id(),id,version),"商品已移出购物车");return "redirect:/cart";}
    private static void act(RedirectAttributes r,Runnable action,String ok){try{action.run();r.addFlashAttribute("message",ok);}catch(StoreException e){r.addFlashAttribute("error",e.getMessage());}}
}
