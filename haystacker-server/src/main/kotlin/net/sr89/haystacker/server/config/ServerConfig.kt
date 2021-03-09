package net.sr89.haystacker.server.config

import java.nio.file.Path

data class ServerConfig(val httpPort: Int, val settingsDirectory: Path)