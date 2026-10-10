package iu.canteen.service;

import iu.canteen.dao.CalendarEventDao;
import iu.canteen.model.CalendarEvent;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Quản lý lịch ngày lễ Việt Nam và sự kiện trường ĐH Quốc tế IU,
 * tính toán hệ số tác động đến lượng khách canteen.
 */
public class HolidayService {
    private final CalendarEventDao calendarEventDao;

    public HolidayService() {
        this.calendarEventDao = new CalendarEventDao();
        initDefaultHolidays(LocalDate.now().getYear());
    }

    public HolidayService(CalendarEventDao calendarEventDao) {
        this.calendarEventDao = calendarEventDao;
        initDefaultHolidays(LocalDate.now().getYear());
    }

    /**
     * Khởi tạo các ngày lễ chính tại Việt Nam và sự kiện trường học tiêu biểu.
     */
    public void initDefaultHolidays(int year) {
        // Tết Dương lịch
        upsertHoliday(LocalDate.of(year, 1, 1), "Nghỉ lễ", "Tết Dương lịch", 0.2);
        // Ngày Giải phóng & Quốc tế Lao động
        upsertHoliday(LocalDate.of(year, 4, 30), "Nghỉ lễ", "Ngày Giải phóng miền Nam", 0.2);
        upsertHoliday(LocalDate.of(year, 5, 1), "Nghỉ lễ", "Ngày Quốc tế Lao động", 0.2);
        // Quốc khánh
        upsertHoliday(LocalDate.of(year, 9, 2), "Nghỉ lễ", "Quốc khánh Việt Nam", 0.2);
        upsertHoliday(LocalDate.of(year, 9, 3), "Nghỉ lễ", "Nghỉ bù Quốc khánh", 0.3);
        // Ngày Nhà giáo VN
        upsertHoliday(LocalDate.of(year, 11, 20), "Sự kiện trường", "Ngày Nhà giáo Việt Nam 20/11", 1.25);
    }

    private void upsertHoliday(LocalDate date, String type, String desc, double factor) {
        calendarEventDao.upsertEvent(new CalendarEvent(0, date, type, desc, factor));
    }

    /**
     * Tính toán hệ số nhu cầu sinh viên cho một ngày cụ thể:
     * - Nếu có sự kiện trong DB: lấy hệ số của sự kiện (nghỉ lễ 0.2, thi cử 1.2, ngày hội 1.35)
     * - Nếu là Thứ 7 / Chủ Nhật: 0.55 (canteen cuối tuần ít sinh viên)
     * - Ngày thường trong tuần: 1.0
     */
    public double getDemandFactor(LocalDate date) {
        if (date == null) date = LocalDate.now();

        Optional<CalendarEvent> eventOpt = calendarEventDao.getEventByDate(date);
        if (eventOpt.isPresent()) {
            return eventOpt.get().getDemandFactor();
        }

        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return 0.55;
        }

        return 1.0;
    }

    public Optional<CalendarEvent> getEventByDate(LocalDate date) {
        return calendarEventDao.getEventByDate(date);
    }

    public List<CalendarEvent> getEventsBetween(LocalDate from, LocalDate to) {
        return calendarEventDao.getEventsBetween(from, to);
    }

    public void addEvent(CalendarEvent event) {
        calendarEventDao.upsertEvent(event);
    }
}
