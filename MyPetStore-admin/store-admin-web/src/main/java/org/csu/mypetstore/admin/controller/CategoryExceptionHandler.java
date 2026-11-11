package org.csu.mypetstore.admin.controller;

import org.csu.mypetstore.service.CategoryNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class CategoryExceptionHandler {

    @ExceptionHandler(CategoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String categoryNotFound(CategoryNotFoundException exception, Model model) {
        model.addAttribute("message", "分类不存在或已被删除");
        return "error/404";
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalidParameter(Exception exception, Model model) {
        model.addAttribute("message", "请求参数格式不正确");
        return "error/400";
    }
}
