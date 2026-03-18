package com.elvarg.plugin.test

import com.elvarg.plugin.event.impl.ServerBootEvent
import com.elvarg.plugin.event.impl.ServerStartedEvent
import com.elvarg.plugin.event.impl.ServerStoppedEvent

com.elvarg.plugin.event.EventListener.on<ServerStartedEvent> {
    then { println("PID: $pid") }
}

com.elvarg.plugin.event.EventListener.on<ServerStoppedEvent> {

    then { println("Server Stopped") }
}
