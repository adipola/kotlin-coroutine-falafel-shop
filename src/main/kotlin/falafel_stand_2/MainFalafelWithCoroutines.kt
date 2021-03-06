package falafel_stand_2



import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.*
import falafel_stand.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

import log
import kotlin.coroutines.CoroutineContext

fun main() {

    val orders = listOf(
        Menu.FalafelInAPlate(listOf(Salad.RedOnion,Salad.SpicySalad),FalafelBalls.OvenCookedBalls),
        Menu.FalafelInAPlate(listOf(Salad.RedOnion,Salad.SpicySalad),FalafelBalls.OvenCookedBalls),
        Menu.FalafelInPita(listOf(Salad.RedOnion,Salad.RedOnion),FalafelBalls.FriedBalls),
        Menu.FalafelInPita(listOf(Salad.TomatoSalad,Salad.SpicySalad),FalafelBalls.FriedBalls)
    )
//    var time = 0
    printHeader("start")
    var time = measureTimeMillis {
        makeFalafel(orders)
    }

    printHeader(" falafel $time")

    runBlocking {
         time = measureTimeMillis {

            falafelWithCoroutine(orders)
        }
        printHeader("falafel with coroutine $time")

        time = measureTimeMillis {
            falafelWithCoroutineAndDispatcher(orders)
        }
//
        printHeader("falafel with coroutine and dispatcher $time")
//
        time = measureTimeMillis {
            falafelWithChannel(orders)
        }
        printHeader(" falafel with channel $time" )

    }



    // ### popular types of channels:

    val channel1 = Channel<Menu>(capacity = Channel.RENDEZVOUS)
    val channel2 = Channel<Menu>(capacity = Channel.CONFLATED)
    val channel3 = Channel<Menu>(capacity = 10)
    val channel4 = Channel<Menu>(capacity = Channel.UNLIMITED)

    // ### Context examples
    Dispatchers.Default
    Dispatchers.IO
    Dispatchers.Main
    Dispatchers.Unconfined



}

private fun printHeader(str: String) {
    log(" ########################################################################")
    log(" ###### $str  ###### ")
    log(" ########################################################################")
}


private suspend fun CoroutineScope.falafelWithChannel(orders: List<Menu>) {

    val ordersChannel = Channel<Menu>()
    val job = launch {
        for (order in orders) {
            ordersChannel.send(order)
        }
        ordersChannel.close()
    }
    coroutineScope {
        launch(CoroutineName("falafel_routine_1")) { makeFalafelWithCoroutineAndChannel(ordersChannel) }
        launch(CoroutineName("falafel_routine_2")) { makeFalafelWithCoroutineAndChannel(ordersChannel) }
    }
}


private suspend fun makeFalafelWithCoroutineAndChannel(ordersChannel :ReceiveChannel<Menu>){
    for(order in ordersChannel){
        falafelWithSuspended(order)
    }
}

private suspend fun falafelWithSuspended(order: Menu) {
    log("processing order: ${order}")
    val dish = when (order) {
        is Menu.FalafelInPita -> {
            val salads = getSaladsSuspended(order.salads)
            val flafelBalls = getFalafelBallsSuspended(order.falafel)
            assembleDishInPitaSuspended(order, salads, flafelBalls)
        }
        is Menu.FalafelInAPlate -> {
            val salads = getSaladsSuspended(order.salads)
            val falafelBalls = getFalafelBallsSuspended(order.falafel)
            assemblePlateSuspended(order, salads, falafelBalls)
        }
    }

    log("SERVE: $dish")
}

private suspend fun falafelWithCoroutine(orders: List<Menu>) {

    log(" ###### falafel with coroutines ######")
    coroutineScope {
        launch(CoroutineName("falafel_routine")) {
            for( order in orders){
                falafelWithSuspended(order)
            }
        }
    }
}

private suspend fun falafelWithCoroutineAndDispatcher(orders: List<Menu>) {
    log(" ###### falafel with coroutines and dispatcher ######")

    coroutineScope {
        launch(Dispatchers.Default + CoroutineName("falafel_routine_with_thread_pool")) {
            makeFalafel(orders)
        }
    }
}





private suspend fun  assembleDishInPitaSuspended(order:Menu.FalafelInPita,salads: MixedSalad, falafelBalls: FalafelBalls): Dish.FalafelInPitta {
    log(" adding salads and falafel to pita")
    delay(10)
    return Dish.FalafelInPitta(order, salads,falafelBalls)
}

private suspend fun assemblePlateSuspended(order:Menu.FalafelInAPlate, salads: MixedSalad, falafelBalls: FalafelBalls): Dish.FalafelInAPlate {
    log(" adding salads and falafel to the plate")
    delay(10)
    return Dish.FalafelInAPlate(order, salads,falafelBalls)
}

private suspend fun getFalafelBallsSuspended(falafel: FalafelBalls): FalafelBalls {
    log(" make  ${falafel.toString()}")
    delay(5)

    return falafel
}

private suspend fun getSaladsSuspended(salads: List<Salad>): MixedSalad {
    log(" cut salads for ${salads.toString()}")
    delay(2 * salads.size.toLong())

    return MixedSalad(salads, salads.sumByDouble { it.price().toDouble() }.toFloat())
}




