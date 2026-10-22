package ru.severnoyesiyanieBack.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import ru.severnoyesiyanieBack.dto.CreateOrderItemRequest
import ru.severnoyesiyanieBack.dto.CreateOrderRequest
import ru.severnoyesiyanieBack.entity.MenuItem
import ru.severnoyesiyanieBack.entity.Order
import ru.severnoyesiyanieBack.entity.OrderItem
import ru.severnoyesiyanieBack.entity.User
import ru.severnoyesiyanieBack.entity.UserRole
import ru.severnoyesiyanieBack.entity.Visit
import ru.severnoyesiyanieBack.entity.VisitStatus
import ru.severnoyesiyanieBack.exception.InvalidUserRoleException
import ru.severnoyesiyanieBack.exception.MenuItemNotFoundException
import ru.severnoyesiyanieBack.exception.OrderNotFoundException
import ru.severnoyesiyanieBack.exception.UserNotFoundException
import ru.severnoyesiyanieBack.exception.VisitAlreadyClosedException
import ru.severnoyesiyanieBack.exception.VisitNotFoundException
import ru.severnoyesiyanieBack.repository.MenuItemRepository
import ru.severnoyesiyanieBack.repository.OrderRepository
import ru.severnoyesiyanieBack.repository.UserRepository
import ru.severnoyesiyanieBack.repository.VisitRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class OrderServiceTest {
    private lateinit var orderRepository: OrderRepository
    private lateinit var visitRepository: VisitRepository
    private lateinit var userRepository: UserRepository
    private lateinit var menuItemRepository: MenuItemRepository
    private lateinit var service: OrderService
    private var savedOrder: Order? = null

    private val client = User(1, "Client", "client@example.com", UserRole.CLIENT)
    private val waiter = User(2, "Waiter", "waiter@example.com", UserRole.WAITER)
    private val worker = User(3, "Worker", "worker@example.com", UserRole.WORKER)
    private val visit = Visit(idVisit = 4, user = client, tableNumber = 6)
    private val cocktail = MenuItem(idPosition = 10, name = "Aurora", price = BigDecimal("125.50"))
    private val lemonade = MenuItem(idPosition = 11, name = "Lemonade", price = BigDecimal("45.25"))
    private val request = CreateOrderRequest(
        visitId = 4,
        waiterId = 2,
        workerId = 3,
        items = listOf(CreateOrderItemRequest(10, 2), CreateOrderItemRequest(11, 3))
    )

    @BeforeEach
    fun setUp() {
        savedOrder = null
        orderRepository = mock(OrderRepository::class.java) { invocation ->
            if (invocation.method.name == "save") {
                invocation.getArgument<Order>(0).also {
                    it.idOrder = 20
                    savedOrder = it
                }
            } else {
                Answers.RETURNS_DEFAULTS.answer(invocation)
            }
        }
        visitRepository = mock(VisitRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        menuItemRepository = mock(MenuItemRepository::class.java)
        service = OrderService(orderRepository, visitRepository, userRepository, menuItemRepository)
    }

    @Test
    fun `creating an order calculates exact totals and connects every item to its order`() {
        stubValidParticipants()
        `when`(menuItemRepository.findById(10)).thenReturn(Optional.of(cocktail))
        `when`(menuItemRepository.findById(11)).thenReturn(Optional.of(lemonade))

        val response = service.createOrder(request)
        val persisted = requireNotNull(savedOrder)

        assertEquals(20, response.idOrder)
        assertEquals(4, response.visitId)
        assertEquals(2, response.waiterId)
        assertEquals(3, response.workerId)
        assertEquals(BigDecimal("386.75"), response.totalAmount)
        assertEquals(listOf(10, 11), response.items.map { it.menuItemId })
        assertEquals(listOf("Aurora", "Lemonade"), response.items.map { it.name })
        assertEquals(listOf(2, 3), response.items.map { it.quantity })
        assertEquals(listOf(BigDecimal("125.50"), BigDecimal("45.25")), response.items.map { it.unitPrice })
        assertEquals(listOf(BigDecimal("251.00"), BigDecimal("135.75")), response.items.map { it.subtotal })
        assertSame(visit, persisted.visit)
        assertSame(waiter, persisted.waiter)
        assertSame(worker, persisted.worker)
        assertSame(cocktail, persisted.items[0].product)
        assertSame(lemonade, persisted.items[1].product)
        persisted.items.forEach { assertSame(persisted, it.order) }
        assertEquals(response.createdAt, persisted.createdAt)
        assertEquals(response.totalAmount, persisted.totalAmount)
        verify(visitRepository).findByIdForUpdate(4)
        verify(orderRepository).save(persisted)
    }

    @Test
    fun `existing order keeps original prices after menu price changes`() {
        val createdAt = LocalDateTime.of(2025, 4, 7, 20, 30)
        val order = Order(20, visit, waiter, worker, BigDecimal("251.00"), createdAt)
        order.items.add(OrderItem(order = order, product = cocktail, unitPrice = BigDecimal("125.50"), quantity = 2))
        cocktail.price = BigDecimal("999.00")
        `when`(orderRepository.findById(20)).thenReturn(Optional.of(order))

        val response = service.getOrderById(20)

        assertEquals(BigDecimal("251.00"), response.totalAmount)
        assertEquals(BigDecimal("125.50"), response.items.single().unitPrice)
        assertEquals(BigDecimal("251.00"), response.items.single().subtotal)
        assertEquals(createdAt, response.createdAt)
        verifyNoInteractions(visitRepository, userRepository, menuItemRepository)
    }

    @Test
    fun `order response permits missing staff references`() {
        val order = Order(idOrder = 20, visit = visit, totalAmount = BigDecimal.ZERO)
        `when`(orderRepository.findById(20)).thenReturn(Optional.of(order))

        val response = service.getOrderById(20)

        assertNull(response.waiterId)
        assertNull(response.workerId)
        assertEquals(emptyList<Any>(), response.items)
    }

    @Test
    fun `missing order is reported`() {
        `when`(orderRepository.findById(99)).thenReturn(Optional.empty())

        assertThrows(OrderNotFoundException::class.java) { service.getOrderById(99) }
    }

    @Test
    fun `missing visit prevents creation before staff and menu are read`() {
        `when`(visitRepository.findByIdForUpdate(4)).thenReturn(null)

        assertThrows(VisitNotFoundException::class.java) { service.createOrder(request) }

        verifyNoInteractions(userRepository, menuItemRepository, orderRepository)
    }

    @Test
    fun `closed visit rejects a new order`() {
        visit.status = VisitStatus.CLOSED
        `when`(visitRepository.findByIdForUpdate(4)).thenReturn(visit)

        assertThrows(VisitAlreadyClosedException::class.java) { service.createOrder(request) }

        verifyNoInteractions(userRepository, menuItemRepository, orderRepository)
    }

    @Test
    fun `missing waiter prevents order persistence`() {
        `when`(visitRepository.findByIdForUpdate(4)).thenReturn(visit)
        `when`(userRepository.findById(2)).thenReturn(Optional.empty())

        assertThrows(UserNotFoundException::class.java) { service.createOrder(request) }

        verifyNoInteractions(menuItemRepository, orderRepository)
    }

    @Test
    fun `missing worker prevents order persistence`() {
        `when`(visitRepository.findByIdForUpdate(4)).thenReturn(visit)
        `when`(userRepository.findById(2)).thenReturn(Optional.of(waiter))
        `when`(userRepository.findById(3)).thenReturn(Optional.empty())

        assertThrows(UserNotFoundException::class.java) { service.createOrder(request) }

        verifyNoInteractions(menuItemRepository, orderRepository)
    }

    @Test
    fun `a client cannot act as waiter`() {
        stubValidParticipants()
        waiter.role = UserRole.CLIENT

        assertThrows(InvalidUserRoleException::class.java) { service.createOrder(request) }

        verifyNoInteractions(menuItemRepository, orderRepository)
    }

    @Test
    fun `a waiter cannot act as preparation worker`() {
        stubValidParticipants()
        worker.role = UserRole.WAITER

        assertThrows(InvalidUserRoleException::class.java) { service.createOrder(request) }

        verifyNoInteractions(menuItemRepository, orderRepository)
    }

    @Test
    fun `missing menu item prevents saving a partial order`() {
        stubValidParticipants()
        `when`(menuItemRepository.findById(10)).thenReturn(Optional.of(cocktail))
        `when`(menuItemRepository.findById(11)).thenReturn(Optional.empty())

        assertThrows(MenuItemNotFoundException::class.java) { service.createOrder(request) }

        verifyNoInteractions(orderRepository)
    }

    private fun stubValidParticipants() {
        `when`(visitRepository.findByIdForUpdate(4)).thenReturn(visit)
        `when`(userRepository.findById(2)).thenReturn(Optional.of(waiter))
        `when`(userRepository.findById(3)).thenReturn(Optional.of(worker))
    }
}
