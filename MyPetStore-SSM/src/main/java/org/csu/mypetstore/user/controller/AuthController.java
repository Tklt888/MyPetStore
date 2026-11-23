package org.csu.mypetstore.user.controller;

import jakarta.servlet.http.*;import org.csu.mypetstore.user.service.*;import org.springframework.http.*;import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final CaptchaService captcha;private final AccountService accounts;public AuthController(CaptchaService c,AccountService a){captcha=c;accounts=a;}
    @GetMapping("/login") String login(@RequestParam(required=false)String error,@RequestParam(required=false)String expired,HttpSession session,Model model){if(expired!=null){model.addAttribute("error","账号已停用或权限已变化，请重新登录");}else if(error!=null){Object e=session.getAttribute("LOGIN_CAPTCHA_ERROR");session.removeAttribute("LOGIN_CAPTCHA_ERROR");model.addAttribute("error",e==null?"账号、密码或状态不正确":e);}return "SignonForm";}
    @GetMapping(value="/captcha.svg",produces="image/svg+xml") ResponseEntity<String> captcha(HttpSession session){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(captcha.svg(captcha.issue(session)));}
    @GetMapping("/register") String register(){return "Register";}
    @PostMapping("/register") String register(@RequestParam String username,@RequestParam String password,@RequestParam String displayName,@RequestParam(required=false)String email,@RequestParam(required=false)String phone,@RequestParam String captcha,HttpSession session,RedirectAttributes redirect,Model model){
        try{if(!this.captcha.verify(session,captcha))throw new StoreException("验证码不正确或已失效");accounts.register(username,password,displayName,email,phone);redirect.addFlashAttribute("message","注册成功，请登录");return "redirect:/login";}catch(StoreException e){model.addAttribute("error",e.getMessage());model.addAttribute("username",username);model.addAttribute("displayName",displayName);model.addAttribute("email",email);model.addAttribute("phone",phone);return "Register";}
    }
}
