package com.xeno.subpilot.chat.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "chat_model_preference")
class ChatModelPreference(
    @Id
    val chatId: Long,
    @Column(name = "model")
    var model: String,
)
