package classes

import enums.TimeOfDay

// Класс, использующийся для получения текущего времени суток (одно из значений перечисления TimeOfDay).
class Clock {
    static TimeOfDay getTimeOfDay() {
        Calendar currentTime = Calendar.getInstance()
        int currentHour = currentTime.get(Calendar.HOUR_OF_DAY)

        if (currentHour < 6)
            return TimeOfDay.NIGHT
        if (currentHour < 12)
            return TimeOfDay.MORNING
        if (currentHour < 17)
            return TimeOfDay.DAY
        if (currentHour < 23)
            return TimeOfDay.EVENING
        return TimeOfDay.NIGHT
    }
}
