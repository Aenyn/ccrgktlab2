package fr.panicot.ccrg.botting.bots

import fr.panicot.ccrg.botting.Bot
import fr.panicot.ccrg.core.messaging.MessageController
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.support.CronTrigger
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by William on 16/03/2017.
 */
class BobBot(messageController: MessageController, random: Random): Bot(messageController, random) {
    private val scheduler = ThreadPoolTaskScheduler()

    val botNameList = Arrays.asList("BobBot", "bobBot", "BoBot", "boBot")
    val bandNameList = Arrays.asList("Noisia", "IM")

    override fun start() {
        val todayName = botNameList[random.nextInt(botNameList.size)]
        val todayBand = bandNameList[random.nextInt(bandNameList.size)]
        val reminderHour = random.nextInt(3) + 13 - 1;
        val reminderMinute = random.nextInt(60);

        scheduler.initialize()
        scheduler.schedule(Runnable{messageController.sendMessage(todayName, "daily reminder que $todayBand c'est trop bien")}, CronTrigger("0 $reminderMinute $reminderHour * * *"))
    }
}
