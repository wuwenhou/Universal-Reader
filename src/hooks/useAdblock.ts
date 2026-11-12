import { useCallback, useEffect, useState } from 'react';

import { useAdblockSettings } from '@hooks/persisted/useAdblockSettings';
import { adblockService } from '@services/adblock/adblockService';

export interface UseAdblockResult {
  /** Whether ad blocking is switched on. */
  enabled: boolean;
  /** Whether the filter engine finished building. */
  ready: boolean;
  /** Cosmetic rules that hide ad containers on the given page. */
  getCosmeticCss: (pageUrl: string) => string;
  /** Requests blocked since the app started. */
  blockedCount: number;
}

/**
 * Keeps the ad-blocking engine in sync with the persisted settings and
 * exposes the state the reader needs.
 */
export const useAdblock = (): UseAdblockResult => {
  const { enabled, enabledLists, exceptions } = useAdblockSettings();
  const [ready, setReady] = useState(adblockService.isReady);
  const [blockedCount, setBlockedCount] = useState(0);

  useEffect(() => {
    let cancelled = false;

    adblockService.applySettings({ enabled, enabledLists, exceptions });

    if (!enabled) {
      setReady(false);
      return;
    }

    void adblockService.ensureReady().then(() => {
      if (!cancelled) {
        setReady(adblockService.isReady);
      }
    });

    return () => {
      cancelled = true;
    };
  }, [enabled, enabledLists, exceptions]);

  const getCosmeticCss = useCallback(
    (pageUrl: string) => adblockService.getCosmeticCss(pageUrl),
    [],
  );

  useEffect(() => {
    if (!ready) {
      return;
    }

    const interval = setInterval(() => {
      setBlockedCount(adblockService.getBlockedCount());
    }, 2000);

    return () => clearInterval(interval);
  }, [ready]);

  return { enabled, ready, getCosmeticCss, blockedCount };
};
