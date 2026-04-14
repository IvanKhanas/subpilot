package com.xeno.subpilot.subscription.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "user_model_preference")
class UserModelPreference(
    @Id
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "model_id", nullable = false)
    var modelId: String,
)
