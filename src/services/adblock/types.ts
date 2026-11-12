/**
 * Shared types for the ad-blocking service.
 */

export type AdblockFilterListId = 'easylist' | 'easyprivacy' | 'abpvn';

export type AdblockRequestType =
  | 'main_frame'
  | 'sub_frame'
  | 'stylesheet'
  | 'script'
  | 'image'
  | 'font'
  | 'object'
  | 'xmlhttprequest'
  | 'ping'
  | 'csp_report'
  | 'media'
  | 'websocket'
  | 'other';

export interface AdblockSettings {
  /** Master switch. When disabled, no request is ever blocked. */
  enabled: boolean;
  /** Filter lists that are applied. */
  enabledLists: AdblockFilterListId[];
  /** Hostnames for which blocking is disabled entirely. */
  exceptions: string[];
  /** Number of requests blocked since the counters were last reset. */
  blockedCount: number;
}

export interface AdblockListDescriptor {
  id: AdblockFilterListId;
  /** Human readable name shown in the settings screen. */
  label: string;
  /** Where the bundled copy was fetched from. */
  url: string;
  /** License of the list, shown in the settings screen. */
  license: string;
}

export interface AdblockMatchResult {
  blocked: boolean;
  /** The filter rule that caused the block, if any. */
  filter?: string;
}

export interface AdblockHtmlResult {
  /** Chapter HTML with blocked resources removed. */
  html: string;
  /** Cosmetic rules that hide ad containers in the page. */
  css: string;
  /** Number of blocked resources that were removed from the HTML. */
  removed: number;
}
