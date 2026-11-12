/**
 * Transport security for the app's own network requests.
 *
 * Everything is fetched over HTTPS. A source that only serves plain HTTP can
 * be allowed explicitly, which keeps the exception visible instead of
 * silently downgrading every request.
 */
export const INSECURE_SOURCE_EXCEPTIONS_KEY = 'INSECURE_SOURCE_EXCEPTIONS';

const normalizeHostname = (hostname: string): string =>
  hostname.trim().toLowerCase().replace(/^\*\./, '');

/**
 * True when the URL uses plain HTTP.
 */
export const isInsecureUrl = (url: string): boolean => {
  try {
    return new URL(url).protocol === 'http:';
  } catch {
    return false;
  }
};

export const isHostAllowedInsecure = (
  hostname: string,
  exceptions: string[],
): boolean => {
  const normalized = normalizeHostname(hostname);

  return exceptions.some(exception => {
    const allowed = normalizeHostname(exception);
    return normalized === allowed || normalized.endsWith(`.${allowed}`);
  });
};
