package org.csu.mypetstore.admin.controller;

import jakarta.servlet.http.HttpSession;
import org.csu.mypetstore.admin.security.AdminAuthenticationFailureHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String expired,
            HttpSession session,
            Model model) {
        Object rememberedUsername = session.getAttribute(AdminAuthenticationFailureHandler.LOGIN_USERNAME);
        session.removeAttribute(AdminAuthenticationFailureHandler.LOGIN_USERNAME);
        model.addAttribute("username", rememberedUsername == null ? "" : rememberedUsername);

        if (error != null) {
            model.addAttribute("message", "账号或密码错误，或账号不可用");
        } else if (expired != null) {
            model.addAttribute("message", "登录状态已失效，请重新登录");
        } else if (logout != null) {
            model.addAttribute("message", "已安全退出");
        }
        return "login";
    }
}
