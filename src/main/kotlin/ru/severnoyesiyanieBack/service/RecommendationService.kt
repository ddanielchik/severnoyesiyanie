package ru.severnoyesiyanieBack.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.severnoyesiyanieBack.dto.MenuItemResponse
import ru.severnoyesiyanieBack.entity.VisitStatus
import ru.severnoyesiyanieBack.exception.VisitAlreadyClosedException
import ru.severnoyesiyanieBack.exception.VisitNotFoundException
import ru.severnoyesiyanieBack.repository.MenuItemRepository
import ru.severnoyesiyanieBack.repository.VisitRepository

@Service
class RecommendationService(
    private val visitRepository: VisitRepository,
    private val menuItemRepository: MenuItemRepository
) {

    @Transactional(readOnly = true)
    fun getRecommendations(visitId: Int): List<MenuItemResponse> {

        val visit = visitRepository.findById(visitId)
            .orElseThrow { VisitNotFoundException(visitId) }

        if (visit.status != VisitStatus.OPEN) {
            throw VisitAlreadyClosedException(visitId)
        }

        val preferredTastes = visit.tastePreferences
            .map { it.name }
            .toSet()

        if (preferredTastes.isEmpty()) {
            return emptyList()
        }

        return menuItemRepository.findByTasteTags(
            preferredTastes,
            PageRequest.of(0, 50)
        ).content.map { cocktail ->
            MenuItemResponse(
                idPosition = requireNotNull(cocktail.idPosition),
                name = cocktail.name,
                price = cocktail.price,
                ingredients = requireNotNull(cocktail.ingredients),
                tasteTags = cocktail.tasteTags.map { it.name }
            )
        }
    }
}