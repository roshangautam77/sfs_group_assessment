export const TASK_STATUSES = ['TODO', 'IN_PROGRESS', 'DONE'] as const;
export const TASK_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'] as const;
export type TaskStatus = (typeof TASK_STATUSES)[number];
export type TaskPriority = (typeof TASK_PRIORITIES)[number];
export type StatusFilter = TaskStatus | '';
export interface Task {
  readonly id: string;
  readonly title: string;
  readonly description: string | null;
  readonly status: TaskStatus;
  readonly priority: TaskPriority;
  readonly assignee: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}
export interface CreateTask {
  readonly title: string;
  readonly priority: TaskPriority;
  readonly description?: string | null;
  readonly assignee?: string | null;
  readonly status?: TaskStatus;
}
export const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: 'To do',
  IN_PROGRESS: 'In progress',
  DONE: 'Done',
};
