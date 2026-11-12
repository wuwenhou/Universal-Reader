import { isUrlAllowed } from '@services/network';

import { adblockService } from './adblockService';

export class BlockedRequestError extends Error {
  constructor(url: string) {
    super(`Blocked by ad blocker: ${url}`);
    this.name = 'BlockedRequestError';
  }
}

type FetchLike = typeof fetch;

const isUrl = (input: RequestInfo | URL): input is string =>
  typeof input === 'string';

const toUrl = (input: RequestInfo | URL): string => {
  if (isUrl(input)) {
    return input;
  }
  if (input instanceof URL) {
    return input.toString();
  }
  return input.url;
};

/**
 * Guesses the resource type from the URL so uBO's `$image` / `$script`
 * options are evaluated against the right type.
 */
const guessType = (
  url: string,
): 'script' | 'image' | 'stylesheet' | 'other' => {
  const path = url.split(/[?#]/)[0].toLowerCase();

  if (/\.(js|mjs)$/.test(path)) {
    return 'script';
  }
  if (/\.(png|jpe?g|gif|webp|avif|bmp|svg|ico)$/.test(path)) {
    return 'image';
  }
  if (/\.css$/.test(path)) {
    return 'stylesheet';
  }

  return 'other';
};

let installed = false;

/**
 * Wraps the global `fetch` so requests that match the filter lists never
 * leave the device.
 *
 * Blocked requests resolve with an empty response instead of throwing, so a
 * plugin that fetches an ad script alongside the chapter keeps working.
 */
export const installAdblockFetchInterceptor = (): void => {
  if (installed) {
    return;
  }
  installed = true;

  const originalFetch: FetchLike = globalThis.fetch;

  globalThis.fetch = (async (
    input: RequestInfo | URL,
    init?: RequestInit,
  ): Promise<Response> => {
    const url = toUrl(input);

    // Plain HTTP is only used when the source was explicitly allowed.
    if (!isUrlAllowed(url)) {
      return new Response('', { status: 403, statusText: 'Blocked' });
    }

    if (adblockService.isBlocked(url, { type: guessType(url) })) {
      return new Response('', { status: 204, statusText: 'Blocked' });
    }

    return originalFetch(input, init);
  }) as FetchLike;
};
