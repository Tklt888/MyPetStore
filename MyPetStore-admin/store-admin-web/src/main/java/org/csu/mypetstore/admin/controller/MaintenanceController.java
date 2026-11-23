package org.csu.mypetstore.admin.controller;

import java.nio.charset.StandardCharsets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class MaintenanceController {
    private final JdbcTemplate database;
    private final PasswordEncoder passwords;

    public MaintenanceController(JdbcTemplate database, PasswordEncoder passwords) {
        this.database = database;
        this.passwords = passwords;
    }

    @PostMapping("/products/{id}/archive")
    public String archiveProduct(@PathVariable long id, @RequestParam long version, RedirectAttributes flash) {
        int changed = database.update("""
                UPDATE product SET archived_at = CURRENT_TIMESTAMP, status = 'DRAFT', version = version + 1
                WHERE id = ? AND version = ? AND archived_at IS NULL
                """, id, version);
        result(flash, changed, "商品已删除");
        return "redirect:/admin/products";
    }

    @PostMapping("/orders/{id}/archive")
    public String archiveOrder(@PathVariable long id, RedirectAttributes flash) {
        int changed = database.update("""
                UPDATE sales_order SET archived_at = CURRENT_TIMESTAMP
                WHERE id = ? AND status = 'CANCELLED' AND archived_at IS NULL
                """, id);
        result(flash, changed, "订单已删除");
        return "redirect:/admin/orders";
    }

    @PostMapping("/orders/{id}/shipping-address")
    public String editAddress(@PathVariable long id, @RequestParam String receiverName,
            @RequestParam String receiverPhone, @RequestParam String receiverAddress, RedirectAttributes flash) {
        if (valid(receiverName, 50) && valid(receiverPhone, 30) && valid(receiverAddress, 255)) {
            int changed = database.update("""
                    UPDATE sales_order SET receiver_name = ?, receiver_phone = ?, receiver_address = ?
                    WHERE id = ? AND archived_at IS NULL AND status IN ('PENDING_PAYMENT', 'PAID')
                    """, receiverName.strip(), receiverPhone.strip(), receiverAddress.strip(), id);
            result(flash, changed, "收货信息已保存");
        } else {
            flash.addFlashAttribute("error", "请填写有效的收货姓名、电话和地址");
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/customers/{id}/profile")
    public String editCustomer(@PathVariable long id, @RequestParam String displayName,
            @RequestParam String email, @RequestParam String phone, RedirectAttributes flash) {
        if (valid(displayName, 50) && email.strip().length() <= 100 && phone.strip().length() <= 30
                && (email.isBlank() || email.strip().matches("[^\\s@]+@[^\\s@]+"))) {
            int changed = database.update("""
                    UPDATE store_user SET display_name = ?, email = ?, phone = ?
                    WHERE id = ? AND role = 'CUSTOMER'
                    """, displayName.strip(), optional(email), optional(phone), id);
            result(flash, changed, "客户资料已保存");
        } else {
            flash.addFlashAttribute("error", "请检查显示名、邮箱和手机号");
        }
        return "redirect:/admin/customers";
    }

    @PostMapping("/customers/{id}/reset-password")
    public String resetPassword(@PathVariable long id, @RequestParam String password, RedirectAttributes flash) {
        if (password.length() >= 8 && password.getBytes(StandardCharsets.UTF_8).length <= 72) {
            int changed = database.update("UPDATE store_user SET password_hash = ? WHERE id = ? AND role = 'CUSTOMER'",
                    passwords.encode(password), id);
            result(flash, changed, "客户密码已重置");
        } else {
            flash.addFlashAttribute("error", "密码须至少 8 个字符且不超过 72 字节");
        }
        return "redirect:/admin/customers";
    }

    private static boolean valid(String value, int max) {
        return !value.isBlank() && value.strip().length() <= max;
    }

    private static String optional(String value) {
        return value.isBlank() ? null : value.strip();
    }

    private static void result(RedirectAttributes flash, int changed, String message) {
        if (changed == 1) {
            flash.addFlashAttribute("message", message);
        } else {
            flash.addFlashAttribute("error", "记录不存在、状态已变更或当前操作不可用，请刷新后重试");
        }
    }
}
