package io.github.nealgandhi.danielbot

import com.kotlindiscord.kord.extensions.commands.converters.impl.boolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import dev.kord.rest.builder.interaction.embed

@OptIn(KordPreview::class)
class TestExtension : Extension() {
    override val name = "test"

    class TestArgs : Arguments() {
        val string by string("string", "String argument")

        val bool by boolean("bool", "Boolean argument")
    }

    override suspend fun setup() {
        slashCommand(TestExtension::TestArgs) {
            name = "test"
            description = "Test command, please ignore"
            autoAck = AutoAckType.PUBLIC

            action {
                publicFollowUp {
                    embed {
                        title = "Test response"
                        description = "Test description"

                        field {
                            name = "String"
                            value = arguments.string
                        }

                        field {
                            name = "Bool"
                            value = arguments.bool.toString()
                        }
                    }
                }
            }
        }
    }
}
