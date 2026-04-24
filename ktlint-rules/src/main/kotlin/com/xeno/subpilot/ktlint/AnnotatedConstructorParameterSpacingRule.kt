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
package com.xeno.subpilot.ktlint

import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COMMA
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SECONDARY_CONSTRUCTOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class AnnotatedConstructorParameterSpacingRule : Rule(
    ruleId = RuleId("subpilot:annotated-constructor-parameter-spacing"),
    about = Rule.About(maintainer = "subpilot"),
) {
    @Suppress("DEPRECATION")
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType != VALUE_PARAMETER) return

        val grandParentType = node.treeParent?.treeParent?.elementType
        if (grandParentType != PRIMARY_CONSTRUCTOR && grandParentType != SECONDARY_CONSTRUCTOR) return

        if (!node.hasAnnotations()) return

        val prevWhitespace = node.treePrev ?: return
        if (prevWhitespace.elementType != WHITE_SPACE) return

        val beforeWhitespace = prevWhitespace.treePrev ?: return
        if (beforeWhitespace.elementType != COMMA) return

        if (prevWhitespace.text.contains("\n\n")) return

        emit(node.startOffset, "Missing blank line before annotated constructor parameter", true)
        if (autoCorrect) {
            (prevWhitespace as LeafPsiElement).rawReplaceWithText(prevWhitespace.text.replaceFirst("\n", "\n\n"))
        }
    }
}

private fun ASTNode.hasAnnotations(): Boolean {
    val modifierList = firstChildNode ?: return false
    if (modifierList.elementType != MODIFIER_LIST) return false
    return modifierList.getChildren(null).any { it.elementType == ANNOTATION_ENTRY }
}
