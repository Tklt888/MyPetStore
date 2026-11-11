package org.csu.mypetstore.admin.controller;

import org.csu.mypetstore.service.ProductConflictException;
import org.csu.mypetstore.service.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ProductExceptionHandler {
    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(Model model) {
        model.addAttribute("message", "商品不存在或已被删除");
        return "error/404";
    }

    @ExceptionHandler(ProductConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict(ProductConflictException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error/400";
    }
}
