package com.bignerdranch.android.autopark

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val userId: Int = 0,
    val name: String,
    val email: String,
    val login: String,
    val password: String,
    val role: String,
    val salaryBonus: Double = 0.0
)

@Entity(
    tableName = "buses",
    indices = [Index(value = ["busNumber"], unique = true)]
)
data class Bus(
    @PrimaryKey(autoGenerate = true)
    val busId: Int = 0,
    val busNumber: Int,
    val model: String,
    val registrationNumber: String,

    @ColumnInfo(name = "purchase_date")
    val purchaseDate: String,
    val initialPrice: Double,
    val currentValue: Double,
    val depreciation: Double = 0.0,
    val condition: String,
    val mileage: Int = 0
)

@Entity(tableName = "routes")
data class Route(
    @PrimaryKey(autoGenerate = true)
    val routeId: Int = 0,
    val routeNumber: String,
    val startPoint: String,
    val endPoint: String,
    val distance: Double,
    val estimatedTime: Int
)

@Entity(
    tableName = "driver_route",
    primaryKeys = ["driverId", "routeId"],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["driverId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Route::class,
            parentColumns = ["routeId"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DriverRouteCrossRef(
    val driverId: Int,
    val routeId: Int
)

@Entity(
    tableName = "bus_driver",
    primaryKeys = ["busId", "driverId"],
    foreignKeys = [
        ForeignKey(
            entity = Bus::class,
            parentColumns = ["busId"],
            childColumns = ["busId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["driverId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BusDriverCrossRef(
    val busId: Int,
    val driverId: Int
)