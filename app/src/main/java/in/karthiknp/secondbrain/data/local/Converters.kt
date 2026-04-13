package `in`.karthiknp.secondbrain.data.local

import androidx.room.TypeConverter
import `in`.karthiknp.secondbrain.domain.model.HabitFrequency
import `in`.karthiknp.secondbrain.domain.model.MemoryType
import `in`.karthiknp.secondbrain.domain.model.TaskStatus

/**
 * Room type converters for enum and custom types.
 */
class Converters {

    @TypeConverter
    fun fromMemoryType(type: MemoryType): String = type.name

    @TypeConverter
    fun toMemoryType(value: String): MemoryType = MemoryType.valueOf(value)

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String = status.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter
    fun fromHabitFrequency(freq: HabitFrequency): String = freq.name

    @TypeConverter
    fun toHabitFrequency(value: String): HabitFrequency = HabitFrequency.valueOf(value)
}
