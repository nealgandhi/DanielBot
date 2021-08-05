package io.github.nealgandhi.danielbot.util

import com.kotlindiscord.kord.extensions.utils.env

fun requireEnv(name: String) = env(name) ?: error("You must provide the $name environment variable.")
