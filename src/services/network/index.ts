import { getMMKVObject, setMMKVObject } from '@utils/mmkv/mmkv';

import {
  INSECURE_SOURCE_EXCEPTIONS_KEY,
  isHostAllowedInsecure,
  isInsecureUrl,
} from './security';

const normalizeHostname = (hostname: string): string =>
  hostname.trim().toLowerCase().replace(/^\*\./, '');

/**
 * Sources that are allowed to be fetched over plain HTTP.
 *
 * A source is added here only when it has no HTTPS endpoint; every other
 * request has to use HTTPS.
 */
export const getInsecureSourceExceptions = (): string[] =>
  getMMKVObject<string[]>(INSECURE_SOURCE_EXCEPTIONS_KEY) ?? [];

export const setInsecureSourceExceptions = (exceptions: string[]): void => {
  setMMKVObject(
    INSECURE_SOURCE_EXCEPTIONS_KEY,
    exceptions.map(normalizeHostname).filter(Boolean),
  );
};

export const addInsecureSourceException = (hostname: string): void => {
  const normalized = normalizeHostname(hostname);

  if (!normalized) {
    return;
  }

  const exceptions = getInsecureSourceExceptions();
  if (exceptions.includes(normalized)) {
    return;
  }

  setInsecureSourceExceptions([...exceptions, normalized]);
};

export const removeInsecureSourceException = (hostname: string): void => {
  const normalized = normalizeHostname(hostname);
  setInsecureSourceExceptions(
    getInsecureSourceExceptions().filter(entry => entry !== normalized),
  );
};

/**
 * True when the URL may be fetched.
 *
 * Plain HTTP is rejected unless the host was explicitly allowed.
 */
export const isUrlAllowed = (url: string): boolean => {
  if (!isInsecureUrl(url)) {
    return true;
  }

  try {
    const { hostname } = new URL(url);
    return isHostAllowedInsecure(hostname, getInsecureSourceExceptions());
  } catch {
    return false;
  }
};
