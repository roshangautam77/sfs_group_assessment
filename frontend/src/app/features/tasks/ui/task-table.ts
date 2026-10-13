import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { STATUS_LABELS, Task } from '../domain/task';
@Component({
  selector: 'app-task-table',
  imports: [DatePipe],
  templateUrl: './task-table.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskTable {
  readonly tasks = input.required<readonly Task[]>();
  readonly statusLabels = STATUS_LABELS;
}
