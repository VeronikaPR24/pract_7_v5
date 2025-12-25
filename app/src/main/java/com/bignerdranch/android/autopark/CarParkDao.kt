package com.bignerdranch.android.autopark

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface CarParkDao {

    @Query("SELECT * FROM users WHERE login = :login AND password = :password AND role = :role")
    suspend fun authenticate(login: String, password: String, role: String): User?

    @Query("SELECT * FROM users WHERE login = :login")
    suspend fun getUserByLogin(login: String): User?

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: Int): User?

    @Query("SELECT * FROM users WHERE login = :login AND role = :role")
    suspend fun getUserByLoginAndRole(login: String, role: String): User?

    @Insert
    suspend fun insertUser(user: User): Long

    @Query("DELETE FROM users WHERE login = :login AND role = 'passenger'")
    suspend fun deletePassengerByLogin(login: String): Int

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUserById(userId: Int)

    @Update
    suspend fun updateUser(user: User)

    @Insert
    suspend fun insertBus(bus: Bus): Long

    @Update
    suspend fun updateBus(bus: Bus)

    @Delete
    suspend fun deleteUser(user: User)

    @Delete
    suspend fun deleteBus(bus: Bus)

    @Query("SELECT * FROM buses ORDER BY busNumber")
    fun getAllBuses(): LiveData<List<Bus>>

    @Query("SELECT * FROM buses WHERE condition = :condition")
    suspend fun getBusesByCondition(condition: String): List<Bus>

    @Query("SELECT * FROM buses WHERE busId = :busId")
    suspend fun getBusById(busId: Int): Bus?

    @Query("SELECT * FROM users WHERE userId = (SELECT driverId FROM bus_driver WHERE busId = :busId)")
    suspend fun getDriverForBus(busId: Int): User?

    @Query("SELECT * FROM buses WHERE busNumber = :busNumber")
    suspend fun getBusByNumber(busNumber: Int): Bus?

    @Insert
    suspend fun insertRoute(route: Route)

    @Update
    suspend fun updateRoute(route: Route)

    @Query("SELECT * FROM routes WHERE routeNumber LIKE '%' || :query || '%'")
    suspend fun searchRoutes(query: String): List<Route>

    @Delete
    suspend fun deleteRoute(route: Route)

    @Query("SELECT * FROM routes ORDER BY routeNumber")
    fun getAllRoutes(): LiveData<List<Route>>

    @Query("SELECT * FROM routes WHERE routeId = :routeId")
    suspend fun getRouteById(routeId: Int): Route?

    @Insert
    suspend fun assignDriverToRoute(crossRef: DriverRouteCrossRef)

    @Delete
    suspend fun removeDriverFromRoute(crossRef: DriverRouteCrossRef)

    @Insert
    suspend fun assignBusToDriver(crossRef: BusDriverCrossRef)

    @Delete
    suspend fun removeBusFromDriver(crossRef: BusDriverCrossRef)

    @Transaction
    @Query("SELECT * FROM users WHERE userId = :driverId AND role = 'driver'")
    suspend fun getDriverWithDetails(driverId: Int): DriverWithDetails?

    @Transaction
    @Query("SELECT * FROM routes WHERE routeId = :routeId")
    suspend fun getRouteWithDetails(routeId: Int): RouteWithDetails?

    @Query("SELECT * FROM routes ORDER BY routeNumber")
    suspend fun getAllRoutesStatic(): List<Route>

    @Query("SELECT * FROM buses ORDER BY busNumber")
    suspend fun getAllBusesStatic(): List<Bus>

    @Query("SELECT * FROM users WHERE role = 'driver'")
    suspend fun getAllDriversStatic(): List<User>

    @Transaction
    @Query("SELECT u.* FROM users u INNER JOIN driver_route dr ON u.userId = dr.driverId WHERE dr.routeId = :routeId")
    suspend fun getDriversForRoute(routeId: Int): List<User>

    @Transaction
    @Query("SELECT r.* FROM routes r INNER JOIN driver_route dr ON r.routeId = dr.routeId WHERE dr.driverId = :driverId")
    suspend fun getRoutesForDriver(driverId: Int): List<Route>

    @Transaction
    @Query("SELECT b.* FROM buses b INNER JOIN bus_driver bd ON b.busId = bd.busId WHERE bd.driverId = :driverId")
    suspend fun getBusForDriver(driverId: Int): Bus?
}

data class DriverWithDetails(
    @Embedded val driver: User,
    @Relation(
        parentColumn = "userId",
        entityColumn = "routeId",
        associateBy = Junction(
            value = DriverRouteCrossRef::class,
            parentColumn = "driverId",
            entityColumn = "routeId"
        )
    )
    val knownRoutes: List<Route>,
    @Relation(
        parentColumn = "userId",
        entityColumn = "busId",
        associateBy = Junction(
            value = BusDriverCrossRef::class,
            parentColumn = "driverId",
            entityColumn = "busId"
        )
    )
    val assignedBuses: List<Bus>
)

data class RouteWithDetails(
    @Embedded val route: Route,
    @Relation(
        parentColumn = "routeId",
        entityColumn = "userId",
        associateBy = Junction(
            value = DriverRouteCrossRef::class,
            parentColumn = "routeId",
            entityColumn = "driverId"
        )
    )
    val qualifiedDrivers: List<User>
)