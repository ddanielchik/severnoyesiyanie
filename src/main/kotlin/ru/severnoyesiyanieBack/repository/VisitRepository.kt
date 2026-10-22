package ru.severnoyesiyanieBack.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.severnoyesiyanieBack.entity.Visit
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface VisitRepository : JpaRepository<Visit, Int>{
    //блокировка на изиененение
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Visit v WHERE v.idVisit = :id")
    fun findByIdForUpdate(@Param("id") id: Int): Visit?
}