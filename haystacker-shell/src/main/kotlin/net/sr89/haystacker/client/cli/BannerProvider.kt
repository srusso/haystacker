package net.sr89.haystacker.client.cli

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.shell.plugin.support.DefaultBannerProvider
import org.springframework.stereotype.Component

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class BannerProvider : DefaultBannerProvider() {
    override fun getBanner(): String {
        return ""
    }

    override fun getVersion(): String {
        return "0.1"
    }

    override fun getWelcomeMessage(): String {
        return "Welcome to Haystacker"
    }

    override fun getProviderName(): String {
        return "Haystacker Banner"
    }
}