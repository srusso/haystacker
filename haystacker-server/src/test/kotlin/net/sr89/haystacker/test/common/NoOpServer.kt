package net.sr89.haystacker.test.common

import org.http4k.server.Http4kServer

class NoOpServer: Http4kServer {
    override fun port(): Int {
        TODO("Not yet implemented")
    }

    override fun start(): Http4kServer {
        TODO("Not yet implemented")
    }

    override fun stop(): Http4kServer {
        TODO("Not yet implemented")
    }
}