package net.sr89.haystacker.client.cli

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.shell.plugin.support.DefaultPromptProvider
import org.springframework.stereotype.Component

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class PromptProvider : DefaultPromptProvider() {
    override fun getPrompt(): String {
        return "haystacker> "
    }

    override fun getProviderName(): String {
        return "Haystacker Prompt"
    }
}