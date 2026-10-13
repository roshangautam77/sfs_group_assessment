import { inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  BehaviorSubject,
  catchError,
  combineLatest,
  EMPTY,
  exhaustMap,
  finalize,
  map,
  of,
  startWith,
  Subject,
  switchMap,
  tap,
} from 'rxjs';
import { errorMessage } from '../../../core/http/api-error.interceptor';
import { CreateTask, StatusFilter, Task } from '../domain/task';
import { TaskApi } from './task-api';
export interface TaskListState {
  readonly tasks: readonly Task[];
  readonly loading: boolean;
  readonly error: string | null;
}
const LOADING: TaskListState = { tasks: [], loading: true, error: null };
@Injectable()
export class TaskStore {
  private readonly api = inject(TaskApi);
  private readonly filterChanges = new BehaviorSubject<StatusFilter>('');
  private readonly reloads = new Subject<void>();
  private readonly creates = new Subject<CreateTask>();
  private readonly selected = signal<StatusFilter>('');
  private readonly isSaving = signal(false);
  private readonly saveFailure = signal<string | null>(null);
  private readonly createdCount = signal(0);
  readonly filter = this.selected.asReadonly();
  readonly saving = this.isSaving.asReadonly();
  readonly createError = this.saveFailure.asReadonly();
  readonly createSuccess = this.createdCount.asReadonly();
  readonly list = toSignal(
    combineLatest([this.filterChanges, this.reloads.pipe(startWith(undefined))]).pipe(
      switchMap(([status]) =>
        this.api.list(status).pipe(
          map((tasks): TaskListState => ({ tasks, loading: false, error: null })),
          catchError((error: unknown) =>
            of<TaskListState>({ tasks: [], loading: false, error: errorMessage(error) }),
          ),
          startWith(LOADING),
        ),
      ),
    ),
    { initialValue: LOADING },
  );
  constructor() {
    this.creates
      .pipe(
        exhaustMap((task) => {
          this.isSaving.set(true);
          this.saveFailure.set(null);
          return this.api.create(task).pipe(
            tap(() => {
              this.createdCount.update((count) => count + 1);
              this.reloads.next();
            }),
            catchError((error: unknown) => {
              this.saveFailure.set(errorMessage(error));
              return EMPTY;
            }),
            finalize(() => this.isSaving.set(false)),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe();
  }
  setFilter(status: StatusFilter): void {
    if (status === this.selected()) return;
    this.selected.set(status);
    this.filterChanges.next(status);
  }
  refresh(): void {
    this.reloads.next();
  }
  create(task: CreateTask): void {
    this.creates.next(task);
  }
}
