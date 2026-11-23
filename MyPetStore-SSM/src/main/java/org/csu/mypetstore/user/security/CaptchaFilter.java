package org.csu.mypetstore.user.security;

import jakarta.servlet.*;import jakarta.servlet.http.*;
import org.csu.mypetstore.user.service.CaptchaService;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class CaptchaFilter extends OncePerRequestFilter {
    private final CaptchaService captcha;public CaptchaFilter(CaptchaService captcha){this.captcha=captcha;}
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        if("POST".equals(request.getMethod())&&"/login".equals(request.getServletPath())&&!captcha.verify(request.getSession(),request.getParameter("captcha"))){request.getSession().setAttribute("LOGIN_CAPTCHA_ERROR","验证码不正确或已失效");response.sendRedirect("/login?error");return;}chain.doFilter(request,response);
    }
}
