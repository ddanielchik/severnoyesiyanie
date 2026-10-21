package ru.severnoyesiyanieBack.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.severnoyesiyanieBack.dto.CreateUserRequest
import ru.severnoyesiyanieBack.dto.UpdateUserRequest
import ru.severnoyesiyanieBack.dto.UserResponse
import ru.severnoyesiyanieBack.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ResponseStatus


@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(
        @Valid @RequestBody createUserRequest: CreateUserRequest
    ): UserResponse {
        return userService.createUser(createUserRequest)
    }

    @GetMapping("/{userId}")
    fun getUserById(@PathVariable userId: Int): UserResponse {
        return userService.getUserById(userId)

    }
    @PutMapping("/{userId}")
        fun updateUser(@PathVariable userId: Int, @Valid @RequestBody updateUserRequest: UpdateUserRequest): UserResponse {
            return userService.updateUser(userId, updateUserRequest)
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUserById(@PathVariable userId: Int) {
        userService.deleteUser(userId)
    }


}