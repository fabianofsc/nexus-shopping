package com.nexus.shopping.notification

import com.nexus.shopping.notification.application.exception.NotificationValidationException
import com.nexus.shopping.notification.application.port.outbound.NotificationRepositoryPort
import com.nexus.shopping.notification.application.usecase.ListNotificationsByCustomerUseCase
import com.nexus.shopping.notification.domain.Notification
import com.nexus.shopping.notification.domain.NotificationStatus
import com.nexus.shopping.platform.domain.PageResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class FakeNotificationRepositoryForList : NotificationRepositoryPort {
    var lastCustomerId: Long? = null
    var lastPage: Int? = null
    var lastSize: Int? = null

    override fun save(notification: Notification): Notification = throw UnsupportedOperationException()

    override fun reserve(notification: Notification): Notification = throw UnsupportedOperationException()

    override fun claim(
        notificationKey: String,
        sendingLeaseToken: String,
        sendingLeaseUntil: Instant,
        now: Instant,
    ): Notification? = throw UnsupportedOperationException()

    override fun complete(
        notificationKey: String,
        sendingLeaseToken: String,
        status: NotificationStatus,
        sentAt: Instant?,
    ): Notification? = throw UnsupportedOperationException()

    override fun findByNotificationKey(notificationKey: String): Notification? = throw UnsupportedOperationException()

    override fun findById(id: Long): Notification? = throw UnsupportedOperationException()

    override fun findByCustomerId(
        customerId: Long,
        page: Int,
        size: Int,
    ): PageResult<Notification> {
        lastCustomerId = customerId
        lastPage = page
        lastSize = size
        return PageResult(content = emptyList(), page = page, size = size, count = 0, hasNext = false)
    }
}

class ListNotificationsByCustomerUseCaseTest {
    @Test
    fun `delegates to repository with given customerId, page and size`() {
        val repository = FakeNotificationRepositoryForList()
        val useCase = ListNotificationsByCustomerUseCase(repository)

        val result = useCase.list(customerId = 1L, page = 0, size = 50)

        assertEquals(1L, repository.lastCustomerId)
        assertEquals(0, repository.lastPage)
        assertEquals(50, repository.lastSize)
        assertEquals(0, result.count)
    }

    @Test
    fun `throws NotificationValidationException when customerId is missing`() {
        val useCase = ListNotificationsByCustomerUseCase(FakeNotificationRepositoryForList())

        assertFailsWith<NotificationValidationException> {
            useCase.list(customerId = null, page = 0, size = 50)
        }
    }

    @Test
    fun `throws NotificationValidationException when customerId is zero or negative`() {
        val useCase = ListNotificationsByCustomerUseCase(FakeNotificationRepositoryForList())

        assertFailsWith<NotificationValidationException> {
            useCase.list(customerId = 0L, page = 0, size = 50)
        }
        assertFailsWith<NotificationValidationException> {
            useCase.list(customerId = -1L, page = 0, size = 50)
        }
    }

    @Test
    fun `throws NotificationValidationException when page is negative`() {
        val useCase = ListNotificationsByCustomerUseCase(FakeNotificationRepositoryForList())

        assertFailsWith<NotificationValidationException> {
            useCase.list(customerId = 1L, page = -1, size = 50)
        }
    }

    @Test
    fun `throws NotificationValidationException when size is out of range`() {
        val useCase = ListNotificationsByCustomerUseCase(FakeNotificationRepositoryForList())

        assertFailsWith<NotificationValidationException> {
            useCase.list(customerId = 1L, page = 0, size = 501)
        }
    }
}
