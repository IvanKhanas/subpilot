package com.xeno.subpilot.loyalty.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "user_loyalty_balance")
class UserLoyaltyBalance(
    @Id
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "points", nullable = false)
    var points: Long,
)
