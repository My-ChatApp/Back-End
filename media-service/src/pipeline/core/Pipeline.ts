import type { Filter } from './types.js';

/** Orchestrator: runs filters in order; each filter is the next point on the pipe. */
export class Pipeline<T> {
  constructor(private readonly filters: Filter<T>[]) {}

  async execute(msg: T): Promise<T> {
    for (const filter of this.filters) {
      await filter.process(msg);
    }
    return msg;
  }
}
