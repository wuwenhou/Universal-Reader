/**
 * Parses JSON without throwing.
 *
 * Repository lists, tracker responses and other third party payloads are not
 * guaranteed to be valid JSON. A broken payload must not crash the app, so
 * the fallback is returned instead.
 */
export const safeJsonParse = <T>(text: string, fallback: T): T => {
  try {
    const parsed = JSON.parse(text);
    return parsed === null || parsed === undefined ? fallback : (parsed as T);
  } catch {
    return fallback;
  }
};

/**
 * Parses a JSON response body without throwing.
 */
export const safeJsonResponse = async <T>(
  response: Response,
  fallback: T,
): Promise<T> => {
  try {
    const text = await response.text();
    return safeJsonParse<T>(text, fallback);
  } catch {
    return fallback;
  }
};
