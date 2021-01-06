package com.sachin.app.chat

fun main() {

    val person : Person = Student("Sachin", 19, "DAV")

    println(person)
}

interface Person {
    val name: String
    val age: Int
}

data class Student(override val name: String, override val age: Int, val school: String) : Person