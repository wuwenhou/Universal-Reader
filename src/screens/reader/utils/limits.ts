/**
 * Limits that keep a broken or hostile chapter from taking the reader down.
 */
export const MAX_CHAPTER_HTML_LENGTH = 5 * 1024 * 1024;
export const MAX_CHAPTER_PARAGRAPHS = 20000;
export const CHAPTER_LOAD_TIMEOUT_MS = 30000;

/**
 * Thrown when a chapter takes longer than {@link CHAPTER_LOAD_TIMEOUT_MS}.
 */
export class ChapterTimeoutError extends Error {
  constructor(timeoutMs: number) {
    super(`The chapter did not load within ${timeoutMs / 1000} seconds.`);
    this.name = 'ChapterTimeoutError';
  }
}

/**
 * Thrown when a chapter is too large to render.
 */
export class ChapterTooLargeError extends Error {
  constructor(length: number, limit: number) {
    super(
      `The chapter is too large to display (${Math.round(
        length / 1024,
      )} KB, the limit is ${Math.round(limit / 1024)} KB).`,
    );
    this.name = 'ChapterTooLargeError';
  }
}
/**
 * Rejects with a {@link ChapterTimeoutError} when the promise does not settle
 * in time.
 *
 * A plugin that never answers would otherwise leave the reader on the
 * loading screen forever.
 */
export const withTimeout = <T>(
  promise: Promise<T>,
  timeoutMs: number = CHAPTER_LOAD_TIMEOUT_MS,
): Promise<T> => {
  let timeout: ReturnType<typeof setTimeout> | undefined;

  const timeoutPromise = new Promise<never>((_, reject) => {
    timeout = setTimeout(
      () => reject(new ChapterTimeoutError(timeoutMs)),
      timeoutMs,
    );
  });

  return Promise.race([promise, timeoutPromise]).finally(() => {
    if (timeout) {
      clearTimeout(timeout);
    }
  }) as Promise<T>;
};
