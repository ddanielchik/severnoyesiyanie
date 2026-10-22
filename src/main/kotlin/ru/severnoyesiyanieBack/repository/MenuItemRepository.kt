package ru.severnoyesiyanieBack.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import ru.severnoyesiyanieBack.entity.MenuItem

interface MenuItemRepository : JpaRepository<MenuItem, Int> {

    fun findAllBy(pageable: Pageable): Slice<MenuItem>
}