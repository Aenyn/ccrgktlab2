package fr.panicot.ccrg.core.messaging

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.servlet.http.HttpServletRequest

/**
 * Created by William on 11/02/2017.
 */
@RestController
class MessageController {
    val SYSTEM_ANNOUNCEMENT = "System announcement"
    val counter = AtomicLong()
    val messages = ArrayList<Message>()
    val users = HashMap<String, User>()


    @RequestMapping("/messages/longPoll", method = arrayOf(RequestMethod.GET))
    fun longPollMessages(@RequestParam(value = "latestId", defaultValue = "0") id: Long): MessageBatch {
        var messagesSinceId = getMessagesSinceId(id)
        while (messagesSinceId.messages.isEmpty()) {
            Thread.sleep(100)
            messagesSinceId = getMessagesSinceId(id)
        }
        return messagesSinceId
    }

    @RequestMapping("/messages/get", method = arrayOf(RequestMethod.GET))
    fun getMessages(@RequestParam(value = "latestId", defaultValue = "0") id: Long): MessageBatch {
        return getMessagesSinceId(id)
    }

    @RequestMapping("/messages/send", method = arrayOf(RequestMethod.POST))
    fun sendMessage(@RequestParam("author") author: String, @RequestParam("content") content: String): Unit {
        val timestamp = LocalDateTime.now()
        messages.add(Message(counter.incrementAndGet(), timestamp.toLocalTime().toString(), author, content))
    }

    @RequestMapping("/users/get", method = arrayOf(RequestMethod.GET))
    fun getUsers(): List<User> {
        return getActiveUsers()
    }

    @RequestMapping("/users/me", method = arrayOf(RequestMethod.GET))
    fun getMe(request: HttpServletRequest): User {
        val username = request.remoteUser
        if (users.containsKey(username)) {
            return users[username]?:User(username, false, LocalDateTime.now())
        } else {
            val user = User(username, false, LocalDateTime.now())
            users.put(username, user)
            return user
        }
    }

    @RequestMapping("/users/announce", method = arrayOf(RequestMethod.POST))
    fun announceArrival(@RequestParam("user") user: String, @RequestParam("isArrival") isArrival: Boolean): Unit {
        val requestTime = LocalDateTime.now()
        updateUserLastSeen(user, requestTime)
        val announcement = user + if(isArrival) " vient de se connecter" else " vient de ragequit"
        messages.add(Message(counter.incrementAndGet(), requestTime.toLocalTime().toString(), SYSTEM_ANNOUNCEMENT, announcement))
    }

    @RequestMapping("/users/keepalive", method = arrayOf(RequestMethod.GET))
    fun keepAlive(@RequestParam("user") user: String): Unit {
        val requestTime = LocalDateTime.now()
        updateUserLastSeen(user, requestTime)
    }

    fun getMessagesSinceId(id: Long): MessageBatch {
        return MessageBatch(messages.filter { message -> message.id > id }, counter.get())
    }

    fun getActiveUsers(): List<User> {
        val requestTime = LocalDateTime.now()
        return users.filter { user -> user.value.lastSeen.isAfter(requestTime.minusMinutes(5L)) }.map { user -> user.value }
    }

    fun updateUserLastSeen(author: String, timestamp: LocalDateTime): Unit {
        if (users.containsKey(author)) {
            users[author]?.lastSeen = timestamp
        } else {
            users.put(author, User(author, false, timestamp))
        }
    }

    fun processMessage(message: Message): Message {
        return MessageProcessor(message)
                .escapeHtmlTags()
                .processLinks()
                .finalizeMessage()
    }
}