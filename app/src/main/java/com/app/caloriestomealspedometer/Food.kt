package com.app.caloriestomealspedometer

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Food : RealmObject() {
    @PrimaryKey
    var id: Long = 0L
    var name: String = ""
    var unit: String = ""
    var calorie: Long = 0L
    var imagePath: String = ""
}