package com.andrejKir.connect.shared.web;

import com.andrejKir.connect.accounts.exception.DuplicateEmailException;
import com.andrejKir.connect.accounts.exception.DuplicateUserException;
import com.andrejKir.connect.accounts.exception.DuplicateUsernameException;
import com.andrejKir.connect.accounts.exception.InvalidPasswordResetTokenException;
import com.andrejKir.connect.messaging.exception.ConversationNotFoundException;
import com.andrejKir.connect.messaging.exception.NotFriendsException;
import com.andrejKir.connect.messaging.exception.SelfConversationException;
import com.andrejKir.connect.shared.exception.RateLimitExceededException;
import com.andrejKir.connect.social.exception.AlreadyFriendsException;
import com.andrejKir.connect.social.exception.InvalidFriendshipDirectionException;
import com.andrejKir.connect.social.exception.FriendshipNotPendingException;
import com.andrejKir.connect.social.exception.FriendshipRequestAlreadyPendingException;
import com.andrejKir.connect.social.exception.FriendshipRequestLimitExceededException;
import com.andrejKir.connect.social.exception.FriendshipRequestNotFoundException;
import com.andrejKir.connect.social.exception.FriendshipRequestOnCooldownException;
import com.andrejKir.connect.social.exception.FriendshipTargetNotFoundException;
import com.andrejKir.connect.social.exception.OwnFriendshipRequestException;
import com.andrejKir.connect.social.exception.SelfFriendshipException;
import com.andrejKir.connect.shared.exception.LocalizedException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {
    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler({DuplicateUsernameException.class, DuplicateEmailException.class, DuplicateUserException.class})
    public ProblemDetail handleDuplicate(LocalizedException e){
        String message = resolve(e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException e){
        String message = messageSource.getMessage("error.invalid.credentials", null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, message);
    }


    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(RateLimitExceededException e){
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, resolve(e));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()))
                .body(problemDetail);
    }

    @ExceptionHandler({
            SelfFriendshipException.class,
            InvalidFriendshipDirectionException.class,
            SelfConversationException.class
    })
    public ProblemDetail handleBadRequest(LocalizedException e){
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, resolve(e));
    }

    @ExceptionHandler({
            FriendshipRequestAlreadyPendingException.class,
            AlreadyFriendsException.class,
            FriendshipRequestOnCooldownException.class,
            FriendshipNotPendingException.class,
            FriendshipRequestLimitExceededException.class
    })
    public ProblemDetail handleFriendshipConflict(LocalizedException e){
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, resolve(e));
    }

    @ExceptionHandler({
            FriendshipTargetNotFoundException.class,
            FriendshipRequestNotFoundException.class,
            ConversationNotFoundException.class
    })
    public ProblemDetail handleNotFound(LocalizedException e){
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, resolve(e));
    }

    @ExceptionHandler({
            OwnFriendshipRequestException.class,
            NotFriendsException.class
    })
    public ProblemDetail handleForbidden(LocalizedException e){
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, resolve(e));
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ProblemDetail handleInvalidPasswordResetToken(LocalizedException e){
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, resolve(e));
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
