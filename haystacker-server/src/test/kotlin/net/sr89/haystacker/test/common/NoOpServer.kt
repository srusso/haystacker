package net.sr89.haystacker.test.common

import org.http4k.server.Http4kServer

class NoOpServer: Http4kServer {
    override fun port(): Int {
        return 0
    }

    override fun start(): Http4kServer {
        return this
    }

    override fun stop(): Http4kServer {
        return this
    }
}