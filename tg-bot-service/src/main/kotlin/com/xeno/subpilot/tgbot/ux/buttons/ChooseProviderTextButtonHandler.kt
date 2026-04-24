/*
 * Copyright 2024 Ivan Khanas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import com.xeno.subpilot.tgbot.ux.ScreenRenderer
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(2)
class ChooseProviderTextButtonHandler(
    private val navigationService: NavigationService,
    private val screenRenderer: ScreenRenderer,
) : TextButtonHandler {

    override fun supports(text: String) = text == BotButtons.BTN_CHOOSE_MODEL

    override suspend fun handle(message: Message) {
        navigationService.push(message.chat.id, BotScreen.MAIN_MENU)
        screenRenderer.render(message.chat.id, BotScreen.PROVIDER_MENU)
    }
}
