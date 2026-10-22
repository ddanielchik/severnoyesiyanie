package ru.severnoyesiyanieBack.controller

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.severnoyesiyanieBack.dto.MenuItemResponse
import ru.severnoyesiyanieBack.service.MenuItemService
import org.springframework.data.domain.Slice
import org.springframework.web.bind.annotation.RequestParam
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import ru.severnoyesiyanieBack.dto.CreateMenuItemRequest
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.DeleteMapping


@RestController
@RequestMapping("/api/menu")
class MenuItemController (private val menuItemService: MenuItemService){

    @GetMapping("/{idPosition}")
    fun getMenuItemById(
        @PathVariable idPosition: Int): MenuItemResponse{
        return menuItemService.getMenuItemById(idPosition)
    }
    @GetMapping
    fun getAllMenuItems(
        @RequestParam(defaultValue = "0") page: Int
    ): Slice<MenuItemResponse> {
        return menuItemService.getAllMenuItems(page)
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createMenuItem(
        @Valid @RequestBody request: CreateMenuItemRequest
    ): MenuItemResponse {
        return menuItemService.createMenuItem(request)
    }
    @PutMapping("/{idPosition}")
    fun updateMenuItem(
        @PathVariable idPosition: Int,
        @Valid @RequestBody request: CreateMenuItemRequest
    ): MenuItemResponse {
        return menuItemService.updateMenuItem(idPosition, request)
    }
    @DeleteMapping("/{idPosition}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMenuItem(@PathVariable idPosition: Int) {
        menuItemService.deleteMenuItem(idPosition)
    }
}