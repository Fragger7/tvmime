package com.tvmime.tv.sv_ui.theme

import com.tvmime.tv.sv_ui.design.AppSpacing
import com.tvmime.tv.sv_ui.design.LocalAppSpacing

typealias Spacing = AppSpacing

val LocalSpacing = LocalAppSpacing

fun defaultSpacing(): Spacing = AppSpacing()
