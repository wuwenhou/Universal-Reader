import { safeJsonParse } from '@utils/safeJson';

import {
  ChapterTimeoutError,
  ChapterTooLargeError,
  MAX_CHAPTER_HTML_LENGTH,
  withTimeout,
} from '../limits';

describe('reader limits', () => {
  describe('withTimeout', () => {
    it('resolves when the promise settles in time', async () => {
      await expect(withTimeout(Promise.resolve('done'), 1000)).resolves.toBe(
        'done',
      );
    });

    it('rejects when the promise takes too long', async () => {
      const never = new Promise<string>(() => {});

      await expect(withTimeout(never, 10)).rejects.toBeInstanceOf(
        ChapterTimeoutError,
      );
    });

    it('rejects with the original error', async () => {
      const failure = new Error('boom');

      await expect(withTimeout(Promise.reject(failure), 1000)).rejects.toBe(
        failure,
      );
    });
  });

  describe('ChapterTooLargeError', () => {
    it('mentions the size and the limit', () => {
      const error = new ChapterTooLargeError(2048, 1024);

      expect(error.message).toContain('2 KB');
      expect(error.message).toContain('1 KB');
    });
  });

  describe('safeJsonParse', () => {
    it('parses valid JSON', () => {
      expect(safeJsonParse<number[]>('[1, 2]', [])).toEqual([1, 2]);
    });

    it('returns the fallback for invalid JSON', () => {
      expect(safeJsonParse('not json', ['fallback'])).toEqual(['fallback']);
    });

    it('returns the fallback for a null payload', () => {
      expect(safeJsonParse('null', ['fallback'])).toEqual(['fallback']);
    });
  });

  it('caps the chapter size', () => {
    expect(MAX_CHAPTER_HTML_LENGTH).toBeGreaterThan(0);
  });
});
