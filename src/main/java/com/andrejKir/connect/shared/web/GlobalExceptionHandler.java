package com.andrejKir.connect.shared.web;

import com.andrejKir.connect.accounts.exception.DuplicateEmailException;
import com.andrejKir.connect.accounts.exception.DuplicateUsernameException;
import com.andrejKir.connect.accounts.exception.InvalidCredentialsException;
import com.andrejKir.connect.social.exception.FriendshipNotPendingException;
import com.andrejKir.connect.social.exception.SelfFriendshipException;
import com.andrejKir.connect.shared.exception.LocalizedException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler({DuplicateUsernameException.class, DuplicateEmailException.class})
    public ProblemDetail handleDuplicate(LocalizedException e){
        String message = resolve(e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(LocalizedException e){
        String message = resolve(e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, message);
    }

    @ExceptionHandler(SelfFriendshipException.class)
    public ProblemDetail handleSelfFriendship(LocalizedException e){
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, resolve(e));
    }

    @ExceptionHandler(FriendshipNotPendingException.class)
    public ProblemDetail handleFriendshipNotPending(LocalizedException e){
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, resolve(e));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e){
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }


    private String resolve(LocalizedException e) {
        return messageSource.getMessage(e.getMessageKey(), e.getArgs(), LocaleContextHolder.getLocale());
    }


}
