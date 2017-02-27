package classes

import enums.TimeOfDay

class Clock {
    static TimeOfDay getTimeOfDay() {
        Calendar currentTime = Calendar.getInstance()
        int currentHour = currentTime.get(Calendar.HOUR_OF_DAY)

        if (currentHour < 12 && currentHour > 6)
            return TimeOfDay.MORNING
        else if (currentHour < 17 && currentHour > 6)
            return TimeOfDay.DAY
        else if (currentHour < 23 && currentHour > 6)
            return TimeOfDay.EVENING
        else
            return TimeOfDay.NIGHT
    }
}
