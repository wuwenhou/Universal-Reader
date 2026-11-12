import { load } from 'cheerio';

import { adblockEngine } from './engine';
import { AdblockHtmlResult, AdblockRequestType } from './types';

const URL_ATTRIBUTES: Record<string, AdblockRequestType> = {
  src: 'image',
  'data-src': 'image',
  'data-original': 'image',
  poster: 'image',
  'srcset': 'image',
  href: 'stylesheet',
  'xlink:href': 'other',
};

/**
 * Guesses the resource type of a URL so uBO's `$image` / `$script` options
 * are evaluated against the right type.
 */
const guessRequestType = (url: string): AdblockRequestType => {
  const path = url.split(/[?#]/)[0].toLowerCase();

  if (/\.(js|mjs)$/.test(path)) {
    return 'script';
  }
  if (/\.css$/.test(path)) {
    return 'stylesheet';
  }
  if (/\.(png|jpe?g|gif|webp|avif|bmp|svg|ico)$/.test(path)) {
    return 'image';
  }
  if (/\.(mp4|webm|ogg|mp3|m4a|wav|flac)$/.test(path)) {
    return 'media';
  }
  if (/\.(woff2?|ttf|otf|eot)$/.test(path)) {
    return 'font';
  }

  return 'other';
};

const resolveUrl = (value: string, baseUrl: string): string => {
  try {
    return new URL(value, baseUrl).toString();
  } catch {
    return value;
  }
};

/**
 * Removes blocked resources from a chapter and collects the cosmetic rules
 * that hide ad containers.
 *
 * The chapter is rendered inside a WebView that only loads the plugin's own
 * markup, so dropping the blocked elements here is what actually stops the
 * ads from loading.
 */
export const filterChapterHtml = (
  html: string,
  pageUrl: string,
  allowHostname?: string,
): AdblockHtmlResult => {
  if (!adblockEngine.isReady) {
    return { html, css: '', removed: 0 };
  }

  let removed = 0;

  try {
    const $ = load(html, null, false);

    $('*').each((_, element) => {
      const node = $(element);
      const tagName = (element as { tagName?: string }).tagName?.toLowerCase();

      Object.entries(URL_ATTRIBUTES).forEach(([attribute, type]) => {
        const value = node.attr(attribute);
        if (!value || !value.trim()) {
          return;
        }

        const url = resolveUrl(value.trim(), pageUrl);
        if (!/^https?:/i.test(url)) {
          return;
        }

        const requestType = type === 'other' ? guessRequestType(url) : type;
        const result = adblockEngine.match(url, {
          type: requestType,
          sourceUrl: pageUrl,
          allowHostname,
        });

        if (result.blocked) {
          node.remove();
          removed += 1;
        }
      });

      if (tagName === 'a' && node.attr('href')) {
        const url = resolveUrl(node.attr('href')!.trim(), pageUrl);
        const result = adblockEngine.match(url, {
          type: 'main_frame',
          sourceUrl: pageUrl,
          allowHostname,
        });
        if (result.blocked) {
          node.remove();
          removed += 1;
        }
      }
    });

    return {
      html: $.html(),
      css: adblockEngine.getCosmeticCss(pageUrl),
      removed,
    };
  } catch {
    return { html, css: '', removed: 0 };
  }
};
