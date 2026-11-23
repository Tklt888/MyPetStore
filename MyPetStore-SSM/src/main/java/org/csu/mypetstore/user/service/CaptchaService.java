package org.csu.mypetstore.user.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class CaptchaService {
    public static final String KEY="USER_CAPTCHA"; private final SecureRandom random=new SecureRandom();
    public String issue(HttpSession session){String value=String.format("%04d",random.nextInt(10000));session.setAttribute(KEY,value);return value;}
    public boolean verify(HttpSession session,String input){Object expected=session.getAttribute(KEY);session.removeAttribute(KEY);return expected!=null&&input!=null&&expected.toString().equalsIgnoreCase(input.strip());}
    public String svg(String value){return """
      <svg xmlns="http://www.w3.org/2000/svg" width="128" height="44" viewBox="0 0 128 44"><rect width="128" height="44" rx="8" fill="#f1eee5"/><path d="M4 9L124 34M8 38L118 5" stroke="#2f5d50" stroke-opacity=".18"/><text x="64" y="30" text-anchor="middle" font-family="monospace" font-size="24" font-weight="700" letter-spacing="6" fill="#1f2a24">%s</text></svg>
      """.formatted(value);}
}
