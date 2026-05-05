/*
 * Copyright 2026 Ivan Khanas
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
package com.xeno.subpilot.admin.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

import java.math.BigDecimal

data class CreatePlanRequest(
    @field:NotBlank
    val planId: String,

    @field:NotBlank
    val provider: String,

    @field:NotBlank
    val displayName: String,

    @field:NotNull
    @field:DecimalMin("0.01")
    val price: BigDecimal,

    @field:NotBlank
    val currency: String,

    @field:NotEmpty
    @field:Valid
    val allocations: List<AllocationRequest>,
)
