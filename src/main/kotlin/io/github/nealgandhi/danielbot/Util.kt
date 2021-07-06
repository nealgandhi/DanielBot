package io.github.nealgandhi.danielbot

import dev.kord.common.entity.ChannelType

inline val ChannelType.isDm: Boolean get() = this == ChannelType.DM || this == ChannelType.GroupDM

