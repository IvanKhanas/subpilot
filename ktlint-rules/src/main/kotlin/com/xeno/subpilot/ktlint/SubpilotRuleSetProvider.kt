package com.xeno.subpilot.ktlint

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

class SubpilotRuleSetProvider : RuleSetProviderV3(
    id = RuleSetId("subpilot"),
) {
    override fun getRuleProviders(): Set<RuleProvider> = setOf(
        RuleProvider { AnnotatedConstructorParameterSpacingRule() },
    )
}
