package ru.severnoyesiyanieBack.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.severnoyesiyanieBack.dto.ErrorResponse

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFoundException(exception: UserNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse(message = exception.message ?: "User not found" ))
    }

    @ExceptionHandler(UserAlreadyExistsException::class)
    fun handleUserAlreadyExistsException(exception: UserAlreadyExistsException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
        .body(ErrorResponse(message = exception.message ?: "User already exists"))
    }
    // валидация входных данных
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(exception: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage?:"Validation error"

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse(message = message))
    }
    @ExceptionHandler(MenuItemNotFoundException::class)
    fun handleMenuItemNotFound(
        ex: MenuItemNotFoundException
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(ex.message ?: "Cocktail not found"))
    }
}