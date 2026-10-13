import { ChangeDetectionStrategy, Component, effect, inject, viewChild } from '@angular/core';
import { TaskStore } from './data-access/task-store';
import { STATUS_LABELS, StatusFilter, TASK_STATUSES } from './domain/task';
import { TaskCreateForm } from './ui/task-create-form';
import { TaskTable } from './ui/task-table';
@Component({
  selector: 'app-task-page',
  imports: [TaskCreateForm, TaskTable],
  providers: [TaskStore],
  templateUrl: './task-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskPage {
  readonly store = inject(TaskStore);
  readonly statuses = TASK_STATUSES;
  readonly statusLabels = STATUS_LABELS;
  readonly createForm = viewChild(TaskCreateForm);
  constructor() {
    effect(() => {
      if (this.store.createSuccess() > 0) this.createForm()?.reset();
    });
  }
  changeFilter(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    if (value === '' || TASK_STATUSES.some((status) => status === value))
      this.store.setFilter(value as StatusFilter);
  }
}
