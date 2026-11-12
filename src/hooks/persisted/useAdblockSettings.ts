import { useMMKVObject } from 'react-native-mmkv';

import { AdblockFilterListId } from '@services/adblock/types';

export const ADBLOCK_SETTINGS = 'ADBLOCK_SETTINGS';

export interface AdblockSettings {
  /** Master switch for ad blocking. */
  enabled: boolean;
  /** Filter lists that are applied when blocking is enabled. */
  enabledLists: AdblockFilterListId[];
  /** Hostnames for which blocking is disabled. */
  exceptions: string[];
  /** Requests blocked since the counter was last reset. */
  blockedCount: number;
}

export const initialAdblockSettings: AdblockSettings = {
  enabled: true,
  enabledLists: ['abpvn', 'easylist'],
  exceptions: [],
  blockedCount: 0,
};

export const useAdblockSettings = () => {
  const [settings = initialAdblockSettings, setSettings] =
    useMMKVObject<AdblockSettings>(ADBLOCK_SETTINGS);

  const setAdblockSettings = (values: Partial<AdblockSettings>) => {
    setSettings({ ...initialAdblockSettings, ...settings, ...values });
  };

  const toggleList = (list: AdblockFilterListId) => {
    const enabledLists = settings.enabledLists.includes(list)
      ? settings.enabledLists.filter(id => id !== list)
      : [...settings.enabledLists, list];

    setAdblockSettings({ enabledLists });
  };

  const addException = (hostname: string) => {
    const normalized = hostname.trim().toLowerCase();
    if (!normalized || settings.exceptions.includes(normalized)) {
      return;
    }
    setAdblockSettings({ exceptions: [...settings.exceptions, normalized] });
  };

  const removeException = (hostname: string) => {
    setAdblockSettings({
      exceptions: settings.exceptions.filter(entry => entry !== hostname),
    });
  };

  return {
    ...initialAdblockSettings,
    ...settings,
    setAdblockSettings,
    toggleList,
    addException,
    removeException,
  };
};
