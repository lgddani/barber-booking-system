export type ExceptionType = 'CLOSED' | 'CUSTOM_HOURS';

export interface ScheduleException {
  id: string;
  date: string;
  type: ExceptionType;
  startTime: string | null;
  endTime: string | null;
}

export interface ScheduleExceptionRequest {
  date: string;
  type: ExceptionType;
  startTime?: string;
  endTime?: string;
}
