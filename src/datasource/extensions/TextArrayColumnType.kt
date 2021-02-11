package me.simplepush.datasource.extensions

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.postgresql.jdbc.PgArray

fun Table.textArray(name: String): Column<List<String>> = registerColumn(name, TextArrayColumnType())

private class TextArrayColumnType : ColumnType() {
    override fun sqlType() = "text[]"

    override fun valueFromDB(value: Any): List<String> {
        return if (value is PgArray) {
            val array = value.array
            if (array is Array<*>) {
                array.map {
                    it as String
                }
            }
            else {
                throw Exception("Values returned from database if not of type kotlin Array<*> ")
            }
        }
        else throw Exception("Values returned from database if not of type PgArray")
    }
}