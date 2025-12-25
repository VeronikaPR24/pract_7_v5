package com.bignerdranch.android.autopark

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Bus::class,
        Route::class,
        DriverRouteCrossRef::class,
        BusDriverCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CarParkDatabase : RoomDatabase() {
    abstract fun carparkdao(): CarParkDao

    companion object {
        @Volatile
        private var INSTANCE: CarParkDatabase? = null

        fun getDatabase(context: Context): CarParkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CarParkDatabase::class.java,
                    "fleet_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                populateDatabase(INSTANCE!!.carparkdao())
            }
        }
    }
}

private suspend fun populateDatabase(dao: CarParkDao) {
    val dispatcher = User(
        name = "Диспетчер",
        email = "dispatcher@mail.com",
        login = "dispatcher",
        password = "123456",
        role = "dispatcher"
    )
    dao.insertUser(dispatcher)

    val drivers = listOf(
        User(
            name = "Петр Петров",
            email = "petrov@mail.com",
            login = "driver1",
            password = "driver1",
            role = "driver",
            salaryBonus = 5200.0
        ),
        User(
            name = "Сергей Сергеев",
            email = "sergeev@mail.com",
            login = "driver2",
            password = "driver2",
            role = "driver",
            salaryBonus = 4800.0
        ),
    )
    drivers.forEach { dao.insertUser(it) }

    val buses = listOf(
        Bus(
            busNumber = 101,
            model = "ПАЗ-3205",
            registrationNumber = "А123ВС77",
            purchaseDate = "01.01.2021",
            initialPrice = 2500000.0,
            currentValue = 2000000.0,
            depreciation = 20.0,
            condition = "хорошее",
            mileage = 45000
        ),
        Bus(
            busNumber = 102,
            model = "ЛиАЗ-5292",
            registrationNumber = "В234ТР77",
            purchaseDate = "15.03.2021",
            initialPrice = 3000000.0,
            currentValue = 2400000.0,
            depreciation = 20.0,
            condition = "хорошее",
            mileage = 60000
        ),
        Bus(
            busNumber = 103,
            model = "МАЗ-206",
            registrationNumber = "С345УК77",
            purchaseDate = "10.06.2022",
            initialPrice = 2800000.0,
            currentValue = 2500000.0,
            depreciation = 10.0,
            condition = "хорошее",
            mileage = 20000
        ),
        Bus(
            busNumber = 104,
            model = "ПАЗ-3204",
            registrationNumber = "Е456МН77",
            purchaseDate = "20.09.2020",
            initialPrice = 2200000.0,
            currentValue = 1500000.0,
            depreciation = 32.0,
            condition = "плохое",
            mileage = 80000
        ),
    )
    buses.forEach { dao.insertBus(it) }

    val routes = listOf(
        Route(
            routeNumber = "101",
            startPoint = "Вокзал",
            endPoint = "ЕКТС",
            distance = 3.1,
            estimatedTime = 35
        ),
        Route(
            routeNumber = "202",
            startPoint = "Пассаж",
            endPoint = "ЕКТС",
            distance = 4.0,
            estimatedTime = 11
        ),
    )
    routes.forEach { dao.insertRoute(it) }
    val driverRouteCrossRefs = listOf(
        DriverRouteCrossRef(driverId = 2, routeId = 1),
        DriverRouteCrossRef(driverId = 2, routeId = 2),
        DriverRouteCrossRef(driverId = 3, routeId = 1),
        DriverRouteCrossRef(driverId = 3, routeId = 3),
        DriverRouteCrossRef(driverId = 4, routeId = 2),
        DriverRouteCrossRef(driverId = 4, routeId = 3)
    )
    driverRouteCrossRefs.forEach { dao.assignDriverToRoute(it) }
    val busDriverCrossRefs = listOf(
        BusDriverCrossRef(busId = 1, driverId = 2),
        BusDriverCrossRef(busId = 2, driverId = 3),
        BusDriverCrossRef(busId = 3, driverId = 4),
        BusDriverCrossRef(busId = 4, driverId = 2)
    )
    busDriverCrossRefs.forEach { dao.assignBusToDriver(it) }
}