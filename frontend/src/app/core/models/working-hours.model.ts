export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface WorkingHoursItem {
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
}
