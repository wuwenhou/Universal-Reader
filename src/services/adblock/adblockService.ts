import { getPluginHostnames } from '@plugins/registry';

import { adblockEngine } from './engine';
import { filterChapterHtml } from './htmlFilter';
import { AdblockFilterListId } from './types';

export interface AdblockChapterResult {
  html: string;
  css: string;
  removed: number;
}

/**
 * Hosts the app talks to itself. Blocking one of them would break the app
 * (updates, backups, trackers), so they are never filtered.
 */
const APP_HOSTS = [
  'api.github.com',
  'github.com',
  'objects.githubusercontent.com',
  'raw.githubusercontent.com',
  'googleapis.com',
  'myanimelist.net',
  'anilist.co',
  'kitsu.io',
  'mangaupdates.com',
];

export const isAppHost = (hostname: string): boolean =>
  APP_HOSTS.some(host => hostname === host || hostname.endsWith(`.${host}`));

class AdblockService {
  private buildPromise: Promise<void> | null = null;

  private blockedCount = 0;

  private exceptions: string[] = [];

  private enabled = true;

  private enabledLists: AdblockFilterListId[] = ['abpvn', 'easylist'];

  /**
   * Applies the persisted settings.
   *
   * The engine is rebuilt in the background, so switching a filter list does
   * not block the UI.
   */
  applySettings(settings: {
    enabled: boolean;
    enabledLists: AdblockFilterListId[];
    exceptions: string[];
  }): void {
    this.enabled = settings.enabled;
    this.exceptions = settings.exceptions.map(entry =>
      entry.trim().toLowerCase(),
    );

    if (!settings.enabled) {
      this.buildPromise = null;
      return;
    }

    const listsChanged =
      settings.enabledLists.length !== this.enabledLists.length ||
      settings.enabledLists.some(id => !this.enabledLists.includes(id));

    this.enabledLists = settings.enabledLists;

    if (listsChanged) {
      this.buildPromise = adblockEngine
        .configure(settings.enabledLists)
        .then(() => undefined)
        .catch(() => undefined);
    } else if (!this.buildPromise) {
      this.buildPromise = adblockEngine
        .configure(settings.enabledLists)
        .then(() => undefined)
        .catch(() => undefined);
    }
  }

  /** Resolves once the engine can answer requests. */
  async ensureReady(): Promise<void> {
    if (!this.enabled) {
      return;
    }
    await this.buildPromise;
  }

  get isEnabled(): boolean {
    return this.enabled;
  }

  get isReady(): boolean {
    return adblockEngine.isReady;
  }

  /** True when the given hostname opted out of blocking. */
  isException(hostname: string): boolean {
    const normalized = hostname.toLowerCase();
    return this.exceptions.some(
      exception =>
        normalized === exception || normalized.endsWith(`.${exception}`),
    );
  }

  /**
   * True when the hostname belongs to a source site.
   *
   * The chapter text and the images that belong to it are needed to read, so
   * a source site is never blocked.
   */
  isPluginHostname(hostname: string): boolean {
    const normalized = hostname.toLowerCase();
    return getPluginHostnames().some(
      host =>
        normalized === host ||
        normalized.endsWith(`.${host}`) ||
        host.endsWith(`.${normalized}`),
    );
  }

  /**
   * Checks a URL against the filter lists.
   *
   * Returns `false` for the app's own hosts and for hosts that opted out.
   */
  isBlocked(
    url: string,
    options: { type?: 'script' | 'image' | 'stylesheet' | 'other' } = {},
  ): boolean {
    if (!this.enabled || !adblockEngine.isReady) {
      return false;
    }

    let hostname = '';
    try {
      hostname = new URL(url).hostname.toLowerCase();
    } catch {
      return false;
    }

    if (!hostname || isAppHost(hostname) || this.isException(hostname)) {
      return false;
    }

    // The plugin fetches the chapter from its own site - that request must
    // never be blocked, only the third party resources it references.
    if (this.isPluginHostname(hostname)) {
      return false;
    }

    const result = adblockEngine.match(url, {
      type: options.type ?? 'other',
    });

    if (result.blocked) {
      this.blockedCount += 1;
    }

    return result.blocked;
  }

  /**
   * Removes blocked resources from a chapter and returns the cosmetic rules
   * for the page.
   */
  filterChapter(html: string, pageUrl: string): AdblockChapterResult {
    if (!this.enabled) {
      return { html, css: '', removed: 0 };
    }

    let hostname = '';
    try {
      hostname = new URL(pageUrl).hostname.toLowerCase();
    } catch {
      hostname = '';
    }

    const result = filterChapterHtml(
      html,
      pageUrl,
      this.isException(hostname) ? hostname : undefined,
    );

    this.blockedCount += result.removed;

    return result;
  }

  /** Cosmetic rules that hide ad containers on the given page. */
  getCosmeticCss(pageUrl: string): string {
    if (!this.enabled) {
      return '';
    }
    return adblockEngine.getCosmeticCss(pageUrl);
  }

  getBlockedCount(): number {
    return this.blockedCount;
  }

  resetBlockedCount(): void {
    this.blockedCount = 0;
  }
}

export const adblockService = new AdblockService();

export { adblockEngine };
export * from './types';
