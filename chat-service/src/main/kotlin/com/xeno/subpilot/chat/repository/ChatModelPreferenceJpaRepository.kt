package com.xeno.subpilot.chat.repository

import com.xeno.subpilot.chat.entity.ChatModelPreference
import org.springframework.data.jpa.repository.JpaRepository

interface ChatModelPreferenceJpaRepository : JpaRepository<ChatModelPreference, Long>
