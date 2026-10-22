package ru.severnoyesiyanieBack.service

import org.springframework.stereotype.Service
import ru.severnoyesiyanieBack.repository.VisitRepository
import ru.severnoyesiyanieBack.repository.MenuItemRepository
import org.springframework.transaction.annotation.Transactional
import ru.severnoyesiyanieBack.entity.VisitStatus
import ru.severnoyesiyanieBack.exception.VisitNotFoundException
import ru.severnoyesiyanieBack.exception.VisitAlreadyClosedException
import ru.severnoyesiyanieBack.dto.MenuItemResponse

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

        return menuItemRepository.findAll().filter { cocktail ->
            cocktail.tasteTags.any { tag ->
                tag.name in preferredTastes
            }
        }
            .take(50)
            .map { cocktail ->
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
