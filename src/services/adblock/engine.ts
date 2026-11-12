import { FiltersEngine, Request } from '@ghostery/adblocker';

import { ADBLOCK_LISTS, ADBLOCK_LIST_ORDER, buildFilterText } from './lists';
import {
  AdblockFilterListId,
  AdblockMatchResult,
  AdblockRequestType,
} from './types';

/**
 * Maps the request types the reader cares about onto the types uBO filters
 * use. The engine evaluates `$script`, `$image`, ... options against this
 * value, so getting it right decides whether a rule applies.
 */
const toEngineRequestType = (type: AdblockRequestType): Request['type'] => {
  switch (type) {
    case 'main_frame':
      return 'main_frame';
    case 'sub_frame':
      return 'sub_frame';
    case 'stylesheet':
      return 'stylesheet';
    case 'script':
      return 'script';
    case 'image':
      return 'image';
    case 'font':
      return 'font';
    case 'object':
      return 'object';
    case 'xmlhttprequest':
      return 'xmlhttprequest';
    case 'ping':
      return 'ping';
    case 'csp_report':
      return 'csp_report';
    case 'media':
      return 'media';
    case 'websocket':
      return 'websocket';
    default:
      return 'other';
  }
};

export interface AdblockMatchOptions {
  type?: AdblockRequestType;
  /** The page that triggered the request. */
  sourceUrl?: string;
  /**
   * When set, requests to this host are never blocked. It is used to keep the
   * source site itself reachable while third party ads are blocked.
   */
  allowHostname?: string;
}

const hostnameOf = (url: string): string => {
  try {
    return new URL(url).hostname.toLowerCase();
  } catch {
    return '';
  }
};

/**
 * In-memory uBO-compatible filter engine.
 *
 * The engine is built lazily: parsing the filter lists takes a moment, so it
 * happens in the background after the app started and callers await
 * {@link AdblockEngine.ready} when they need a verdict immediately.
 */
class AdblockEngine {
  private engine: FiltersEngine | null = null;

  private building: Promise<FiltersEngine> | null = null;

  private enabledLists: AdblockFilterListId[] = [];

  /**
   * Builds the engine for the given lists. Returns the previous engine when
   * the list selection did not change, so a settings toggle does not rebuild
   * an identical engine.
   */
  async configure(lists: AdblockFilterListId[]): Promise<void> {
    const wanted = ADBLOCK_LIST_ORDER.filter(id => lists.includes(id));
    const unchanged =
      wanted.length === this.enabledLists.length &&
      wanted.every((id, index) => this.enabledLists[index] === id);

    if (unchanged && this.engine) {
      return;
    }

    this.enabledLists = wanted;
    this.engine = null;
    this.building = null;

    await this.ensureReady();
  }

  /** Builds the engine if it does not exist yet. */
  async ensureReady(): Promise<FiltersEngine> {
    if (this.engine) {
      return this.engine;
    }

    if (!this.building) {
      const lists = this.enabledLists;
      const build: Promise<FiltersEngine> = Promise.resolve().then(() => {
        const filters = buildFilterText(lists);
        const engine = FiltersEngine.parse(filters, {
          enableCompression: true,
          enableHtmlFiltering: true,
          loadCosmeticFilters: true,
        });

        // A rebuild may have been started while this one was running. The
        // newer engine wins, so the older one is dropped instead of
        // overwriting it.
        if (this.building === build) {
          this.engine = engine;
        }

        return engine;
      });

      this.building = build;
    }

    return this.building;
  }

  get isReady(): boolean {
    return this.engine !== null;
  }

  /**
   * Checks a request against the filter lists.
   *
   * The engine is used synchronously, so it has to be built already - callers
   * that cannot await should treat a missing engine as "not blocked".
   */
  match(url: string, options: AdblockMatchOptions = {}): AdblockMatchResult {
    const engine = this.engine;
    if (!engine) {
      return { blocked: false };
    }

    const hostname = hostnameOf(url);
    if (!hostname) {
      return { blocked: false };
    }

    if (options.allowHostname) {
      const allowed = options.allowHostname.toLowerCase();
      if (
        hostname === allowed ||
        hostname.endsWith(`.${allowed}`) ||
        allowed.endsWith(`.${hostname}`)
      ) {
        return { blocked: false };
      }
    }

    try {
      const request = Request.fromRawDetails({
        url,
        sourceUrl: options.sourceUrl ?? '',
        type: toEngineRequestType(options.type ?? 'other'),
      });

      const result = engine.match(request);
      if (!result.match) {
        return { blocked: false };
      }

      return {
        blocked: true,
        filter: result.filter?.rawLine ?? result.filter?.toString(),
      };
    } catch {
      return { blocked: false };
    }
  }

  /**
   * Returns the CSS that hides ad containers on the given page.
   *
   * uBO cosmetic rules are plain CSS selectors, so they can be injected into
   * the reader page directly.
   */
  getCosmeticCss(url: string): string {
    const engine = this.engine;
    if (!engine) {
      return '';
    }

    const hostname = hostnameOf(url);
    if (!hostname) {
      return '';
    }

    try {
      const request = Request.fromRawDetails({ url, type: 'main_frame' });
      const cosmetics = engine.getCosmeticsFilters({
        url,
        hostname,
        domain: request.domain,
        getBaseRules: true,
        getRulesFromHostname: true,
        getInjectionRules: true,
        getExtendedRules: true,
      });

      return cosmetics.styles ?? '';
    } catch {
      return '';
    }
  }

  /** The lists the engine was built from. */
  get activeLists(): AdblockFilterListId[] {
    return [...this.enabledLists];
  }
}

export const adblockEngine = new AdblockEngine();

export { ADBLOCK_LISTS, ADBLOCK_LIST_ORDER };
