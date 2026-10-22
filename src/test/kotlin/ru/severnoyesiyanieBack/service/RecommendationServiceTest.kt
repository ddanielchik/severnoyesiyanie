package ru.severnoyesiyanieBack.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import ru.severnoyesiyanieBack.dto.MenuItemResponse
import ru.severnoyesiyanieBack.entity.MenuItem
import ru.severnoyesiyanieBack.entity.TasteTag
import ru.severnoyesiyanieBack.entity.User
import ru.severnoyesiyanieBack.entity.Visit
import ru.severnoyesiyanieBack.entity.VisitStatus
import ru.severnoyesiyanieBack.exception.VisitAlreadyClosedException
import ru.severnoyesiyanieBack.exception.VisitNotFoundException
import ru.severnoyesiyanieBack.repository.MenuItemRepository
import ru.severnoyesiyanieBack.repository.VisitRepository
import java.math.BigDecimal
import java.util.Optional

class RecommendationServiceTest {
    private val visitRepository = mock(VisitRepository::class.java)
    private val menuItemRepository = mock(MenuItemRepository::class.java)
    private val service = RecommendationService(visitRepository, menuItemRepository)

    @Test
    fun `missing visit is rejected without reading the menu`() {
        doReturn(Optional.empty<Visit>()).`when`(visitRepository).findById(404)

        val exception = assertThrows(VisitNotFoundException::class.java) {
            service.getRecommendations(404)
        }

        assertTrue(exception.message.orEmpty().contains("404"))
        verifyNoInteractions(menuItemRepository)
    }

    @Test
    fun `closed visit is rejected even when preferences are empty`() {
        val visit = visit().apply { status = VisitStatus.CLOSED }
        doReturn(Optional.of(visit)).`when`(visitRepository).findById(7)

        assertThrows(VisitAlreadyClosedException::class.java) {
            service.getRecommendations(7)
        }

        verifyNoInteractions(menuItemRepository)
    }

    @Test
    fun `open visit without preferences has no recommendations and skips menu lookup`() {
        doReturn(Optional.of(visit())).`when`(visitRepository).findById(7)

        assertTrue(service.getRecommendations(7).isEmpty())

        verifyNoInteractions(menuItemRepository)
    }

    @Test
    fun `recommendations query preferred tastes with a fifty item limit and map returned cocktails`() {
        val visit = visit(TasteTag(1, "Sour"), TasteTag(2, "Sweet"))
        val sourAndSweet = cocktail(1, TasteTag(1, "Sour"), TasteTag(2, "Sweet"))
        val sweet = cocktail(3, TasteTag(2, "Sweet"))
        val tastes = setOf("Sour", "Sweet")
        val pageable = PageRequest.of(0, 50)
        doReturn(Optional.of(visit)).`when`(visitRepository).findById(7)
        doReturn(SliceImpl(listOf(sourAndSweet, sweet), pageable, true))
            .`when`(menuItemRepository).findByTasteTags(tastes, pageable)

        val result = service.getRecommendations(7)

        assertEquals(
            listOf(
                MenuItemResponse(1, "Cocktail 1", BigDecimal("450.50"), "Gin, lime", listOf("Sour", "Sweet")),
                MenuItemResponse(3, "Cocktail 3", BigDecimal("450.50"), "Gin, lime", listOf("Sweet"))
            ),
            result
        )
        verify(menuItemRepository).findByTasteTags(tastes, pageable)
        verifyNoMoreInteractions(menuItemRepository)
    }

    @Test
    fun `an empty query result produces no recommendations`() {
        val tastes = setOf("Sour")
        val pageable = PageRequest.of(0, 50)
        doReturn(Optional.of(visit(TasteTag(1, "Sour")))).`when`(visitRepository).findById(7)
        doReturn(SliceImpl<MenuItem>(emptyList(), pageable, false))
            .`when`(menuItemRepository).findByTasteTags(tastes, pageable)

        assertTrue(service.getRecommendations(7).isEmpty())

        verify(menuItemRepository).findByTasteTags(tastes, pageable)
        verifyNoMoreInteractions(menuItemRepository)
    }

    private fun visit(vararg tastes: TasteTag) = Visit(
        idVisit = 7,
        user = User(idUser = 1, name = "Anna", email = "anna@example.com"),
        tastePreferences = tastes.toMutableSet()
    )

    private fun cocktail(id: Int, vararg tastes: TasteTag) = MenuItem(
        idPosition = id,
        name = "Cocktail $id",
        price = BigDecimal("450.50"),
        ingredients = "Gin, lime",
        tasteTags = tastes.toMutableSet()
    )
}
